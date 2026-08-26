package com.zakiy.platform.ui.quizzes

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
import androidx.compose.material3.AlertDialog
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
import com.zakiy.platform.network.dto.GradeAttemptRequest
import com.zakiy.platform.network.dto.QuizQuestionDetail
import com.zakiy.platform.network.dto.QuizStudentStatus
import com.zakiy.platform.network.dto.TeacherQuizDetail
import com.zakiy.platform.network.dto.UpdateQuizRequest
import com.zakiy.platform.ui.common.PLATFORM_MADRASATI
import com.zakiy.platform.ui.common.openMadrasatiLink
import kotlinx.coroutines.launch

/** تفاصيل اختبار من جهة المعلم - مسودة: أزرار تعديل/نشر/حذف. منشور: قائمة
 * حالة كل طالب + إجاباته + حقل درجة (يشتغل سواء اتصحح تلقائيًا أو لا، المعلم
 * دايمًا يقدر يعدّل الدرجة يدويًا). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(quizId: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    var quiz by remember { mutableStateOf<TeacherQuizDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPublishConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        isLoading = true
        errorMessage = null
        quiz = runCatching { NetworkModule.backendApi.teacherQuizDetail(quizId) }.getOrElse {
            errorMessage = it.message
            null
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz?.title.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        val current = quiz
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            current == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(errorMessage ?: stringResource(R.string.error_generic))
            }
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(current.subject, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(current.title, style = MaterialTheme.typography.headlineSmall)
                if (current.platform != PLATFORM_MADRASATI && current.timeLimitMinutes != null) {
                    Text(
                        stringResource(R.string.quiz_time_limit_value, current.timeLimitMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }

                if (current.platform == PLATFORM_MADRASATI) {
                    MadrasatiQuizLinkEditor(quizId = quizId, externalLink = current.externalLink, onSaved = { scope.launch { load() } })
                    Spacer(modifier = Modifier.size(16.dp))
                }

                if (!current.isPublished) {
                    Text(stringResource(R.string.quiz_unpublished_notice), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.size(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onEdit(quizId) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.btn_edit_quiz))
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.delete)) }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = { showPublishConfirm = true },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_publish_quiz))
                    }
                } else if (current.platform != PLATFORM_MADRASATI) {
                    Text(stringResource(R.string.quiz_students_heading), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.size(8.dp))
                    if (current.students.isEmpty()) {
                        Text(stringResource(R.string.quiz_no_students), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        current.students.forEach { student ->
                            StudentAttemptRow(quizId = quizId, questions = current.questions, student = student, onGraded = { scope.launch { load() } })
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                } else {
                    Text(stringResource(R.string.madrasati_solved_there_notice), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.quiz_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    isBusy = true
                    scope.launch {
                        runCatching { NetworkModule.backendApi.deleteQuiz(quizId) }
                        isBusy = false
                        onBack()
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showPublishConfirm) {
        AlertDialog(
            onDismissRequest = { showPublishConfirm = false },
            title = { Text(stringResource(R.string.btn_publish_quiz)) },
            text = { Text(stringResource(R.string.quiz_publish_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showPublishConfirm = false
                    isBusy = true
                    scope.launch {
                        errorMessage = runCatching { NetworkModule.backendApi.publishQuiz(quizId) }.exceptionOrNull()?.message
                        isBusy = false
                        load()
                    }
                }) { Text(stringResource(R.string.btn_publish_quiz)) }
            },
            dismissButton = { TextButton(onClick = { showPublishConfirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun StudentAttemptRow(
    quizId: String,
    questions: List<QuizQuestionDetail>,
    student: QuizStudentStatus,
    onGraded: () -> Unit,
) {
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
                        label = { Text(stringResource(R.string.quiz_grade_label)) },
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                runCatching { NetworkModule.backendApi.gradeQuizAttempt(quizId, student.userId, GradeAttemptRequest(gradeText)) }
                                isSaving = false
                                onGraded()
                            }
                        },
                        enabled = !isSaving,
                    ) { Text(stringResource(R.string.btn_save_grade)) }
                }
            }
        }
    }
}

/** رابط مدرستي - قابل للإضافة/التعديل بأي وقت حتى بعد النشر (الباك إند
 * يستثني external_link من قيد "ما تقدر تعدّل اختبار منشور" خصيصًا). */
@Composable
private fun MadrasatiQuizLinkEditor(quizId: String, externalLink: String?, onSaved: () -> Unit) {
    var linkText by remember(externalLink) { mutableStateOf(externalLink ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column {
        OutlinedTextField(
            value = linkText,
            onValueChange = { linkText = it },
            label = { Text(stringResource(R.string.madrasati_link_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { com.zakiy.platform.ui.common.openMadrasatiLink(context, linkText) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.btn_open_madrasati))
            }
            Spacer(modifier = Modifier.size(8.dp))
            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        runCatching { NetworkModule.backendApi.updateQuiz(quizId, UpdateQuizRequest(externalLink = linkText.trim().ifBlank { null })) }
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
}
