package com.zakiy.platform.ui.assignments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.AssignmentSummary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(authManager: AuthManager, onOpenAssignment: (String) -> Unit) {
    val role by authManager.role.collectAsStateWithLifecycle()
    val isTeacher = role == "teacher"

    var assignments by remember { mutableStateOf<List<AssignmentSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        isLoading = true
        assignments = runCatching {
            if (isTeacher) NetworkModule.backendApi.teacherAssignments().assignments
            else NetworkModule.backendApi.studentAssignments().assignments
        }.getOrDefault(emptyList())
        isLoading = false
    }

    LaunchedEffect(isTeacher) { reload() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.assignments_heading)) }) },
        floatingActionButton = {
            if (isTeacher) {
                FloatingActionButton(onClick = { showCreateSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            assignments.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.assignments_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                items(assignments, key = { it.id }) { assignment ->
                    AssignmentCard(assignment = assignment, isTeacher = isTeacher, onClick = { onOpenAssignment(assignment.id) })
                }
            }
        }
    }

    if (showCreateSheet) {
        AssignmentCreateSheet(
            onDismiss = { showCreateSheet = false },
            onCreated = {
                showCreateSheet = false
                scope.launch { reload() }
            },
        )
    }
}

@Composable
private fun AssignmentCard(assignment: AssignmentSummary, isTeacher: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(assignment.title, style = MaterialTheme.typography.titleSmall)
                Text(assignment.subject, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val statusText = if (isTeacher) {
                val done = assignment.submittedCount ?: 0
                val total = assignment.totalCount ?: 0
                stringResource(R.string.assignment_submitted_count, done, total)
            } else {
                stringResource(if (assignment.submitted == true) R.string.assignment_status_done else R.string.assignment_status_pending)
            }
            val isDone = if (isTeacher) {
                (assignment.submittedCount ?: 0) >= (assignment.totalCount ?: 0) && (assignment.totalCount ?: 0) > 0
            } else {
                assignment.submitted == true
            }
            Text(
                statusText,
                style = MaterialTheme.typography.labelMedium,
                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}
