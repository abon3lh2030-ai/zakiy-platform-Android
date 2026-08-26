package com.zakiy.platform.ui.assignments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zakiy.platform.network.dto.CreateAssignmentRequest
import com.zakiy.platform.network.dto.SchoolClassSummary
import com.zakiy.platform.ui.common.PLATFORM_MADRASATI
import com.zakiy.platform.ui.common.PLATFORM_ZAKIY
import com.zakiy.platform.ui.common.PlatformPicker
import com.zakiy.platform.ui.common.QuestionDraft
import com.zakiy.platform.ui.common.QuestionEditor
import com.zakiy.platform.ui.common.openMadrasatiLink
import com.zakiy.platform.ui.common.toInput
import kotlinx.coroutines.launch

private const val SUBMISSION_TYPE_FILE = "file"
private const val SUBMISSION_TYPE_QUESTIONS = "questions"

/** إنشاء واجب جديد - دايمًا لكل طلاب الفصل (الباك إند ما عاد يقبل استهداف
 * طالب واحد بعينه - target_student_id أُزيل تمامًا). المنصة (ذكيّ/مدرستي)
 * وطريقة التسليم (ملف/أسئلة) تحدّدان أي جزء من النموذج يظهر - نفس نظام
 * الأسئلة اللي الاختبارات تستخدمه بالضبط (QuestionDraft/QuestionEditor
 * مشتركة). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentCreateSheet(onDismiss: () -> Unit, onCreated: () -> Unit) {
    val context = LocalContext.current
    var classes by remember { mutableStateOf<List<SchoolClassSummary>>(emptyList()) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var subject by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var classMenuExpanded by remember { mutableStateOf(false) }
    var platform by remember { mutableStateOf(PLATFORM_ZAKIY) }
    var externalLink by remember { mutableStateOf("") }
    var submissionType by remember { mutableStateOf(SUBMISSION_TYPE_FILE) }
    val questions = remember { mutableStateListOf<QuestionDraft>() }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val roster = runCatching { NetworkModule.backendApi.teacherRoster() }.getOrNull()
        classes = roster?.classes ?: emptyList()
        selectedClassId = classes.firstOrNull()?.id
    }

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
                            classMenuExpanded = false
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
                Spacer(modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.assignment_submission_type_label), style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    SubmissionTypeOption(SUBMISSION_TYPE_FILE, stringResource(R.string.assignment_submission_type_file), submissionType) { submissionType = it }
                    SubmissionTypeOption(SUBMISSION_TYPE_QUESTIONS, stringResource(R.string.assignment_submission_type_questions), submissionType) { submissionType = it }
                }

                if (submissionType == SUBMISSION_TYPE_QUESTIONS) {
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.quiz_questions_heading), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.size(8.dp))
                    questions.forEachIndexed { index, draft ->
                        QuestionEditor(index = index, draft = draft, onRemove = { questions.removeAt(index) })
                        Spacer(modifier = Modifier.size(10.dp))
                    }
                    OutlinedButton(onClick = { questions.add(QuestionDraft()) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.btn_add_question))
                    }
                }
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            val isMadrasati = platform == PLATFORM_MADRASATI
            val needsQuestions = !isMadrasati && submissionType == SUBMISSION_TYPE_QUESTIONS
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
                                    subject = subject,
                                    title = title,
                                    content = content,
                                    submissionType = if (isMadrasati) SUBMISSION_TYPE_FILE else submissionType,
                                    questions = if (needsQuestions) questions.map { it.toInput() } else null,
                                    platform = platform,
                                    externalLink = externalLink.trim().ifBlank { null },
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
                enabled = !isSaving && selectedClassId != null && subject.isNotBlank() && title.isNotBlank() &&
                    (!needsQuestions || questions.isNotEmpty()),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_create_assignment))
            }
        }
    }
}

@Composable
private fun SubmissionTypeOption(value: String, label: String, current: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(end = 16.dp)
            .selectable(selected = current == value, onClick = { onChange(value) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = current == value, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
