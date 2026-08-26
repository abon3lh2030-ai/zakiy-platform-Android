package com.zakiy.platform.ui.madrasati

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.GenerateStudyPlanRequest
import com.zakiy.platform.network.dto.SaveStudyPlanRequest
import com.zakiy.platform.network.dto.StudyPlanContent
import com.zakiy.platform.network.dto.parseAiContent
import com.zakiy.platform.network.dto.toApiErrorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** خطة مذاكرة ذكية (طالب) - إنشاء/عرض. ما فيه PATCH لهذا المسار بالباك إند -
 * نفس منطق مساعد الواجب بالضبط: الحفظ دايمًا POST جديد. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanScreen(planId: String?, onBack: () -> Unit) {
    val lang = remember { java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" } }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var currentId by remember { mutableStateOf(planId) }
    var subjects by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf<String?>(null) }
    var hoursText by remember { mutableStateOf("") }
    var content by remember { mutableStateOf<StudyPlanContent?>(null) }

    var isLoading by remember { mutableStateOf(planId != null) }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var generateError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var justSaved by remember { mutableStateOf(false) }
    var justCopied by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val genFailedLabel = stringResource(R.string.err_lesson_prep_gen_failed)
    val subjectsRequiredLabel = stringResource(R.string.err_study_plan_subjects_required)

    LaunchedEffect(Unit) {
        if (planId != null) {
            val detail = runCatching { NetworkModule.backendApi.studyPlanDetail(planId) }.getOrNull()
            if (detail != null) {
                subjects = detail.subjects
                examDate = detail.examDate
                hoursText = detail.hoursPerDay?.toString().orEmpty()
                content = detail.content
            }
            isLoading = false
        }
    }

    LaunchedEffect(justSaved) { if (justSaved) { delay(1500); justSaved = false } }
    LaunchedEffect(justCopied) { if (justCopied) { delay(1500); justCopied = false } }

    fun generate() {
        generateError = null
        if (subjects.isBlank()) {
            generateError = subjectsRequiredLabel
            return
        }
        isGenerating = true
        scope.launch {
            try {
                val resp = NetworkModule.backendApi.generateStudyPlan(
                    GenerateStudyPlanRequest(subjects, examDate, hoursText.toDoubleOrNull(), lang),
                )
                content = parseAiContent<StudyPlanContent>(resp.contentRaw)
                currentId = null
            } catch (e: Exception) {
                generateError = e.toApiErrorMessage(genFailedLabel)
            } finally {
                isGenerating = false
            }
        }
    }

    fun save() {
        val c = content ?: return
        saveError = null
        if (subjects.isBlank()) {
            saveError = subjectsRequiredLabel
            return
        }
        isSaving = true
        scope.launch {
            try {
                // ما فيه PATCH لخطة المذاكرة بالباك إند - الحفظ دايمًا POST جديد
                val saved = NetworkModule.backendApi.createStudyPlan(
                    SaveStudyPlanRequest(subjects, examDate, hoursText.toDoubleOrNull(), c),
                )
                currentId = saved.id
                justSaved = true
            } catch (e: Exception) {
                saveError = e.toApiErrorMessage(genFailedLabel)
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (planId != null) R.string.study_plan_view_heading else R.string.study_plan_create_heading)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = subjects, onValueChange = { subjects = it },
                label = { Text(stringResource(R.string.ph_study_plan_subjects)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(8.dp))

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(examDate ?: stringResource(R.string.study_plan_exam_date_placeholder))
            }
            Spacer(modifier = Modifier.size(8.dp))

            OutlinedTextField(
                value = hoursText,
                onValueChange = { new -> if (new.all { it.isDigit() || it == '.' }) hoursText = new },
                label = { Text(stringResource(R.string.ph_study_plan_hours)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(12.dp))

            if (generateError != null) {
                Text(generateError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            Button(onClick = { generate() }, enabled = !isGenerating, modifier = Modifier.fillMaxWidth()) {
                if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.btn_generate_study_plan))
                }
            }

            val c = content
            if (c != null) {
                Spacer(modifier = Modifier.size(20.dp))
                Card {
                    Column(modifier = Modifier.padding(14.dp)) {
                        c.days.forEach { day ->
                            SectionLabel(day.dateLabel)
                            BulletList(day.tasks)
                        }
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                Card {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SectionLabel(stringResource(R.string.sp_tips_label))
                        Text(c.generalTips, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.size(14.dp))
                ActionButtons(
                    isCopied = justCopied,
                    isSaved = justSaved,
                    isSaving = isSaving,
                    showDelete = currentId != null,
                    saveLabelRes = R.string.btn_save_study_plan,
                    onCopy = {
                        clipboard.setText(AnnotatedString(buildStudyPlanCopyText(c)))
                        justCopied = true
                    },
                    onRegenerate = { generate() },
                    onSave = { save() },
                    onDelete = { showDeleteConfirm = true },
                )

                if (saveError != null) {
                    Text(saveError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = examDate?.let {
                runCatching { java.time.LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        examDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { examDate = null; showDatePicker = false }) { Text(stringResource(R.string.study_plan_clear_date)) }
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        ) { DatePicker(state = state) }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.btn_delete_lesson_prep)) },
            text = { Text(stringResource(R.string.confirm_delete_study_plan)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val id = currentId ?: return@TextButton
                    isDeleting = true
                    scope.launch {
                        runCatching { NetworkModule.backendApi.deleteStudyPlan(id) }
                        isDeleting = false
                        onBack()
                    }
                }, enabled = !isDeleting) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun buildStudyPlanCopyText(c: StudyPlanContent): String = buildString {
    c.days.forEach { day ->
        appendLine("${day.dateLabel}:")
        day.tasks.forEach { appendLine("- $it") }
        appendLine()
    }
    append(c.generalTips)
}
