package com.zakiy.platform.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.ui.study.StudyFlowState
import com.zakiy.platform.ui.study.StudyQuizScreen
import com.zakiy.platform.ui.study.StudySummaryScreen
import com.zakiy.platform.ui.study.StudyTextScreen

/** فتح كتاب من المكتبة "للمذاكرة" - نفس شاشات المذاكرة الفردية بالضبط (نص/
 * تلخيص/اختبار)، بس جلسة مستقلة تبدأ من نص الكتاب المحفوظ مباشرة بدل رفع
 * ملف جديد. */
@Composable
fun LibraryDetailScreen(bookId: String, onBack: () -> Unit) {
    val studyState = remember { StudyFlowState() }
    var isLoaded by remember { mutableStateOf(false) }
    val navController = rememberNavController()

    LaunchedEffect(bookId) {
        val detail = runCatching { NetworkModule.backendApi.libraryBook(bookId) }.getOrNull()
        studyState.reset(detail?.extractedText.orEmpty())
        isLoaded = true
    }

    if (!isLoaded) return

    NavHost(navController = navController, startDestination = "text") {
        composable("text") {
            StudyTextScreen(studyState = studyState, onNavigateToSummary = { navController.navigate("summary") }, onBack = onBack)
        }
        composable("summary") {
            StudySummaryScreen(studyState = studyState, onNavigateToQuiz = { navController.navigate("quiz") }, onBack = { navController.popBackStack() })
        }
        composable("quiz") {
            StudyQuizScreen(studyState = studyState, onBack = { navController.popBackStack() })
        }
    }
}
