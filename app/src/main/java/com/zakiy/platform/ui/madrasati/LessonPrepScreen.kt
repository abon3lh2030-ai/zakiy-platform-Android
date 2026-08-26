package com.zakiy.platform.ui.madrasati

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.GenerateLessonPrepRequest
import com.zakiy.platform.network.dto.LessonPrepContent
import com.zakiy.platform.network.dto.SaveLessonPrepRequest
import com.zakiy.platform.network.dto.UpdateLessonPrepRequest
import com.zakiy.platform.network.dto.parseAiContent
import com.zakiy.platform.network.dto.toApiErrorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** تحضير درس بالذكاء الاصطناعي - إنشاء/عرض/تعديل. نفس شاشة الموقع بالضبط:
 * `prepId` null يعني تحضير جديد، غير null يحمّل تحضير محفوظ (نموذجه قابل
 * للتعديل قبل الحفظ من جديد بـ PATCH). التوليد من جديد (حتى وأنت تعرض تحضير
 * محفوظ) يُعتبر مسودة جديدة - حفظها لاحقًا POST جديد لا PATCH. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPrepScreen(prepId: String?, onBack: () -> Unit) {
    val lang = remember { java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" } }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var currentId by remember { mutableStateOf(prepId) }
    var subject by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var lessonTitle by remember { mutableStateOf("") }
    var content by remember { mutableStateOf<LessonPrepContent?>(null) }

    var isLoading by remember { mutableStateOf(prepId != null) }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var generateError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var justSaved by remember { mutableStateOf(false) }
    var justCopied by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val genFailedLabel = stringResource(R.string.err_lesson_prep_gen_failed)
    val fieldsRequiredLabel = stringResource(R.string.err_lesson_prep_fields_required)

    LaunchedEffect(Unit) {
        if (prepId != null) {
            val detail = runCatching { NetworkModule.backendApi.lessonPrepDetail(prepId) }.getOrNull()
            if (detail != null) {
                subject = detail.subject
                gradeLevel = detail.gradeLevel
                unit = detail.unit.orEmpty()
                lessonTitle = detail.lessonTitle
                content = detail.content
            }
            isLoading = false
        }
    }

    LaunchedEffect(justSaved) { if (justSaved) { delay(1500); justSaved = false } }
    LaunchedEffect(justCopied) { if (justCopied) { delay(1500); justCopied = false } }

    fun generate() {
        generateError = null
        if (subject.isBlank() || gradeLevel.isBlank() || lessonTitle.isBlank()) {
            generateError = fieldsRequiredLabel
            return
        }
        isGenerating = true
        scope.launch {
            try {
                val resp = NetworkModule.backendApi.generateLessonPrep(
                    GenerateLessonPrepRequest(subject, gradeLevel, unit.ifBlank { null }, lessonTitle, lang),
                )
                content = parseAiContent<LessonPrepContent>(resp.contentRaw)
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
        if (subject.isBlank() || gradeLevel.isBlank() || lessonTitle.isBlank()) {
            saveError = fieldsRequiredLabel
            return
        }
        isSaving = true
        scope.launch {
            try {
                val id = currentId
                if (id != null) {
                    NetworkModule.backendApi.updateLessonPrep(
                        id,
                        UpdateLessonPrepRequest(subject, gradeLevel, unit.ifBlank { null }, lessonTitle, c),
                    )
                } else {
                    val saved = NetworkModule.backendApi.createLessonPrep(
                        SaveLessonPrepRequest(subject, gradeLevel, unit.ifBlank { null }, lessonTitle, c),
                    )
                    currentId = saved.id
                }
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
                title = { Text(stringResource(if (prepId != null) R.string.lesson_prep_view_heading else R.string.lesson_prep_create_heading)) },
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
                value = subject, onValueChange = { subject = it },
                label = { Text(stringResource(R.string.ph_lesson_prep_subject)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = gradeLevel, onValueChange = { gradeLevel = it },
                label = { Text(stringResource(R.string.ph_lesson_prep_grade)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = unit, onValueChange = { unit = it },
                label = { Text(stringResource(R.string.ph_lesson_prep_unit)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = lessonTitle, onValueChange = { lessonTitle = it },
                label = { Text(stringResource(R.string.ph_lesson_prep_title)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
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
                    Text(stringResource(R.string.btn_generate_lesson_prep))
                }
            }

            val c = content
            if (c != null) {
                Spacer(modifier = Modifier.size(20.dp))
                Card {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SectionLabel(stringResource(R.string.lp_objectives_label))
                        BulletList(c.objectives)
                        SectionLabel(stringResource(R.string.lp_intro_label))
                        Text(c.intro, style = MaterialTheme.typography.bodyMedium)
                        SectionLabel(stringResource(R.string.lp_steps_label))
                        BulletList(c.steps)
                        SectionLabel(stringResource(R.string.lp_activities_label))
                        BulletList(c.activities)
                        SectionLabel(stringResource(R.string.lp_assessment_label))
                        Text(c.assessment, style = MaterialTheme.typography.bodyMedium)
                        SectionLabel(stringResource(R.string.lp_homework_label))
                        Text(c.homework, style = MaterialTheme.typography.bodyMedium)
                        SectionLabel(stringResource(R.string.lp_enrichment_label))
                        Text(c.enrichment, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.size(14.dp))
                ActionButtons(
                    isCopied = justCopied,
                    isSaved = justSaved,
                    isSaving = isSaving,
                    showDelete = currentId != null,
                    saveLabelRes = R.string.btn_save_lesson_prep,
                    onCopy = {
                        clipboard.setText(AnnotatedString(buildLessonPrepCopyText(c)))
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.btn_delete_lesson_prep)) },
            text = { Text(stringResource(R.string.confirm_delete_lesson_prep)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val id = currentId ?: return@TextButton
                    isDeleting = true
                    scope.launch {
                        runCatching { NetworkModule.backendApi.deleteLessonPrep(id) }
                        isDeleting = false
                        onBack()
                    }
                }, enabled = !isDeleting) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun buildLessonPrepCopyText(c: LessonPrepContent): String = buildString {
    appendLine(c.objectives.joinToString("\n") { "- $it" })
    appendLine()
    appendLine(c.intro)
    appendLine()
    appendLine(c.steps.joinToString("\n") { "- $it" })
    appendLine()
    appendLine(c.activities.joinToString("\n") { "- $it" })
    appendLine()
    appendLine(c.assessment)
    appendLine()
    appendLine(c.homework)
    appendLine()
    append(c.enrichment)
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
internal fun BulletList(items: List<String>) {
    Column {
        items.forEach { item ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text("• ", style = MaterialTheme.typography.bodyMedium)
                Text(item, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** أزرار نسخ/توليد من جديد/حفظ/حذف موحّدة لأدوات مدرستي القابلة للحفظ الثلاث
 * (تحضير الدرس/مساعد الواجب/خطة المذاكرة) - نفس الموقع بالضبط يستخدم مفاتيح
 * i18n موحّدة لنسخ/توليد من جديد/حذف بكل الأدوات، وتختلف بس تسمية زر الحفظ
 * (يُمرَّر عبر `saveLabelRes`). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActionButtons(
    isCopied: Boolean,
    isSaved: Boolean,
    isSaving: Boolean,
    showDelete: Boolean,
    saveLabelRes: Int,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onCopy) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text(stringResource(if (isCopied) R.string.copied_label else R.string.btn_copy_lesson_prep))
        }
        OutlinedButton(onClick = onRegenerate) {
            Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text(stringResource(R.string.btn_regenerate_lesson_prep))
        }
        Button(onClick = onSave, enabled = !isSaving) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp))
            else {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text(stringResource(if (isSaved) R.string.saved_label else saveLabelRes))
            }
        }
        if (showDelete) {
            OutlinedButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text(stringResource(R.string.btn_delete_lesson_prep))
            }
        }
    }
}
