package com.zakiy.platform.ui.embeddedweb

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zakiy.platform.R
import com.zakiy.platform.network.ApiConfig
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.SessionStore
import com.zakiy.platform.network.TokenHolder
import org.json.JSONObject

private const val SUPABASE_PROJECT_REF = "qwlbufcailgpxxatgyez"

// مفتاح localStorage الافتراضي اللي supabase-js (v2) يخزّن فيه الجلسة لو ما
// فيه storageKey مخصص بـ createClient - الموقع ينادي createClient(url, key)
// بمعطيين بس (website/src/js/00-globals.js)، فما فيه أي تخصيص، والصيغة
// الافتراضية دايمًا `sb-<project-ref>-auth-token`
private const val SUPABASE_STORAGE_KEY = "sb-$SUPABASE_PROJECT_REF-auth-token"

/** الغلاف الأساسي (بدون Scaffold) - يُستخدم مستقلًا لشاشة كاملة (معمل
 * الروبوتات عبر [EmbeddedWebScreen]) أو مدمجًا داخل تبويب بشاشة ثانية (تبويب
 * الكيمياء بمختبر العلوم - زر الرجوع فوق يصير من الشاشة الحاضنة نفسها).
 *
 * تمرير الجلسة (auth passthrough): نحقن جلسة Supabase الحالية بالتطبيق
 * (accessToken + refreshToken من TokenHolder/SessionStore) داخل localStorage
 * بنفس مفتاح supabase-js الافتراضي - بأول لحظة ممكنة (onPageStarted، قبل
 * ما سكربتات الصفحة تشتغل) عشان كود الموقع نفسه (getSession -> onAuthSuccess)
 * يتعامل معها زي أي جلسة محفوظة من قبل بمتصفح عادي - نفس آلية استمرارية
 * الجلسة اللي الموقع أصلًا يعتمد عليها بين تبويباته. إعادة الحقن بـ
 * onPageFinished شبكة أمان لو التوقيت الأول فات. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedWebView(
    authManager: AuthManager,
    navigateJsFunction: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var sessionReady by remember { mutableStateOf(false) }
    var sessionJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val accessToken = TokenHolder.accessToken
        val refreshToken = SessionStore(context.applicationContext).readRefreshToken()
        val userId = authManager.userId.value
        val email = authManager.email.value
        sessionJson = if (accessToken != null && refreshToken != null && userId != null) {
            buildSupabaseSessionJson(accessToken, refreshToken, userId, email)
        } else {
            null
        }
        sessionReady = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (sessionReady) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            private var injectedOnStart = false

                            private fun injectSession(view: WebView) {
                                val session = sessionJson ?: return
                                view.evaluateJavascript(
                                    "(function(){try{localStorage.setItem(" +
                                        "${JSONObject.quote(SUPABASE_STORAGE_KEY)}, ${JSONObject.quote(session)}" +
                                        ");}catch(e){}})();",
                                    null,
                                )
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                loadError = false
                                if (!injectedOnStart) {
                                    injectedOnStart = true
                                    injectSession(view)
                                }
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                // شبكة أمان: نعيد الحقن لو onPageStarted فاتت السباق مع
                                // سكربتات الصفحة، ثم ننادي دالة التنقّل الجاهزة بالموقع
                                injectSession(view)
                                val fn = navigateJsFunction
                                if (!fn.isNullOrBlank()) {
                                    view.evaluateJavascript(
                                        "(function(){try{if(typeof $fn === 'function'){$fn();}}catch(e){}})();",
                                        null,
                                    )
                                }
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request == null || request.isForMainFrame) loadError = true
                            }
                        }
                        loadUrl(ApiConfig.WEB_BASE)
                    }
                },
            )
        }
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        if (loadError) {
            Text(
                stringResource(R.string.embedded_web_load_error),
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** شاشة كاملة (Scaffold + زر رجوع) تلف [EmbeddedWebView] - تُستخدم لمعمل
 * الروبوتات (مسار تنقّل مستقل خاص فيه، تمامًا زي ما الموقع يعامله كزر
 * سايدبار مستقل مو تبويب داخل مختبر العلوم). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddedWebScreen(
    titleRes: Int,
    authManager: AuthManager,
    onBack: () -> Unit,
    navigateJsFunction: String? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        EmbeddedWebView(
            authManager = authManager,
            navigateJsFunction = navigateJsFunction,
            modifier = Modifier.padding(padding),
        )
    }
}

private fun buildSupabaseSessionJson(accessToken: String, refreshToken: String, userId: String, email: String?): String {
    val nowSeconds = System.currentTimeMillis() / 1000
    val user = JSONObject().apply {
        put("id", userId)
        put("aud", "authenticated")
        put("role", "authenticated")
        if (email != null) put("email", email)
        put("app_metadata", JSONObject())
        put("user_metadata", JSONObject())
    }
    val session = JSONObject().apply {
        put("access_token", accessToken)
        put("refresh_token", refreshToken)
        put("token_type", "bearer")
        // ما نعرف وقت انتهاء التوكن الحقيقي هنا (AuthManager ما يخزّنه) -
        // ساعة افتراضية معقولة؛ لو كانت خاطئة (منتهية فعليًا أقرب)، supabase-js
        // بنفسه يجدد تلقائيًا بـ refresh_token الحقيقي اللي مررناه، فتبقى
        // النتيجة صحيحة بكل الأحوال
        put("expires_in", 3600)
        put("expires_at", nowSeconds + 3600)
        put("user", user)
    }
    return session.toString()
}
