package com.zakiy.platform.ui.role

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zakiy.platform.R
import com.zakiy.platform.network.AccountRole
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.ui.admin.AdminDashboardScreen
import com.zakiy.platform.ui.ai.AiBookPickerScreen
import com.zakiy.platform.ui.ai.AiConversationScreen
import com.zakiy.platform.ui.ai.AiConversationsScreen
import com.zakiy.platform.ui.assignments.AssignmentDetailScreen
import com.zakiy.platform.ui.assignments.AssignmentsScreen
import com.zakiy.platform.ui.embeddedweb.EmbeddedWebScreen
import com.zakiy.platform.ui.gradesheet.GradesheetScreen
import com.zakiy.platform.ui.library.LibraryDetailScreen
import com.zakiy.platform.ui.library.LibraryScreen
import com.zakiy.platform.ui.madrasati.EnrichmentScreen
import com.zakiy.platform.ui.madrasati.HomeworkHelpScreen
import com.zakiy.platform.ui.madrasati.LessonPrepScreen
import com.zakiy.platform.ui.madrasati.MadrasatiHubScreen
import com.zakiy.platform.ui.madrasati.ResultsAnalysisScreen
import com.zakiy.platform.ui.madrasati.StudyPlanScreen
import com.zakiy.platform.ui.messages.MessagesScreen
import com.zakiy.platform.ui.messages.NotificationsScreen
import com.zakiy.platform.ui.messages.ThreadScreen
import com.zakiy.platform.ui.navigation.Screen
import com.zakiy.platform.ui.quizzes.QuizDetailScreen
import com.zakiy.platform.ui.quizzes.QuizEditScreen
import com.zakiy.platform.ui.quizzes.QuizzesScreen
import com.zakiy.platform.ui.rooms.RoomLobbyScreen
import com.zakiy.platform.ui.rooms.RoomScreen
import com.zakiy.platform.ui.sciencelab.ScienceLabScreen
import com.zakiy.platform.ui.school.SchoolAttendanceScreen
import com.zakiy.platform.ui.school.SchoolClassesScreen
import com.zakiy.platform.ui.school.SchoolDashboardScreen
import com.zakiy.platform.ui.school.SchoolStaffScreen
import com.zakiy.platform.ui.school.SchoolStudentsScreen
import com.zakiy.platform.ui.teacher.TeacherAttendanceScreen
import com.zakiy.platform.ui.teacher.TeacherDashboardScreen
import com.zakiy.platform.ui.teacher.TeacherPerformanceScreen
import com.zakiy.platform.ui.teacher.TeacherRosterScreen
import com.zakiy.platform.ui.teacher.TeacherScheduleScreen

/** يوجّه للوحة المناسبة حسب الدور المؤسسي - كل لوحة تحل محل التبويبات
 * العادية بالكامل (نفس منطق RoleRoutedView بتطبيق iOS). */
