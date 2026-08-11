package com.zakiy.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.SocketManager
import com.zakiy.platform.ui.RootApp

/** يوفّر AuthManager لكل الشجرة بدون DI framework - نمط بسيط ومباشر
 * (CompositionLocal) يكفي حجم التطبيق هذا. */
val LocalAuthManager = staticCompositionLocalOf<AuthManager> {
    error("AuthManager ما انحقن - لازم يكون داخل RootApp")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authManager = AuthManager.getInstance(applicationContext)

        setContent {
            CompositionLocalProvider(LocalAuthManager provides authManager) {
                RootApp(authManager = authManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketManager.disconnect()
    }
}
