package com.zakiy.platform.ui.assignments

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.zakiy.platform.network.dto.QuizQuestionDetail
import com.zakiy.platform.network.dto.SubmitAssignmentAnswersRequest
import com.zakiy.platform.network.dto.UpdateAssignmentLinkRequest
import com.zakiy.platform.ui.common.PLATFORM_MADRASATI
import com.zakiy.platform.ui.common.QuestionAnswerCard
import com.zakiy.platform.ui.common.openMadrasatiLink
import com.zakiy.platform.util.uriToMultipartAny
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

private const val SUBMISSION_TYPE_QUESTIONS = "questions"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailScreen(assignmentId: String, isTeacher: Boolean, onBack: () -> Unit) {
    var assignment by remember { mutableStateOf<AssignmentDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

                if (current.platform == PLATFORM_MADRASATI) {
                    if (isTeacher) {
                        MadrasatiAssignmentLinkEditor(assignmentId = assignmentId, externalLink = current.externalLink, onSaved = { scope.launch { load() } })
                    } else {
                        MadrasatiStudentSection(externalLink = current.externalLink)
                    }
                } else if (isTeacher) {
                    TeacherStudentsSection(assignmentId = assignmentId, submissionType = current.submissionType, questions = current.questions ?: emptyList(), students = current.students ?: emptyList())
                } else {
                    StudentSubmissionSection(
                        assignmentId = assignmentId,
                        submissionType = current.submissionType,
                        questions = current.questions ?: emptyList(),
                        submission = current.submission,
                    ) { load() }
                }
            }
        }
    }
}

@Composable
private fun MadrasatiAssignmentLinkEditor(assignmentId: String, externalLink: String?, onSaved: () -> Unit) {
    var linkText by remember(externalLink) { mutableStateOf(externalLink ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Text(stringResource(R.string.madrasati_teacher_link_heading), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.size(8.dp))
    OutlinedTextField(
        value = linkText,
        onValueChange = { linkText = it },
        label = { Text(stringResource(R.string.madrasati_link_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.size(8.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { openMadrasatiLink(context, linkText) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.btn_open_madrasati))
        }
        Spacer(modifier = Modifier.size(8.dp))
        Button(
            onClick = {
                isSaving = true
                scope.launch {
                    runCatching { NetworkModule.backendApi.updateAssignmentLink(assignmentId, UpdateAssignmentLinkRequest(linkText.trim().ifBlank { null })) }
                    isSaving = false
                    onSaved()
                }
            },
            enabled = !isSaving,
            modifier = Modifier.weight(1f),
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text(stringResource(R.string.save))
        }
    }
}

@Composable
private fun MadrasatiStudentSection(externalLink: String?) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.madrasati_platform_notice),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (externalLink.isNullOrBlank()) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                stringResource(R.string.madrasati_no_link_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Button(onClick = { openMadrasatiLink(context, externalLink) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_open_madrasati))
        }
    }
}

@Composable
private fun TeacherStudentsSection(
    assignmentId: String,
    submissionType: String,
    questions: List<QuizQuestionDetail>,
    students: List<AssignmentStudentStatus>,
) {
    Text(stringResource(R.string.assignment_students_heading), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.size(8.dp))
    if (students.isEmpty()) {
        Text(stringResource(R.string.assignment_no_students), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    students.forEach { student ->
        if (submissionType == SUBMISSION_TYPE_QUESTIONS) {
            StudentQuestionsSubmissionRow(assignmentId = assignmentId, questions = questions, student = student)
        } else {
            StudentSubmissionRow(assignmentId = assignmentId, student = student)
        }
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

/** صف طالب لواجب بنظام الأسئلة - نفس نمط StudentAttemptRow بشاشة تفاصيل
 * الاختبار بالضبط (توسيع لعرض الإجابات + حقل درجة يشتغل سواء اتصحح تلقائيًا
 * أو ينتظر تصحيح يدوي). */
@Composable
private fun StudentQuestionsSubmissionRow(assignmentId: String, questions: List<QuizQuestionDetail>, student: AssignmentStudentStatus) {
    var expanded by remember { mutableStateOf(false) }
    var gradeText by remember(student.userId) { mutableStateOf(student.grade ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                if (student.isAutoGraded && student.score != null) {
                    Text(
                        stringResource(R.string.quiz_auto_graded_score, student.score, student.totalQuestions ?: questions.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(stringResource(if (expanded) R.string.btn_hide_answers else R.string.btn_view_answers))
                }
                if (expanded) {
                    questions.sortedBy { it.orderIndex }.forEach { question ->
                        val answer = student.answers?.get(question.id)
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(question.questionText, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.quiz_student_answer_label, answer?.takeIf { it.isNotBlank() } ?: stringResource(R.string.quiz_no_answer)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (!question.correctAnswer.isNullOrBlank()) {
                                Text(
                                    stringResource(R.string.quiz_correct_answer_label, question.correctAnswer),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
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
private fun StudentSubmissionSection(
    assignmentId: String,
    submissionType: String,
    questions: List<QuizQuestionDetail>,
    submission: com.zakiy.platform.network.dto.AssignmentSubmission?,
    onSubmitted: suspend () -> Unit,
) {
    if (submission != null) {
        Column {
            if (!submission.fileName.isNullOrBlank()) {
                Text(stringResource(R.string.assignment_submitted_file_label, submission.fileName), style = MaterialTheme.typography.bodyMedium)
            }
            if (!submission.note.isNullOrBlank()) {
                Text(stringResource(R.string.assignment_note_shown, submission.note), style = MaterialTheme.typography.bodyMedium)
            }
            if (submissionType == SUBMISSION_TYPE_QUESTIONS && submission.isAutoGraded && submission.score != null) {
                Text(
                    stringResource(R.string.quiz_auto_graded_score, submission.score, submission.totalQuestions ?: questions.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                submission.grade?.let { stringResource(R.string.assignment_grade_shown, it) } ?: stringResource(R.string.assignment_not_graded_yet),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    if (submissionType == SUBMISSION_TYPE_QUESTIONS) {
        QuestionsSubmissionForm(assignmentId = assignmentId, questions = questions, onSubmitted = onSubmitted)
    } else {
        FileSubmissionForm(assignmentId = assignmentId, onSubmitted = onSubmitted)
    }
}

@Composable
private fun QuestionsSubmissionForm(assignmentId: String, questions: List<QuizQuestionDetail>, onSubmitted: suspend () -> Unit) {
    val answers = remember { mutableStateMapOf<String, String>() }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Text(stringResource(R.string.assignment_submit_heading), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.size(8.dp))
    questions.sortedBy { it.orderIndex }.forEach { question ->
        QuestionAnswerCard(
            questionText = question.questionText,
            questionType = question.questionType,
            choices = question.choices,
            currentAnswer = answers[question.id],
            onAnswerChange = { answers[question.id] = it },
        )
    }
    if (errorMessage != null) {
        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
    }
    Spacer(modifier = Modifier.size(12.dp))
    Button(
        onClick = {
            isSubmitting = true
            errorMessage = null
            scope.launch {
                try {
                    NetworkModule.backendApi.submitAssignmentAnswers(assignmentId, SubmitAssignmentAnswersRequest(answers.toMap()))
                    onSubmitted()
                } catch (e: Exception) {
                    errorMessage = e.message
                } finally {
                    isSubmitting = false
                }
            }
        },
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_submit_assignment))
    }
}

@Composable
private fun FileSubmissionForm(assignmentId: String, onSubmitted: suspend () -> Unit) {
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