@Composable
fun RoleNavHost(role: AccountRole, authManager: AuthManager) {
    val navController = rememberNavController()
    val start = when (role) {
        AccountRole.Admin -> Screen.AdminDashboard
        AccountRole.SchoolAdmin, AccountRole.SchoolAdministration -> Screen.SchoolDashboard
        AccountRole.Teacher -> Screen.TeacherDashboard
        // الطالب ما يوصل هذا الـ NavHost إطلاقًا (يكمل بـ MainNavHost العادي) -
        // موجود هنا بس عشان switch يكون شامل كل حالات enum
        AccountRole.Student -> Screen.TeacherDashboard
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Screen.AdminDashboard) {
            AdminDashboardScreen(
                authManager = authManager,
                onOpenAiAssistant = { navController.navigate(Screen.AiConversations) },
                onOpenMadrasati = { navController.navigate(Screen.MadrasatiHub) },
                onOpenScienceLab = { navController.navigate(Screen.ScienceLab) },
                onOpenRoboticsLab = { navController.navigate(Screen.RoboticsLab) },
            )
        }

        composable(Screen.SchoolDashboard) {
            SchoolDashboardScreen(
                authManager = authManager,
                onOpenTeachers = { navController.navigate(Screen.SchoolTeachers) },
                onOpenAdministration = { navController.navigate(Screen.SchoolAdministration) },
                onOpenStudents = { navController.navigate(Screen.SchoolStudents) },
                onOpenClasses = { navController.navigate(Screen.SchoolClasses) },
                onOpenAttendance = { navController.navigate(Screen.SchoolAttendance) },
                onOpenLibrary = { navController.navigate(Screen.SchoolLibrary) },
                onOpenMessages = { navController.navigate(Screen.Messages) },
                onOpenAiAssistant = { navController.navigate(Screen.AiConversations) },
                onOpenMadrasati = { navController.navigate(Screen.MadrasatiHub) },
                onOpenScienceLab = { navController.navigate(Screen.ScienceLab) },
                onOpenRoboticsLab = { navController.navigate(Screen.RoboticsLab) },
            )
        }
        composable(Screen.SchoolTeachers) { SchoolStaffScreen(isTeachers = true, authManager = authManager) }
        composable(Screen.SchoolAdministration) { SchoolStaffScreen(isTeachers = false, authManager = authManager) }
        composable(Screen.SchoolStudents) { SchoolStudentsScreen() }
        composable(Screen.SchoolClasses) { SchoolClassesScreen() }
        composable(Screen.SchoolAttendance) { SchoolAttendanceScreen() }
        composable(Screen.SchoolLibrary) {
            LibraryScreen(authManager = authManager, onOpenBook = { navController.navigate(Screen.libraryDetail(it)) })
        }

        composable(Screen.TeacherDashboard) {
            TeacherDashboardScreen(
                onOpenRoster = { navController.navigate(Screen.TeacherRoster) },
                onOpenPerformance = { navController.navigate(Screen.TeacherPerformance) },
                onOpenSchedule = { navController.navigate(Screen.TeacherSchedule) },
                onOpenAttendance = { navController.navigate(Screen.TeacherAttendance) },
                onOpenLibrary = { navController.navigate(Screen.TeacherLibrary) },
                onOpenMessages = { navController.navigate(Screen.Messages) },
                onOpenAssignments = { navController.navigate(Screen.Assignments) },
                onOpenQuizzes = { navController.navigate(Screen.Quizzes) },
                onOpenGradesheet = { navController.navigate(Screen.Gradesheet) },
                onOpenAiAssistant = { navController.navigate(Screen.AiConversations) },
                onOpenMadrasati = { navController.navigate(Screen.MadrasatiHub) },
                onOpenScienceLab = { navController.navigate(Screen.ScienceLab) },
                onOpenRoboticsLab = { navController.navigate(Screen.RoboticsLab) },
                onEnterRoom = { code -> navController.navigate(Screen.room(code, "classroom", true)) },
            )
        }
        composable(Screen.TeacherRoster) { TeacherRosterScreen() }
        composable(Screen.TeacherPerformance) { TeacherPerformanceScreen() }
        composable(Screen.TeacherSchedule) { TeacherScheduleScreen() }
        composable(Screen.TeacherAttendance) { TeacherAttendanceScreen() }
        composable(Screen.Gradesheet) { GradesheetScreen() }
        composable(Screen.TeacherLibrary) {
            LibraryScreen(authManager = authManager, onOpenBook = { navController.navigate(Screen.libraryDetail(it)) })
        }
        composable(Screen.LibraryDetailPattern) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId").orEmpty()
            LibraryDetailScreen(bookId = bookId, onBack = { navController.popBackStack() })
        }
        // دفتر الواجبات - معلم هنا (isTeacher=true)، نفس المسار يُستخدم بجهة
        // الطالب من MainNavHost بمعطى مختلف
        composable(Screen.Assignments) {
            AssignmentsScreen(
                authManager = authManager,
                onOpenAssignment = { id -> navController.navigate(Screen.assignmentDetail(id)) },
            )
        }
        composable(Screen.AssignmentDetailPattern) { backStackEntry ->
            val assignmentId = backStackEntry.arguments?.getString("assignmentId").orEmpty()
            AssignmentDetailScreen(assignmentId = assignmentId, isTeacher = true, onBack = { navController.popBackStack() })
        }
        // الاختبارات - معلم هنا (isTeacher يُستنتج داخل QuizzesScreen من الدور
        // نفسه)، الطالب يستخدم QuizzesScreen/QuizTakeScreen من MainNavHost
        composable(Screen.Quizzes) {
            QuizzesScreen(
                authManager = authManager,
                onOpenQuiz = { id, _ -> navController.navigate(Screen.quizDetail(id)) },
                onCreateQuiz = { navController.navigate(Screen.QuizCreate) },
            )
        }
        composable(Screen.QuizCreate) {
            QuizEditScreen(quizId = null, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }
        composable(Screen.QuizEditPattern) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId").orEmpty()
            QuizEditScreen(quizId = quizId, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }
        composable(Screen.QuizDetailPattern) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId").orEmpty()
            QuizDetailScreen(
                quizId = quizId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.quizEdit(id)) },
            )
        }

        // الرسائل متاحة لكل الأدوار المؤسسية من داخل لوحاتها
        composable(Screen.Messages) {
            MessagesScreen(
                authManager = authManager,
                onOpenThread = { userId, username -> navController.navigate(Screen.thread(userId, username)) },
                onOpenNotifications = { navController.navigate(Screen.Notifications) },
            )
        }
        composable(Screen.ThreadPattern) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId").orEmpty()
            val username = backStackEntry.arguments?.getString("username").orEmpty()
            ThreadScreen(otherUserId = userId, otherUsername = username, authManager = authManager, onBack = { navController.popBackStack() })
        }
        composable(Screen.Notifications) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onOpenThread = { userId, username -> navController.navigate(Screen.thread(userId, username)) },
            )
        }

        // المساعد الذكي - متاح لكل الأدوار المؤسسية (نفس تسجيل المسارات
        // بـ MainNavHost بالضبط، الطالب يستخدم تلك النسخة بدل هذي)
        composable(Screen.AiConversations) {
            AiConversationsScreen(onOpenConversation = { id -> navController.navigate(Screen.aiConversation(id)) })
        }
        composable(Screen.AiConversationPattern) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
            val pendingTitle by backStackEntry.savedStateHandle.getStateFlow<String?>("ai_pending_book_title", null).collectAsStateWithLifecycle()
            val pendingText by backStackEntry.savedStateHandle.getStateFlow<String?>("ai_pending_book_text", null).collectAsStateWithLifecycle()
            AiConversationScreen(
                conversationId = conversationId,
                pendingBookTitle = pendingTitle,
                pendingBookText = pendingText,
                onConsumedPendingBook = {
                    backStackEntry.savedStateHandle["ai_pending_book_title"] = null
                    backStackEntry.savedStateHandle["ai_pending_book_text"] = null
                },
                onOpenBookPicker = { navController.navigate(Screen.aiBookPicker(conversationId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.AiBookPickerPattern) {
            AiBookPickerScreen(
                onPicked = { bookTitle, bookText ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("ai_pending_book_title", bookTitle)
                    navController.previousBackStackEntry?.savedStateHandle?.set("ai_pending_book_text", bookText)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }

        // مدرستي وأدوات ذكيّ - متاحة لكل الأدوار المؤسسية (نفس تسجيل مسارات
        // المساعد الذكي أعلاه بالضبط، الطالب يستخدم نسخة MainNavHost)
        composable(Screen.MadrasatiHub) {
            MadrasatiHubScreen(
                authManager = authManager,
                onBack = { navController.popBackStack() },
                onOpenAssignments = { navController.navigate(Screen.Assignments) },
                onOpenQuizzes = { navController.navigate(Screen.Quizzes) },
                onOpenGradesheet = { navController.navigate(Screen.Gradesheet) },
                // جدولي (جدول الطالب الشخصي) غير قابل للوصول إطلاقًا من هذا
                // الـ NavHost - الطالب (الدور الوحيد اللي يفعّل هذي البطاقة)
                // يكمل بـ MainNavHost دايمًا (RootApp)، فما يوصل
                // MadrasatiHubScreen من هنا أبدًا
                onOpenStudentSchedule = {},
                onOpenLibrary = { navController.navigate(Screen.TeacherLibrary) },
                onOpenAiAssistant = { navController.navigate(Screen.AiConversations) },
                onNewLessonPrep = { navController.navigate(Screen.LessonPrepCreate) },
                onOpenLessonPrep = { id -> navController.navigate(Screen.lessonPrepView(id)) },
                onOpenEnrichment = { navController.navigate(Screen.Enrichment) },
                onOpenResultsAnalysis = { navController.navigate(Screen.ResultsAnalysis) },
                onNewHomeworkHelp = { navController.navigate(Screen.HomeworkHelpCreate) },
                onOpenHomeworkHelp = { id -> navController.navigate(Screen.homeworkHelpView(id)) },
                onNewStudyPlan = { navController.navigate(Screen.StudyPlanCreate) },
                onOpenStudyPlan = { id -> navController.navigate(Screen.studyPlanView(id)) },
            )
        }
        composable(Screen.LessonPrepCreate) { LessonPrepScreen(prepId = null, onBack = { navController.popBackStack() }) }
        composable(Screen.LessonPrepViewPattern) { backStackEntry ->
            val prepId = backStackEntry.arguments?.getString("prepId")
            LessonPrepScreen(prepId = prepId, onBack = { navController.popBackStack() })
        }
        composable(Screen.Enrichment) { EnrichmentScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.ResultsAnalysis) { ResultsAnalysisScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.HomeworkHelpCreate) { HomeworkHelpScreen(sessionId = null, onBack = { navController.popBackStack() }) }
        composable(Screen.HomeworkHelpViewPattern) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            HomeworkHelpScreen(sessionId = sessionId, onBack = { navController.popBackStack() })
        }
        composable(Screen.StudyPlanCreate) { StudyPlanScreen(planId = null, onBack = { navController.popBackStack() }) }
        composable(Screen.StudyPlanViewPattern) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId")
            StudyPlanScreen(planId = planId, onBack = { navController.popBackStack() })
        }

        // مختبر العلوم ومعمل الروبوتات - متاحة لكل الأدوار المؤسسية، نفس
        // مبدأ تسجيل المسارات أعلاه بالضبط (بدون أي تقييد دور)
        composable(Screen.ScienceLab) {
            ScienceLabScreen(authManager = authManager, onBack = { navController.popBackStack() })
        }
        composable(Screen.RoboticsLab) {
            EmbeddedWebScreen(
                titleRes = R.string.nav_robotics_lab,
                authManager = authManager,
                onBack = { navController.popBackStack() },
                navigateJsFunction = "showRoboticsLabScreen",
            )
        }

        composable(Screen.RoomPattern) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code").orEmpty()
            val type = backStackEntry.arguments?.getString("type").orEmpty()
            val isCreator = backStackEntry.arguments?.getString("isCreator").toBoolean()
            RoomScreen(roomCode = code, roomType = type, isCreator = isCreator, authManager = authManager, onLeave = { navController.popBackStack() })
        }
    }
}
