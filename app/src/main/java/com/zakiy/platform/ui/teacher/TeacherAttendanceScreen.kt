package com.zakiy.platform.ui.teacher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.ManualAttendanceRecord
import com.zakiy.platform.network.dto.SaveManualAttendanceRequest
import com.zakiy.platform.network.dto.SchoolStudent
import kotlinx.coroutines.launch

private val statuses = listOf("present", "late", "absent")
private val statusLabels = mapOf("present" to "حاضر", "late" to "متأخر", "absent" to "غايب")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendanceScreen(onBack: () -> Unit) {
    var students by remember { mutableStateOf<List<SchoolStudent>>(emptyList()) }
    var statusByStudent by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var classId by remember { mutableStateOf<String?>(null) }
    val today = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val roster = runCatching { NetworkModule.backendApi.teacherRoster() }.getOrNull()
        students = roster?.students ?: emptyList()
        classId = roster?.classes?.firstOrNull()?.id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_attendance)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(students, key = { it.userId }) { student ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(student.fullName ?: student.username, style = MaterialTheme.typography.titleMedium)
                            Row {
                                statuses.forEach { status ->
                                    FilterChip(
                                        selected = statusByStudent[student.userId] == status,
                                        onClick = { statusByStudent = statusByStudent + (student.userId to status) },
                                        label = { Text(statusLabels[status] ?: status) },
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val cid = classId ?: return@Button
                    scope.launch {
                        runCatching {
                            NetworkModule.backendApi.teacherSaveManualAttendance(
                                SaveManualAttendanceRequest(
                                    classId = cid,
                                    date = today,
                                    records = statusByStudent.map { (studentId, status) -> ManualAttendanceRecord(studentId, status) },
                                ),
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}
