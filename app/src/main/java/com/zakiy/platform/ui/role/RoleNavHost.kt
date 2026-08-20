package com.zakiy.platform.ui.role

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zakiy.platform.network.AccountRole
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.ui.admin.AdminDashboardScreen
import com.zakiy.platform.ui.ai.AiBookPickerScreen
import com.zakiy.platform.ui.ai.AiConversationScreen
import com.zakiy.platform.ui.ai.AiConversationsScreen
import com.zakiy.platform.ui.assignments.AssignmentDetailScreen
import com.zakiy.platform.ui.assignments.AssignmentsScreen
import com.zakiy.platform.ui.library.LibraryDetailScreen
import com.zakiy.platform.ui.library.LibraryScreen
import com.zakiy.platform.ui.messages.MessagesScreen
import com.zakiy.platform.ui.messages.NotificationsScreen
import com.zakiy.platform.ui.messages.ThreadScreen
import com.zakiy.platform.ui.navigation.Screen
import com.zakiy.platform.ui.rooms.RoomLobbyScreen
import com.zakiy.platform.ui.rooms.RoomScreen
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
                onOpenAiAssistant = { navController.navigate(Screen.AiConversations) },
                onEnterRoom = { code -> navController.navigate(Screen.room(code, "classroom", true)) },
            )
        }
        composable(Screen.TeacherRoster) { TeacherRosterScreen() }
        composable(Screen.TeacherPerformance) { TeacherPerformanceScreen() }
        composable(Screen.TeacherSchedule) { TeacherScheduleScreen() }
        composable(Screen.TeacherAttendance) { TeacherAttendanceScreen() }
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

        composable(Screen.RoomPattern) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code").orEmpty()
            val type = backStackEntry.arguments?.getString("type").orEmpty()
            val isCreator = backStackEntry.arguments?.getString("isCreator").toBoolean()
            RoomScreen(roomCode = code, roomType = type, isCreator = isCreator, authManager = authManager, onLeave = { navController.popBackStack() })
        }
    }
}
