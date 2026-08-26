package com.zakiy.platform.ui.madrasati

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.zakiy.platform.network.dto.GenerateHomeworkHelpRequest
import com.zakiy.platform.network.dto.HomeworkHelpContent
import com.zakiy.platform.network.dto.SaveHomeworkHelpRequest
import com.zakiy.platform.network.dto.parseAiContent
import com.zakiy.platform.network.dto.toApiErrorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** مساعد الواجب الذكي (طالب) - إنشاء/عرض. ما فيه PATCH لهذا المسار بالباك
 * إند - زر الحفظ دايمًا POST (يُنشئ جلسة جديدة)، حتى لو نعرض جلسة محفوظة
 * قبل؛ التوليد من جديد يصفّر `currentId` لأن المحتوى الجديد ما انحفظ بعد. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkHelpScreen(sessionId: String?, onBack: () -> Unit) {
    val lang = remember { java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" } }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var currentId by remember { mutableStateOf(sessionId) }
    var subject by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var content by remember { mutableStateOf<HomeworkHelpContent?>(null) }

    var isLoading by remember { mutableStateOf(sessionId != null) }
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
        if (sessionId != null) {
            val detail = runCatching { NetworkModule.backendApi.homeworkHelpDetail(sessionId) }.getOrNull()
            if (detail != null) {
                subject = detail.subject
                gradeLevel = detail.gradeLevel
                topic = detail.topic
                content = detail.content
            }
            isLoading = false
        }
    }

    LaunchedEffect(justSaved) { if (justSaved) { delay(1500); justSaved = false } }
    LaunchedEffect(justCopied) { if (justCopied) { delay(1500); justCopied = false } }

    fun generate() {
        generateError = null
        if (subject.isBlank() || gradeLevel.isBlank() || topic.isBlank()) {
            generateError = fieldsRequiredLabel
            return
        }
        isGenerating = true
        scope.launch {
            try {
                val resp = NetworkModule.backendApi.generateHomeworkHelp(
                    GenerateHomeworkHelpRequest(subject, gradeLevel, topic, lang),
                )
                content = parseAiContent<HomeworkHelpContent>(resp.contentRaw)
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
        if (subject.isBlank() || gradeLevel.isBlank() || topic.isBlank()) {
            saveError = fieldsRequiredLabel
            return
        }
        isSaving = true
        scope.launch {
            try {
                // ما فيه PATCH لمساعد الواجب بالباك إند - الحفظ دايمًا POST جديد
                val saved = NetworkModule.backendApi.createHomeworkHelp(
                    SaveHomeworkHelpRequest(subject, gradeLevel, topic, c),
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
                title = { Text(stringResource(if (sessionId != null) R.string.homework_help_view_heading else R.string.homework_help_create_heading)) },
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
                value = topic, onValueChange = { topic = it },
                label = { Text(stringResource(R.string.ph_homework_topic)) },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
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
                    Text(stringResource(R.string.btn_generate_homework_help))
                }
            }

            val c = content
            if (c != null) {
                Spacer(modifier = Modifier.size(20.dp))
                Card {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SectionLabel(stringResource(R.string.hh_explanation_label))
                        Text(c.explanation, style = MaterialTheme.typography.bodyMedium)
                        SectionLabel(stringResource(R.string.hh_example_label))
                        Text(c.workedExample, style = MaterialTheme.typography.bodyMedium)
                        SectionLabel(stringResource(R.string.hh_practice_label))
                        c.practiceQuestions.forEachIndexed { index, q ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${index + 1}. ${q.question}", style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(q.answer, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        SectionLabel(stringResource(R.string.hh_tips_label))
                        Text(c.tips, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.size(14.dp))
                ActionButtons(
                    isCopied = justCopied,
                    isSaved = justSaved,
                    isSaving = isSaving,
                    showDelete = currentId != null,
                    saveLabelRes = R.string.btn_save_homework_help,
                    onCopy = {
                        clipboard.setText(AnnotatedString(buildHomeworkHelpCopyText(c)))
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
            text = { Text(stringResource(R.string.confirm_delete_homework_help)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val id = currentId ?: return@TextButton
                    isDeleting = true
                    scope.launch {
                        runCatching { NetworkModule.backendApi.deleteHomeworkHelp(id) }
                        isDeleting = false
                        onBack()
                    }
                }, enabled = !isDeleting) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun buildHomeworkHelpCopyText(c: HomeworkHelpContent): String = buildString {
    appendLine(c.explanation)
    appendLine()
    appendLine(c.workedExample)
    appendLine()
    c.practiceQuestions.forEachIndexed { index, q ->
        appendLine("${index + 1}. ${q.question}")
        appendLine("   ${q.answer}")
    }
    appendLine()
    append(c.tips)
}
