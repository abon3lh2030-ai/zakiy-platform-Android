package com.zakiy.platform.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.ui.navigation.Screen

@Composable
fun AuthNavHost(authManager: AuthManager) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Login) {
        composable(Screen.Login) {
            LoginScreen(
                authManager = authManager,
                onGoToSignUp = { navController.navigate(Screen.SignUp) },
            )
        }
        composable(Screen.SignUp) {
            SignUpScreen(
                authManager = authManager,
                onGoToLogin = { navController.popBackStack() },
            )
        }
    }
}
