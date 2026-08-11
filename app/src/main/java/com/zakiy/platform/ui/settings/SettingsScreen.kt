package com.zakiy.platform.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authManager: AuthManager,
    onOpenProfile: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenStudentSchedule: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isAuthenticated by authManager.isAuthenticated.collectAsStateWithLifecycle()
    val username by authManager.username.collectAsStateWithLifecycle()
    val role by authManager.role.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_heading)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isAuthenticated) {
                ListItem(
                    headlineContent = { Text(username) },
                    leadingContent = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                )
                HorizontalDivider()
                SettingsRow(Icons.Filled.AccountCircle, stringResource(R.string.my_profile), onOpenProfile)
                SettingsRow(Icons.Filled.Edit, stringResource(R.string.edit_profile), onOpenEditProfile)

                // حساب مؤسسي (role موجود) وصوله محكوم بعضوية مدرسته لا باشتراك
                // شخصي - ما نعرض له زر الاشتراك إطلاقًا (نفس قاعدة iOS/الموقع)
                if (role == null) {
                    SettingsRow(Icons.Filled.Star, stringResource(R.string.subscription), onOpenSubscription)
                }
                SettingsRow(Icons.Filled.Group, stringResource(R.string.nav_friends), onOpenFriends)
                SettingsRow(Icons.Filled.Archive, stringResource(R.string.nav_archive), onOpenArchive)
                // طالب مرتبط بمدرسة (role="student") يشوف جدوله من هنا -
                // إضافة بسيطة فوق تجربة الحساب الفردي العادية، بدون أي لوحة مستقلة
                if (role == "student") {
                    SettingsRow(Icons.Filled.CalendarMonth, stringResource(R.string.nav_my_schedule), onOpenStudentSchedule)
                }
                HorizontalDivider()
                SettingsRow(Icons.Filled.Logout, stringResource(R.string.logout)) {
                    scope.launch { authManager.signOut() }
                }
            } else {
                Text(
                    stringResource(R.string.login),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        ListItem(
            headlineContent = { Text(title) },
            leadingContent = { Icon(icon, contentDescription = null) },
        )
    }
}
