package com.zakiy.platform.ui.assignments

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.AssignmentDetail
import com.zakiy.platform.network.dto.AssignmentStudentStatus
import com.zakiy.platform.network.dto.GradeRequest
import com.zakiy.platform.util.uriToMultipartAny
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailScreen(assignmentId: String, isTeacher: Boolean, onBack: () -> Unit) {
    var assignment by remember { mutableStateOf<AssignmentDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        isLoading = true
        errorMessage = null
        assignment = runCatching {
            if (isTeacher) NetworkModule.backendApi.teacherAssignmentDetail(assignmentId)
            else NetworkModule.backendApi.studentAssignmentDetail(assignmentId)
        }.getOrElse {
            errorMessage = it.message
            null
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(assignment?.title.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        val current = assignment
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            current == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(errorMessage ?: stringResource(R.string.error_generic))
            }
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(current.subject, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(current.title, style = MaterialTheme.typography.headlineSmall)
                if (current.content.isNotBlank()) {
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(current.content, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.size(20.dp))

                if (isTeacher) {
                    TeacherStudentsSection(assignmentId = assignmentId, students = current.students ?: emptyList())
                } else {
                    StudentSubmissionSection(assignmentId = assignmentId, submission = current.submission) { load() }
                }
            }
        }
    }
}

@Composable
private fun TeacherStudentsSection(assignmentId: String, students: List<AssignmentStudentStatus>) {
    Text(stringResource(R.string.assignment_students_heading), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.size(8.dp))
    if (students.isEmpty()) {
        Text(stringResource(R.string.assignment_no_students), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    students.forEach { student ->
        StudentSubmissionRow(assignmentId = assignmentId, student = student)
        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
private fun StudentSubmissionRow(assignmentId: String, student: AssignmentStudentStatus) {
    var gradeText by remember(student.userId) { mutableStateOf(student.grade ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(student.fullName ?: student.username, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    stringResource(if (student.submitted) R.string.assignment_status_done else R.string.assignment_status_pending),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (student.submitted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            if (student.submitted) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = {
                        scope.launch {
                            val url = runCatching { NetworkModule.backendApi.teacherSubmissionFileUrl(assignmentId, student.userId).url }.getOrNull()
                            if (url != null) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }) { Text(stringResource(R.string.btn_view_file)) }
                    Spacer(modifier = Modifier.size(8.dp))
                    OutlinedTextField(
                        value = gradeText,
                        onValueChange = { gradeText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.assignment_grade_label)) },
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                runCatching { NetworkModule.backendApi.gradeAssignmentSubmission(assignmentId, student.userId, GradeRequest(gradeText)) }
                                isSaving = false
                            }
                        },
                        enabled = !isSaving,
                    ) { Text(stringResource(R.string.btn_save_grade)) }
                }
            }
        }
    }
}

@Composable
private fun StudentSubmissionSection(assignmentId: String, submission: com.zakiy.platform.network.dto.AssignmentSubmission?, onSubmitted: suspend () -> Unit) {
    if (submission != null) {
        Column {
            Text(stringResource(R.string.assignment_submitted_file_label, submission.fileName), style = MaterialTheme.typography.bodyMedium)
            if (!submission.note.isNullOrBlank()) {
                Text(stringResource(R.string.assignment_note_shown, submission.note), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                submission.grade?.let { stringResource(R.string.assignment_grade_shown, it) } ?: stringResource(R.string.assignment_not_graded_yet),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        pickedUri = uri
        pickedName = uri?.lastPathSegment
    }

    Text(stringResource(R.string.assignment_submit_heading), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.size(8.dp))
    OutlinedButton(onClick = { filePicker.launch("*/*") }) {
        Icon(Icons.Filled.AttachFile, contentDescription = null)
        Spacer(modifier = Modifier.size(6.dp))
        Text(pickedName ?: stringResource(R.string.btn_pick_assignment_file))
    }
    Spacer(modifier = Modifier.size(8.dp))
    OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text(stringResource(R.string.assignment_note_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
    if (errorMessage != null) {
        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
    }
    Spacer(modifier = Modifier.size(12.dp))
    Button(
        onClick = {
            val uri = pickedUri ?: return@Button
            isSubmitting = true
            errorMessage = null
            scope.launch {
                try {
                    val part = uriToMultipartAny(context, uri, fieldName = "file")
                    val notePart = note.toRequestBody("text/plain".toMediaTypeOrNull())
                    NetworkModule.backendApi.submitAssignment(assignmentId, part, notePart)
                    onSubmitted()
                } catch (e: Exception) {
                    errorMessage = e.message
                } finally {
                    isSubmitting = false
                }
            }
        },
        enabled = pickedUri != null && !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_submit_assignment))
    }
}
