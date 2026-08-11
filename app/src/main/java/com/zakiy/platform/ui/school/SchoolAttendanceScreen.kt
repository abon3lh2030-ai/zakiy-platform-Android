package com.zakiy.platform.ui.school

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
import com.zakiy.platform.network.dto.SchoolAttendanceReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolAttendanceScreen() {
    var report by remember { mutableStateOf<SchoolAttendanceReport?>(null) }

    LaunchedEffect(Unit) { report = runCatching { NetworkModule.backendApi.schoolAttendance() }.getOrNull() }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_attendance)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("الحضور التلقائي (${report?.attendance?.size ?: 0})", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(report?.attendance.orEmpty()) { row ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(row.joinedAt ?: "—", modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}
