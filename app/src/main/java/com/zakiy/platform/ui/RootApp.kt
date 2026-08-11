package com.zakiy.platform.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AccountRole
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.ui.auth.AuthNavHost
import com.zakiy.platform.ui.auth.ForcePasswordChangeScreen
import com.zakiy.platform.ui.main.MainNavHost
import com.zakiy.platform.ui.role.RoleNavHost
import com.zakiy.platform.ui.theme.AppearanceMode
import com.zakiy.platform.ui.theme.ZakiyTheme

/** الجذر الفعلي - يقرر أي شجرة تنقّل تظهر حسب حالة الحساب، نفس منطق
 * RootView بتطبيق iOS بالضبط: بوابة تغيير كلمة سر إجبارية > توجيه حسب
 * الدور المؤسسي > تجربة الحساب الفردي/الضيف العادية > شاشة الترحيب. */
@Composable
fun RootApp(authManager: AuthManager) {
    val isBootstrapping by authManager.isBootstrapping.collectAsStateWithLifecycle()
    val isAuthenticated by authManager.isAuthenticated.collectAsStateWithLifecycle()
    val didLoadRole by authManager.didLoadRole.collectAsStateWithLifecycle()
    val mustChangePassword by authManager.mustChangePassword.collectAsStateWithLifecycle()
    val role by authManager.role.collectAsStateWithLifecycle()
    val isGuest by authManager.isGuest.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { authManager.bootstrap() }

    ZakiyTheme(appearanceMode = AppearanceMode.SYSTEM) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                isBootstrapping || (isAuthenticated && !didLoadRole) -> SplashScreen()
                isAuthenticated && mustChangePassword -> ForcePasswordChangeScreen(authManager)
                // الطالب (role="student") يكمل بنفس تجربة الحساب الفردي العادية
                // (MainNavHost) بس مع وصول إضافي لجدوله - عكس بقية الأدوار
                // المؤسسية اللي تستبدل التنقّل بالكامل بلوحتها. نفس منطق
                // RoleRoutedView بتطبيق iOS بالضبط (.student -> MainTabView())
                isAuthenticated && AccountRole.from(role) != null && AccountRole.from(role) != AccountRole.Student ->
                    RoleNavHost(role = remember(role) { AccountRole.from(role)!! }, authManager = authManager)
                isAuthenticated || isGuest -> MainNavHost(authManager = authManager)
                else -> AuthNavHost(authManager = authManager)
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.logo_dark),
            contentDescription = null,
            modifier = Modifier.size(160.dp),
        )
    }
}
