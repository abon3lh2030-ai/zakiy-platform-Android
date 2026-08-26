package com.zakiy.platform.ui.teacher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.CreateRoomRequest
import com.zakiy.platform.network.dto.SchoolClassSummary
import com.zakiy.platform.ui.components.DashboardMenuRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    onOpenRoster: () -> Unit,
    onOpenPerformance: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenAssignments: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenMadrasati: () -> Unit,
    onEnterRoom: (String) -> Unit,
) {
    var classes by remember { mutableStateOf<List<SchoolClassSummary>>(emptyList()) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        classes = runCatching { NetworkModule.backendApi.teacherRoster().classes }.getOrDefault(emptyList())
        selectedClassId = classes.firstOrNull()?.id
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.teacher_dash_heading)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (classes.isNotEmpty()) {
                    Row {
                        Text(classes.firstOrNull { it.id == selectedClassId }?.name ?: "")
                        TextButton(onClick = { menuExpanded = true }) { Text("▾") }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            classes.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedClassId = c.id; menuExpanded = false })
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        val classId = selectedClassId ?: return@Button
                        isStarting = true
                        scope.launch {
                            val result = runCatching {
                                NetworkModule.backendApi.createRoom(CreateRoomRequest(roomType = "classroom", classId = classId))
                            }.getOrNull()
                            isStarting = false
                            if (result != null) onEnterRoom(result.roomCode)
                        }
                    },
                    enabled = !isStarting && selectedClassId != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isStarting) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_start_live_class))
                }
            }
            LazyColumn {
                item { DashboardMenuRow(Icons.Filled.Group, Color(0xFF3F51B5), stringResource(R.string.tab_roster), onOpenRoster) }
                item { DashboardMenuRow(Icons.Filled.BarChart, Color(0xFF9C27B0), stringResource(R.string.tab_performance), onOpenPerformance) }
                item { DashboardMenuRow(Icons.Filled.CalendarMonth, Color(0xFFFF9800), stringResource(R.string.tab_schedule), onOpenSchedule) }
                item { DashboardMenuRow(Icons.Filled.Assignment, Color(0xFF009688), stringResource(R.string.tab_attendance), onOpenAttendance) }
                item { DashboardMenuRow(Icons.Filled.Book, Color(0xFF3949AB), stringResource(R.string.tab_library), onOpenLibrary) }
                item { DashboardMenuRow(Icons.Filled.Edit, Color(0xFFE91E63), stringResource(R.string.assignments), onOpenAssignments) }
                item { DashboardMenuRow(Icons.Filled.Quiz, Color(0xFF00897B), stringResource(R.string.quizzes), onOpenQuizzes) }
                item { DashboardMenuRow(Icons.Filled.Message, Color(0xFF2E8B77), stringResource(R.string.nav_messages), onOpenMessages) }
                item { DashboardMenuRow(Icons.Filled.SmartToy, Color(0xFF6D4AFF), stringResource(R.string.ai_assistant), onOpenAiAssistant) }
                item { DashboardMenuRow(Icons.Filled.OpenInBrowser, Color(0xFF00897B), stringResource(R.string.madrasati_heading), onOpenMadrasati) }
            }
        }
    }
}
