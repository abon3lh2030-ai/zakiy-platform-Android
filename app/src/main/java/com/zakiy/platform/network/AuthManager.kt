package com.zakiy.platform.network

import android.content.Context
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/** حالة الحساب الكاملة - نفس دور SupabaseAuthManager بتطبيق iOS بالضبط:
 * الجلسة (Supabase Auth مباشرة بمفتاح anon)، الدور المؤسسي (عبر /api/me)،
 * وبوابة إجبار تغيير كلمة السر. مصدر حقيقة وحيد يُحقن بكل الشاشات. */
class AuthManager private constructor(private val appContext: Context) {
    private val sessionStore = SessionStore(appContext)
    private val goTrue get() = NetworkModule.goTrueApi
    private val backend get() = NetworkModule.backendApi

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isBootstrapping = MutableStateFlow(true)
    val isBootstrapping: StateFlow<Boolean> = _isBootstrapping.asStateFlow()

    private val _didLoadRole = MutableStateFlow(false)
    val didLoadRole: StateFlow<Boolean> = _didLoadRole.asStateFlow()

    private val _role = MutableStateFlow<String?>(null)
    val role: StateFlow<String?> = _role.asStateFlow()

    private val _schoolId = MutableStateFlow<String?>(null)
    val schoolId: StateFlow<String?> = _schoolId.asStateFlow()

    private val _classId = MutableStateFlow<String?>(null)
    val classId: StateFlow<String?> = _classId.asStateFlow()

    private val _mustChangePassword = MutableStateFlow(false)
    val mustChangePassword: StateFlow<Boolean> = _mustChangePassword.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _email = MutableStateFlow<String?>(null)
    val email: StateFlow<String?> = _email.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    // مستوى التعليم من بيانات حساب Supabase الوصفية (user_metadata.education_level) -
    // يهمنا بس القيمة الحرفية "معلم" (تحدد فرضية معلم لحساب فردي بلا role
    // مؤسسي بشاشة "مدرستي" - نفس منطق 03-auth.js بالضبط)
    private val _educationLevel = MutableStateFlow<String?>(null)
    val educationLevel: StateFlow<String?> = _educationLevel.asStateFlow()

    /** تُستدعى مرة وحدة عند إقلاع التطبيق - تجرّب تسترجع جلسة محفوظة */
    suspend fun bootstrap() {
        _isBootstrapping.value = true
        val token = sessionStore.accessTokenFlow.firstOrNull()
        val refreshToken = sessionStore.readRefreshToken()
        _isGuest.value = sessionStore.guestModeFlow.firstOrNull() ?: false
        if (token != null && refreshToken != null) {
            TokenHolder.accessToken = token
            _userId.value = sessionStore.userIdFlow.firstOrNull()
            _email.value = sessionStore.emailFlow.firstOrNull()
            _username.value = sessionStore.usernameFlow.firstOrNull() ?: _email.value.orEmpty()
            _isAuthenticated.value = true
            // نجدّد الجلسة دايمًا وقت الإقلاع (access token عمره ساعة بس) قبل أي نداء آخر
            try {
                val refreshed = goTrue.refreshSession(RefreshGrantRequest(refreshToken))
                applySession(refreshed)
            } catch (e: Exception) {
                // فشل التجديد - الجلسة المحفوظة انتهت فعليًا، نرجّعه لتسجيل الدخول
                signOutLocalOnly()
            }
            if (_isAuthenticated.value) loadRole()
        }
        _isBootstrapping.value = false
    }

    suspend fun signIn(identifier: String, password: String): Result<Unit> = runCatching {
        val email = if (identifier.contains("@")) {
            identifier
        } else {
            backend.resolveLoginIdentifier(com.zakiy.platform.network.dto.ResolveIdentifierRequest(identifier)).email
        }
        val session = goTrue.signInWithPassword(PasswordGrantRequest(email, password))
        applySession(session)
        loadRole()
    }

    suspend fun signUp(username: String, email: String, password: String): Result<Unit> = runCatching {
        val session = goTrue.signUp(SignUpRequest(email, password, mapOf("username" to username)))
        applySession(session)
        sessionStore.saveUsername(username)
        _username.value = username
        loadRole()
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        val token = TokenHolder.accessToken ?: throw IllegalStateException("لا يوجد جلسة")
        goTrue.updateUser("Bearer $token", UpdateUserRequest(password = newPassword))
    }

    suspend fun completeForcedPasswordChange() {
        runCatching { backend.completePasswordChange() }
        _mustChangePassword.value = false
    }

    suspend fun continueAsGuest() {
        _isGuest.value = true
        sessionStore.setGuestMode(true)
    }

    suspend fun signOut() {
        val token = TokenHolder.accessToken
        if (token != null) {
            runCatching { goTrue.signOut("Bearer $token") }
        }
        signOutLocalOnly()
    }

    private suspend fun signOutLocalOnly() {
        TokenHolder.accessToken = null
        sessionStore.clearSession()
        _isAuthenticated.value = false
        _role.value = null
        _schoolId.value = null
        _classId.value = null
        _mustChangePassword.value = false
        _username.value = ""
        _email.value = null
        _userId.value = null
        _didLoadRole.value = false
        _educationLevel.value = null
    }

    private suspend fun applySession(session: GoTrueSession) {
        TokenHolder.accessToken = session.accessToken
        sessionStore.saveSession(session.accessToken, session.refreshToken, session.user.id, session.user.email)
        _userId.value = session.user.id
        _email.value = session.user.email
        session.user.userMetadata["username"]?.let {
            _username.value = it
            sessionStore.saveUsername(it)
        }
        _educationLevel.value = session.user.userMetadata["education_level"]
        _isAuthenticated.value = true
    }

    /** يجيب دور الحساب من الباك إند (/api/me) - يحدد التوجيه (حساب فردي/دور
     * مؤسسي/بوابة تغيير كلمة سر إجبارية) - نفس منطق RootView بـ iOS بالضبط.
     *
     * ملاحظة مهمة: signIn/loadRole تُستدعى غالبًا من scope مربوط بشاشة تسجيل
     * الدخول (rememberCoroutineScope) - واللحظة اللي isAuthenticated تصير true،
     * RootApp يستبدل الشجرة بـ SplashScreen فيتفكك LoginScreen ويُلغى الـscope
     * بتاعه، ويلغي معه طلب /api/me نصّه (IOException: Canceled) قبل ما يوصل -
     * فيضل role=null للأبد بنفس الجلسة والمستخدم يطيح بتجربة حساب فردي/ضيف
     * غلط بدل لوحته المؤسسية. withContext(NonCancellable) يخلي هذا الجزء
     * يكمل لآخره حتى لو الـscope اللي نادى عليه انلغى. */
    suspend fun loadRole() = withContext(NonCancellable) {
        runCatching {
            val me = backend.me()
            _role.value = me.role
            _schoolId.value = me.schoolId
            _classId.value = me.classId
            _mustChangePassword.value = me.mustChangePassword
            me.username?.let { _username.value = it }
        }
        _didLoadRole.value = true
    }

    companion object {
        @Volatile private var instance: AuthManager? = null
        fun getInstance(context: Context): AuthManager =
            instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
    }
}
