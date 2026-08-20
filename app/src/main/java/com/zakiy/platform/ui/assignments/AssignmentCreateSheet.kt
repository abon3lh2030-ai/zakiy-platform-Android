package com.zakiy.platform.ui.assignments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zakiy.platform.network.dto.CreateAssignmentRequest
import com.zakiy.platform.network.dto.SchoolClassSummary
import com.zakiy.platform.network.dto.SchoolStudent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentCreateSheet(onDismiss: () -> Unit, onCreated: () -> Unit) {
    var classes by remember { mutableStateOf<List<SchoolClassSummary>>(emptyList()) }
    var students by remember { mutableStateOf<List<SchoolStudent>>(emptyList()) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var selectedTargetId by remember { mutableStateOf<String?>(null) }
    var subject by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var classMenuExpanded by remember { mutableStateOf(false) }
    var targetMenuExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val roster = runCatching { NetworkModule.backendApi.teacherRoster() }.getOrNull()
        classes = roster?.classes ?: emptyList()
        students = roster?.students ?: emptyList()
        selectedClassId = classes.firstOrNull()?.id
    }

    val studentsInClass = students.filter { it.classId == selectedClassId }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.assignment_new_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.size(12.dp))

            // اختيار الفصل
            Box {
                TextButton(onClick = { classMenuExpanded = true }) {
                    Text(classes.firstOrNull { it.id == selectedClassId }?.name ?: stringResource(R.string.assignment_class_label))
                }
                DropdownMenu(expanded = classMenuExpanded, onDismissRequest = { classMenuExpanded = false }) {
                    classes.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = {
                            selectedClassId = c.id
                            selectedTargetId = null
                            classMenuExpanded = false
                        })
                    }
                }
            }

            // اختيار الطالب المستهدف (أو الكل)
            Box {
                TextButton(onClick = { targetMenuExpanded = true }) {
                    Text(
                        studentsInClass.firstOrNull { it.userId == selectedTargetId }?.let { it.fullName ?: it.username }
                            ?: stringResource(R.string.assignment_target_all)
                    )
                }
                DropdownMenu(expanded = targetMenuExpanded, onDismissRequest = { targetMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.assignment_target_all)) }, onClick = {
                        selectedTargetId = null
                        targetMenuExpanded = false
                    })
                    studentsInClass.forEach { s ->
                        DropdownMenuItem(text = { Text(s.fullName ?: s.username) }, onClick = {
                            selectedTargetId = s.userId
                            targetMenuExpanded = false
                        })
                    }
                }
            }

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text(stringResource(R.string.assignment_subject_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.assignment_title_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.assignment_content_placeholder)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                minLines = 4,
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = {
                    val classId = selectedClassId
                    if (classId == null) {
                        errorMessage = null
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null
                    scope.launch {
                        try {
                            NetworkModule.backendApi.createAssignment(
                                CreateAssignmentRequest(
                                    classId = classId,
                                    targetStudentId = selectedTargetId,
                                    subject = subject,
                                    title = title,
                                    content = content,
                                )
                            )
                            onCreated()
                        } catch (e: Exception) {
                            errorMessage = e.message
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && selectedClassId != null && subject.isNotBlank() && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_create_assignment))
            }
        }
    }
}
