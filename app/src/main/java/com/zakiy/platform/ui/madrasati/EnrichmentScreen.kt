package com.zakiy.platform.ui.madrasati

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
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.EnrichmentContent
import com.zakiy.platform.network.dto.GenerateEnrichmentRequest
import com.zakiy.platform.network.dto.parseAiContent
import com.zakiy.platform.network.dto.toApiErrorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** مولّد نشاط إثرائي مستقل (معلم) - توليد لحظي بدون حفظ، نفس شاشة الموقع. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrichmentScreen(onBack: () -> Unit) {
    val lang = remember { java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" } }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var subject by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var content by remember { mutableStateOf<EnrichmentContent?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var justCopied by remember { mutableStateOf(false) }

    val genFailedLabel = stringResource(R.string.err_lesson_prep_gen_failed)
    val fieldsRequiredLabel = stringResource(R.string.err_lesson_prep_fields_required)

    LaunchedEffect(justCopied) { if (justCopied) { delay(1500); justCopied = false } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.enrichment_heading)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.enrichment_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.size(14.dp))

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
                label = { Text(stringResource(R.string.ph_topic)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(12.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            Button(
                onClick = {
                    errorMessage = null
                    if (subject.isBlank() || gradeLevel.isBlank() || topic.isBlank()) {
                        errorMessage = fieldsRequiredLabel
                        return@Button
                    }
                    isGenerating = true
                    scope.launch {
                        try {
                            val resp = NetworkModule.backendApi.generateEnrichment(
                                GenerateEnrichmentRequest(subject, gradeLevel, topic, lang),
                            )
                            content = parseAiContent<EnrichmentContent>(resp.contentRaw)
                        } catch (e: Exception) {
                            errorMessage = e.toApiErrorMessage(genFailedLabel)
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.btn_generate_enrichment))
                }
            }

            val c = content
            if (c != null) {
                Spacer(modifier = Modifier.size(20.dp))
                Card {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(c.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(c.description, style = MaterialTheme.typography.bodyMedium)
                        SectionLabel(stringResource(R.string.lp_steps_label))
                        BulletList(c.instructions)
                        if (!c.materialsNeeded.isNullOrBlank()) {
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                "${stringResource(R.string.lp_enrichment_label)}: ${c.materialsNeeded}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(14.dp))
                OutlinedButton(onClick = {
                    val text = buildString {
                        appendLine(c.title)
                        appendLine(c.description)
                        append(c.instructions.joinToString("\n") { "- $it" })
                    }
                    clipboard.setText(AnnotatedString(text))
                    justCopied = true
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(stringResource(if (justCopied) R.string.copied_label else R.string.btn_copy_lesson_prep))
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}
