package com.zakiy.platform.ui.quizzes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateListOf
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
import com.zakiy.platform.network.dto.CreateQuizRequest
import com.zakiy.platform.network.dto.SchoolClassSummary
import com.zakiy.platform.network.dto.UpdateQuizRequest
import com.zakiy.platform.ui.common.PLATFORM_MADRASATI
import com.zakiy.platform.ui.common.PLATFORM_ZAKIY
import com.zakiy.platform.ui.common.PlatformPicker
import com.zakiy.platform.ui.common.QUESTION_TYPE_MCQ
import com.zakiy.platform.ui.common.QUESTION_TYPE_TRUE_FALSE
import com.zakiy.platform.ui.common.QuestionDraft
import com.zakiy.platform.ui.common.QuestionEditor
import com.zakiy.platform.ui.common.openMadrasatiLink
import com.zakiy.platform.ui.common.toInput
import kotlinx.coroutines.launch

/** إنشاء/تعديل اختبار - `quizId` null يعني إنشاء (لازم اختيار فصل)، غير null
 * يعني تعديل مسودة موجودة (الفصل ثابت، الباك إند أصلًا يرفض PATCH لاختبار
 * منشور - زر التعديل بشاشة التفاصيل ما يظهر إلا لو مسودة). لما platform=
 * "madrasati" يختفي حقل المدة والأسئلة تمامًا (يُحل بالكامل على مدرستي). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizEditScreen(quizId: String?, onBack: () -> Unit, onSaved: () -> Unit) {
    val isEdit = quizId != null
    val context = LocalContext.current
    var classes by remember { mutableStateOf<List<SchoolClassSummary>>(emptyList()) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var classMenuExpanded by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var timeLimitText by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf(PLATFORM_ZAKIY) }
    var externalLink by remember { mutableStateOf("") }
    val questions = remember { mutableStateListOf<QuestionDraft>() }
    var isLoading by remember { mutableStateOf(isEdit) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (isEdit) {
            val detail = runCatching { NetworkModule.backendApi.teacherQuizDetail(quizId!!) }.getOrNull()
            if (detail != null) {
                subject = detail.subject
                title = detail.title
                timeLimitText = detail.timeLimitMinutes?.toString() ?: ""
                platform = detail.platform
                externalLink = detail.externalLink ?: ""
                questions.clear()
                detail.questions.sortedBy { it.orderIndex }.forEach { q ->
                    val choicesList = q.choices ?: emptyList()
                    questions.add(
                        QuestionDraft(
                            type = q.questionType,
                            text = q.questionText,
                            choices = if (q.questionType == QUESTION_TYPE_MCQ) choicesList.ifEmpty { listOf("", "") } else listOf("", ""),
                            correctChoiceIndex = if (q.questionType == QUESTION_TYPE_MCQ) choicesList.indexOf(q.correctAnswer).takeIf { it >= 0 } else null,
                            correctBool = if (q.questionType == QUESTION_TYPE_TRUE_FALSE) q.correctAnswer?.let { it == "true" } else null,
                        )
                    )
                }
            }
            isLoading = false
        } else {
            val roster = runCatching { NetworkModule.backendApi.teacherRoster() }.getOrNull()
            classes = roster?.classes ?: emptyList()
            selectedClassId = classes.firstOrNull()?.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isEdit) R.string.quiz_edit_heading else R.string.quiz_new_heading)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            if (!isEdit) {
                Box {
                    TextButton(onClick = { classMenuExpanded = true }) {
                        Text(classes.firstOrNull { it.id == selectedClassId }?.name ?: stringResource(R.string.quiz_class_label))
                    }
                    DropdownMenu(expanded = classMenuExpanded, onDismissRequest = { classMenuExpanded = false }) {
                        classes.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = {
                                selectedClassId = c.id
                                classMenuExpanded = false
                            })
                        }
                    }
                }
            }
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text(stringResource(R.string.quiz_subject_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.quiz_title_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Spacer(modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.platform_label), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.size(4.dp))
            PlatformPicker(platform = platform, onPlatformChange = { platform = it })

            if (platform == PLATFORM_MADRASATI) {
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedTextField(
                    value = externalLink,
                    onValueChange = { externalLink = it },
                    label = { Text(stringResource(R.string.madrasati_link_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.size(8.dp))
                TextButton(onClick = { openMadrasatiLink(context, null) }) {
                    Text(stringResource(R.string.btn_open_madrasati))
                }
            } else {
                OutlinedTextField(
                    value = timeLimitText,
                    onValueChange = { new -> if (new.all { it.isDigit() }) timeLimitText = new },
                    label = { Text(stringResource(R.string.quiz_time_limit_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                Spacer(modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.quiz_questions_heading), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(8.dp))

                questions.forEachIndexed { index, draft ->
                    QuestionEditor(index = index, draft = draft, onRemove = { questions.removeAt(index) })
                    Spacer(modifier = Modifier.size(10.dp))
                }

                OutlinedButton(
                    onClick = { questions.add(QuestionDraft()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.btn_add_question))
                }
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }

            val isMadrasati = platform == PLATFORM_MADRASATI
            Button(
                onClick = {
                    val timeLimit = timeLimitText.toIntOrNull()
                    if (!isEdit && selectedClassId == null) {
                        errorMessage = null
                        return@Button
                    }
                    if (!isMadrasati && questions.isEmpty()) {
                        errorMessage = null
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val inputs = if (isMadrasati) null else questions.map { it.toInput() }
                            val link = externalLink.trim().ifBlank { null }
                            if (isEdit) {
                                NetworkModule.backendApi.updateQuiz(
                                    quizId!!,
                                    UpdateQuizRequest(
                                        subject = subject,
                                        title = title,
                                        timeLimitMinutes = if (isMadrasati) null else timeLimit,
                                        questions = inputs,
                                        externalLink = link,
                                    ),
                                )
                            } else {
                                NetworkModule.backendApi.createQuiz(
                                    CreateQuizRequest(
                                        classId = selectedClassId!!,
                                        subject = subject,
                                        title = title,
                                        timeLimitMinutes = if (isMadrasati) null else (timeLimit ?: 0),
                                        questions = inputs,
                                        platform = platform,
                                        externalLink = link,
                                    ),
                                )
                            }
                            onSaved()
                        } catch (e: Exception) {
                            errorMessage = e.message
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && (isEdit || selectedClassId != null) && subject.isNotBlank() && title.isNotBlank() &&
                    (isMadrasati || (timeLimitText.toIntOrNull() != null && questions.isNotEmpty())),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text(stringResource(if (isEdit) R.string.btn_save_quiz_changes else R.string.btn_create_quiz))
            }
        }
    }
}
