package com.zakiy.platform.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.ui.archive.ArchiveScreen
import com.zakiy.platform.ui.assignments.AssignmentDetailScreen
import com.zakiy.platform.ui.assignments.AssignmentsScreen
import com.zakiy.platform.ui.friends.FriendsScreen
import com.zakiy.platform.ui.home.HomeNavHost
import com.zakiy.platform.ui.library.LibraryDetailScreen
import com.zakiy.platform.ui.library.LibraryScreen
import com.zakiy.platform.ui.messages.MessagesScreen
import com.zakiy.platform.ui.messages.NotificationsScreen
import com.zakiy.platform.ui.messages.ThreadScreen
import com.zakiy.platform.ui.navigation.Screen
import com.zakiy.platform.ui.notes.NoteEditorScreen
import com.zakiy.platform.ui.notes.NotesScreen
import com.zakiy.platform.ui.performance.PerformanceScreen
import com.zakiy.platform.ui.profile.EditProfileScreen
import com.zakiy.platform.ui.profile.ProfileScreen
import com.zakiy.platform.ui.settings.SettingsScreen
import com.zakiy.platform.ui.settings.SubscriptionScreen
import com.zakiy.platform.ui.student.StudentScheduleScreen

private data class BottomTab(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelRes: Int)

private val bottomTabs = listOf(
    BottomTab(Screen.Home, Icons.Filled.Home, R.string.nav_home),
    BottomTab(Screen.Library, Icons.Filled.Book, R.string.nav_library),
    BottomTab(Screen.Performance, Icons.Filled.BarChart, R.string.nav_performance),
    BottomTab(Screen.Messages, Icons.Filled.Message, R.string.nav_messages),
    BottomTab(Screen.Settings, Icons.Filled.Menu, R.string.nav_settings),
)

@Composable
fun MainNavHost(authManager: AuthManager) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Home) { HomeNavHost(authManager = authManager) }
            composable(Screen.Library) {
                LibraryScreen(
                    authManager = authManager,
                    onOpenBook = { navController.navigate(Screen.libraryDetail(it)) },
                )
            }
            composable(Screen.LibraryDetailPattern) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId").orEmpty()
                LibraryDetailScreen(bookId = bookId, onBack = { navController.popBackStack() })
            }
            composable(Screen.Performance) { PerformanceScreen() }
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
            composable(Screen.Settings) {
                SettingsScreen(
                    authManager = authManager,
                    onOpenProfile = { navController.navigate(Screen.MyProfile) },
                    onOpenEditProfile = { navController.navigate(Screen.EditProfile) },
                    onOpenFriends = { navController.navigate(Screen.Friends) },
                    onOpenArchive = { navController.navigate(Screen.Archive) },
                    onOpenSubscription = { navController.navigate(Screen.Subscription) },
                    onOpenStudentSchedule = { navController.navigate(Screen.StudentSchedule) },
                    onOpenNotes = { navController.navigate(Screen.Notes) },
                    onOpenAssignments = { navController.navigate(Screen.Assignments) },
                )
            }
            composable(Screen.Notes) {
                NotesScreen(onOpenNote = { noteId -> navController.navigate(Screen.noteEditor(noteId)) })
            }
            composable(Screen.NoteEditorPattern) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId").orEmpty()
                NoteEditorScreen(noteId = noteId, onBack = { navController.popBackStack() })
            }
            // دفتر الواجبات - طالب بس هنا (نفس المسار يُستخدم بجهة المعلم من
            // RoleNavHost بمعطى isTeacher=true مختلف)
            composable(Screen.Assignments) {
                AssignmentsScreen(
                    authManager = authManager,
                    onOpenAssignment = { id -> navController.navigate(Screen.assignmentDetail(id)) },
                )
            }
            composable(Screen.AssignmentDetailPattern) { backStackEntry ->
                val assignmentId = backStackEntry.arguments?.getString("assignmentId").orEmpty()
                AssignmentDetailScreen(assignmentId = assignmentId, isTeacher = false, onBack = { navController.popBackStack() })
            }
            composable(Screen.StudentSchedule) { StudentScheduleScreen(authManager = authManager, onBack = { navController.popBackStack() }) }
            composable(Screen.MyProfile) { ProfileScreen(userId = null, authManager = authManager, onBack = { navController.popBackStack() }) }
            composable(Screen.ProfilePattern) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                ProfileScreen(userId = userId, authManager = authManager, onBack = { navController.popBackStack() })
            }
            composable(Screen.EditProfile) { EditProfileScreen(authManager = authManager, onBack = { navController.popBackStack() }) }
            composable(Screen.Friends) { FriendsScreen(onOpenProfile = { navController.navigate(Screen.profile(it)) }) }
            composable(Screen.Archive) { ArchiveScreen() }
            composable(Screen.Subscription) { SubscriptionScreen() }
        }
    }
}
