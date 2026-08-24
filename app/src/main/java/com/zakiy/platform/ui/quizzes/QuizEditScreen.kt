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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.CreateQuizRequest
import com.zakiy.platform.network.dto.QuizQuestionInput
import com.zakiy.platform.network.dto.SchoolClassSummary
import com.zakiy.platform.network.dto.UpdateQuizRequest
import kotlinx.coroutines.launch

private const val TYPE_MCQ = "mcq"
private const val TYPE_TRUE_FALSE = "true_false"
private const val TYPE_ESSAY = "essay"

/** مسودة سؤال بحالة Compose قابلة للتعديل مباشرة - بديل ViewModel (نفس نمط
 * المشروع اللي ما يستخدم ViewModel إطلاقًا)، `choices` قائمة حالة منفصلة
 * عشان إضافة/حذف اختيار يحدّث الواجهة فورًا. */
private class QuestionDraft(
    type: String = TYPE_MCQ,
    text: String = "",
    choices: List<String> = listOf("", ""),
    correctChoiceIndex: Int? = null,
    correctBool: Boolean? = null,
) {
    var type by mutableStateOf(type)
    var text by mutableStateOf(text)
    val choices = mutableStateListOf(*choices.toTypedArray())
    var correctChoiceIndex by mutableStateOf(correctChoiceIndex)
    var correctBool by mutableStateOf(correctBool)
}

private fun QuestionDraft.toInput(): QuizQuestionInput = when (type) {
    TYPE_MCQ -> QuizQuestionInput(
        questionType = TYPE_MCQ,
        questionText = text,
        choices = choices.filter { it.isNotBlank() },
        correctAnswer = correctChoiceIndex
            ?.let { idx -> choices.getOrNull(idx) }
            ?.takeIf { it.isNotBlank() },
    )
    TYPE_TRUE_FALSE -> QuizQuestionInput(
        questionType = TYPE_TRUE_FALSE,
        questionText = text,
        choices = null,
        correctAnswer = correctBool?.let { if (it) "true" else "false" },
    )
    else -> QuizQuestionInput(questionType = TYPE_ESSAY, questionText = text, choices = null, correctAnswer = null)
}

/** إنشاء/تعديل اختبار - `quizId` null يعني إنشاء (لازم اختيار فصل)، غير null
 * يعني تعديل مسودة موجودة (الفصل ثابت، الباك إند أصلًا يرفض PATCH لاختبار
 * منشور - زر التعديل بشاشة التفاصيل ما يظهر إلا لو مسودة). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizEditScreen(quizId: String?, onBack: () -> Unit, onSaved: () -> Unit) {
    val isEdit = quizId != null
    var classes by remember { mutableStateOf<List<SchoolClassSummary>>(emptyList()) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var classMenuExpanded by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var timeLimitText by remember { mutableStateOf("") }
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
                timeLimitText = detail.timeLimitMinutes.toString()
                questions.clear()
                detail.questions.sortedBy { it.orderIndex }.forEach { q ->
                    val choicesList = q.choices ?: emptyList()
                    questions.add(
                        QuestionDraft(
                            type = q.questionType,
                            text = q.questionText,
                            choices = if (q.questionType == TYPE_MCQ) choicesList.ifEmpty { listOf("", "") } else listOf("", ""),
                            correctChoiceIndex = if (q.questionType == TYPE_MCQ) choicesList.indexOf(q.correctAnswer).takeIf { it >= 0 } else null,
                            correctBool = if (q.questionType == TYPE_TRUE_FALSE) q.correctAnswer?.let { it == "true" } else null,
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

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }

            Button(
                onClick = {
                    val timeLimit = timeLimitText.toIntOrNull()
                    if (!isEdit && selectedClassId == null) {
                        errorMessage = null
                        return@Button
                    }
                    if (questions.isEmpty()) {
                        errorMessage = null
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val inputs = questions.map { it.toInput() }
                            if (isEdit) {
                                NetworkModule.backendApi.updateQuiz(
                                    quizId!!,
                                    UpdateQuizRequest(
                                        subject = subject,
                                        title = title,
                                        timeLimitMinutes = timeLimit,
                                        questions = inputs,
                                    ),
                                )
                            } else {
                                NetworkModule.backendApi.createQuiz(
                                    CreateQuizRequest(
                                        classId = selectedClassId!!,
                                        subject = subject,
                                        title = title,
                                        timeLimitMinutes = timeLimit ?: 0,
                                        questions = inputs,
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
                    timeLimitText.toIntOrNull() != null && questions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text(stringResource(if (isEdit) R.string.btn_save_quiz_changes else R.string.btn_create_quiz))
            }
        }
    }
}

@Composable
private fun QuestionEditor(index: Int, draft: QuestionDraft, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.quiz_question_number, index + 1),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                }
            }

            Text(stringResource(R.string.quiz_question_type_label), style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                QuestionTypeOption(TYPE_MCQ, stringResource(R.string.quiz_type_mcq), draft)
                QuestionTypeOption(TYPE_TRUE_FALSE, stringResource(R.string.quiz_type_true_false), draft)
                QuestionTypeOption(TYPE_ESSAY, stringResource(R.string.quiz_type_essay), draft)
            }

            OutlinedTextField(
                value = draft.text,
                onValueChange = { draft.text = it },
                label = { Text(stringResource(R.string.quiz_question_text_placeholder)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                minLines = 2,
            )

            when (draft.type) {
                TYPE_MCQ -> {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.quiz_mark_correct_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    draft.choices.forEachIndexed { choiceIndex, choiceText ->
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = draft.correctChoiceIndex == choiceIndex,
                                onClick = { draft.correctChoiceIndex = if (draft.correctChoiceIndex == choiceIndex) null else choiceIndex },
                            )
                            OutlinedTextField(
                                value = choiceText,
                                onValueChange = { draft.choices[choiceIndex] = it },
                                label = { Text(stringResource(R.string.quiz_choice_placeholder, choiceIndex + 1)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            if (draft.choices.size > 2) {
                                IconButton(onClick = {
                                    if (draft.correctChoiceIndex == choiceIndex) draft.correctChoiceIndex = null
                                    else if ((draft.correctChoiceIndex ?: -1) > choiceIndex) draft.correctChoiceIndex = draft.correctChoiceIndex!! - 1
                                    draft.choices.removeAt(choiceIndex)
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                    TextButton(onClick = { draft.choices.add("") }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(stringResource(R.string.btn_add_choice))
                    }
                }
                TYPE_TRUE_FALSE -> {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.quiz_mark_correct_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = draft.correctBool == true,
                            onClick = { draft.correctBool = if (draft.correctBool == true) null else true },
                        )
                        Text(stringResource(R.string.quiz_true_label))
                        Spacer(modifier = Modifier.size(16.dp))
                        RadioButton(
                            selected = draft.correctBool == false,
                            onClick = { draft.correctBool = if (draft.correctBool == false) null else false },
                        )
                        Text(stringResource(R.string.quiz_false_label))
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun QuestionTypeOption(type: String, label: String, draft: QuestionDraft) {
    Row(
        modifier = Modifier
            .padding(end = 12.dp)
            .selectable(selected = draft.type == type, onClick = { draft.type = type }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = draft.type == type, onClick = null)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
