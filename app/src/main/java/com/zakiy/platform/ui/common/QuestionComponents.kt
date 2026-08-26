package com.zakiy.platform.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.dto.QuizQuestionInput

/** أنواع الأسئلة الثلاثة - مشتركة بين الاختبارات والواجبات (submission_type
 * "questions") لأنهما يستخدمان نفس نظام الأسئلة بالضبط بالباك إند. */
const val QUESTION_TYPE_MCQ = "mcq"
const val QUESTION_TYPE_TRUE_FALSE = "true_false"
const val QUESTION_TYPE_ESSAY = "essay"

/** مسودة سؤال بحالة Compose قابلة للتعديل مباشرة - بديل ViewModel (نفس نمط
 * المشروع اللي ما يستخدم ViewModel إطلاقًا)، `choices` قائمة حالة منفصلة
 * عشان إضافة/حذف اختيار يحدّث الواجهة فورًا. مشتركة بين محرر الاختبارات
 * ومحرر أسئلة الواجب. */
class QuestionDraft(
    type: String = QUESTION_TYPE_MCQ,
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

fun QuestionDraft.toInput(): QuizQuestionInput = when (type) {
    QUESTION_TYPE_MCQ -> QuizQuestionInput(
        questionType = QUESTION_TYPE_MCQ,
        questionText = text,
        choices = choices.filter { it.isNotBlank() },
        correctAnswer = correctChoiceIndex
            ?.let { idx -> choices.getOrNull(idx) }
            ?.takeIf { it.isNotBlank() },
    )
    QUESTION_TYPE_TRUE_FALSE -> QuizQuestionInput(
        questionType = QUESTION_TYPE_TRUE_FALSE,
        questionText = text,
        choices = null,
        correctAnswer = correctBool?.let { if (it) "true" else "false" },
    )
    else -> QuizQuestionInput(questionType = QUESTION_TYPE_ESSAY, questionText = text, choices = null, correctAnswer = null)
}

/** محرر سؤال واحد (إنشاء/تعديل مسودة) - يُستخدم بمحرر الاختبارات ومحرر أسئلة
 * الواجب بدون أي تكرار. */
@Composable
fun QuestionEditor(index: Int, draft: QuestionDraft, onRemove: () -> Unit) {
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
                QuestionTypeOption(QUESTION_TYPE_MCQ, stringResource(R.string.quiz_type_mcq), draft)
                QuestionTypeOption(QUESTION_TYPE_TRUE_FALSE, stringResource(R.string.quiz_type_true_false), draft)
                QuestionTypeOption(QUESTION_TYPE_ESSAY, stringResource(R.string.quiz_type_essay), draft)
            }

            OutlinedTextField(
                value = draft.text,
                onValueChange = { draft.text = it },
                label = { Text(stringResource(R.string.quiz_question_text_placeholder)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                minLines = 2,
            )

            when (draft.type) {
                QUESTION_TYPE_MCQ -> {
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
                QUESTION_TYPE_TRUE_FALSE -> {
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

/** بطاقة سؤال واحد بوضع الإجابة (طالب يحل) - نفس شكل عرض سؤال الاختبار
 * بالضبط، مشتركة بين حل الاختبار وحل واجب بنظام الأسئلة. */
@Composable
fun QuestionAnswerCard(
    questionText: String,
    questionType: String,
    choices: List<String>?,
    currentAnswer: String?,
    onAnswerChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(questionText, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.size(8.dp))
            when (questionType) {
                QUESTION_TYPE_MCQ -> choices.orEmpty().forEach { choice ->
                    val selected = currentAnswer == choice
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = { onAnswerChange(choice) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(choice)
                    }
                }
                QUESTION_TYPE_TRUE_FALSE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(selected = currentAnswer == "true", onClick = { onAnswerChange("true") }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = currentAnswer == "true", onClick = null)
                        Text(stringResource(R.string.quiz_true_label))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(selected = currentAnswer == "false", onClick = { onAnswerChange("false") }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = currentAnswer == "false", onClick = null)
                        Text(stringResource(R.string.quiz_false_label))
                    }
                }
                else -> OutlinedTextField(
                    value = currentAnswer ?: "",
                    onValueChange = onAnswerChange,
                    label = { Text(stringResource(R.string.quiz_essay_answer_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        }
    }
}
