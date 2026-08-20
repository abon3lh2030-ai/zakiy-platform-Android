package com.zakiy.platform.ui.school

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.SchoolInfo
import com.zakiy.platform.ui.components.DashboardMenuRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolDashboardScreen(
    authManager: AuthManager,
    onOpenTeachers: () -> Unit,
    onOpenAdministration: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenAiAssistant: () -> Unit,
) {
    var info by remember { mutableStateOf<SchoolInfo?>(null) }
    LaunchedEffect(Unit) { info = runCatching { NetworkModule.backendApi.schoolInfo() }.getOrNull() }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.school_dash_heading)) }) }) { padding ->
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            info?.let {
                LinearProgressIndicator(
                    progress = { if (it.maxAccounts > 0) it.accountsUsed.toFloat() / it.maxAccounts else 0f },
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                )
                Text("${it.accountsUsed} / ${it.maxAccounts}", modifier = Modifier.padding(horizontal = 16.dp))
            }
            LazyColumn {
                item { DashboardMenuRow(Icons.Filled.SupervisorAccount, Color(0xFF3F51B5), stringResource(R.string.tab_teachers), onOpenTeachers) }
                item { DashboardMenuRow(Icons.Filled.PersonAddAlt, Color(0xFF795548), stringResource(R.string.admin_staff_heading), onOpenAdministration) }
                item { DashboardMenuRow(Icons.Filled.Group, Color(0xFF009688), stringResource(R.string.tab_students), onOpenStudents) }
                item { DashboardMenuRow(Icons.Filled.Label, Color(0xFFFF9800), stringResource(R.string.tab_classes), onOpenClasses) }
                item { DashboardMenuRow(Icons.Filled.Assignment, Color(0xFF9C27B0), stringResource(R.string.tab_attendance), onOpenAttendance) }
                item { DashboardMenuRow(Icons.Filled.Book, Color(0xFF3949AB), stringResource(R.string.tab_library), onOpenLibrary) }
                item { DashboardMenuRow(Icons.Filled.Message, Color(0xFF2E8B77), stringResource(R.string.nav_messages), onOpenMessages) }
                item { DashboardMenuRow(Icons.Filled.SmartToy, Color(0xFF6D4AFF), stringResource(R.string.ai_assistant), onOpenAiAssistant) }
            }
        }
    }
}
