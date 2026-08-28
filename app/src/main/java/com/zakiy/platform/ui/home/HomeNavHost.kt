package com.zakiy.platform.ui.home

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.ui.navigation.Screen
import com.zakiy.platform.ui.rooms.RoomLobbyScreen
import com.zakiy.platform.ui.rooms.RoomScreen
import com.zakiy.platform.ui.study.StudyFlowState
import com.zakiy.platform.ui.study.StudyQuizScreen
import com.zakiy.platform.ui.study.StudySummaryScreen
import com.zakiy.platform.ui.study.StudyTextScreen

/** شجرة تنقّل الرئيسية - وضع فردي (رفع -> نص -> تلخيص -> اختبار) أو غرفة
 * جماعية/درس مباشر (نفس الشاشات، بس مرتبطة بغرفة Socket.IO حقيقية). */
@Composable
fun HomeNavHost(authManager: AuthManager) {
    val navController = rememberNavController()
    // حالة جلسة المذاكرة الفردية الحالية (نص مستخرج/ملخص/اختبار) - تُشارك
    // بين شاشات الرفع/النص/التلخيص/الاختبار الثلاث طول الجلسة نفسها
    val studyState = androidx.compose.runtime.remember { StudyFlowState() }

    NavHost(navController = navController, startDestination = Screen.Home) {
        composable(Screen.Home) {
            HomeScreen(
                authManager = authManager,
                studyState = studyState,
                onNavigateToText = { navController.navigate(Screen.StudyText) },
                onNavigateToGroupLobby = { navController.navigate(Screen.RoomLobbyGroup) },
                onNavigateToClassroomLobby = { navController.navigate(Screen.RoomLobbyClassroom) },
            )
        }
        composable(Screen.StudyText) {
            StudyTextScreen(
                studyState = studyState,
                onNavigateToSummary = { navController.navigate(Screen.StudySummary) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.StudySummary) {
            StudySummaryScreen(
                studyState = studyState,
                onNavigateToQuiz = { navController.navigate(Screen.StudyQuiz) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.StudyQuiz) {
            StudyQuizScreen(studyState = studyState, onBack = { navController.popBackStack() })
        }
        composable(Screen.RoomLobbyGroup) {
            RoomLobbyScreen(
                roomType = "quiz",
                authManager = authManager,
                onEnterRoom = { code, isCreator -> navController.navigate(Screen.room(code, "quiz", isCreator)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.RoomLobbyClassroom) {
            RoomLobbyScreen(
                roomType = "classroom",
                authManager = authManager,
                onEnterRoom = { code, isCreator -> navController.navigate(Screen.room(code, "classroom", isCreator)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.RoomPattern) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code").orEmpty()
            val type = backStackEntry.arguments?.getString("type").orEmpty()
            val isCreator = backStackEntry.arguments?.getString("isCreator").toBoolean()
            RoomScreen(
                roomCode = code,
                roomType = type,
                isCreator = isCreator,
                authManager = authManager,
                onLeave = { navController.popBackStack(Screen.Home, inclusive = false) },
            )
        }
    }
}
