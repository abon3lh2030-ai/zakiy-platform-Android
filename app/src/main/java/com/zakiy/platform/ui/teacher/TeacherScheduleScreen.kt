package com.zakiy.platform.ui.teacher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.ClassScheduleEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScheduleScreen() {
    var schedule by remember { mutableStateOf<List<ClassScheduleEntry>>(emptyList()) }
    LaunchedEffect(Unit) { schedule = runCatching { NetworkModule.backendApi.teacherSchedule().schedule }.getOrDefault(emptyList()) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_schedule)) }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            items(schedule) { entry ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(entry.subject ?: "—", style = MaterialTheme.typography.titleMedium)
                        Text("${entry.startTime} - ${entry.endTime}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
