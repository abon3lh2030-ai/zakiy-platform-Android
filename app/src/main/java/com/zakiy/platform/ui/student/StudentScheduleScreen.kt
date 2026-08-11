package com.zakiy.platform.ui.student

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.ClassScheduleEntry

/** جدول حصص الطالب - متاح بس لطالب مرتبط بمدرسة (currentUserClassId موجود)،
 * وصول إضافي من إعدادات الحساب الفردي العادي - مو لوحة مستقلة. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScheduleScreen(authManager: AuthManager, onBack: () -> Unit) {
    var schedule by remember { mutableStateOf<List<ClassScheduleEntry>>(emptyList()) }
    LaunchedEffect(Unit) { schedule = runCatching { NetworkModule.backendApi.studentSchedule().schedule }.getOrDefault(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_my_schedule)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
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
