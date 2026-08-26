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
import androidx.compose.material.icons.filled.Assessment
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.GenerateResultsAnalysisRequest
import com.zakiy.platform.network.dto.ResultsAnalysisContent
import com.zakiy.platform.network.dto.parseAiContent
import com.zakiy.platform.network.dto.toApiErrorMessage
import kotlinx.coroutines.launch

/** محلّل نتائج الطلاب (معلم) - يلصق نتائج نصية خام ويطلّع تحليل، بدون حفظ. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsAnalysisScreen(onBack: () -> Unit) {
    val lang = remember { java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" } }
    val scope = rememberCoroutineScope()

    var rawResults by remember { mutableStateOf("") }
    var content by remember { mutableStateOf<ResultsAnalysisContent?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val genFailedLabel = stringResource(R.string.err_lesson_prep_gen_failed)
    val rawResultsRequiredLabel = stringResource(R.string.err_raw_results_required)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.results_analysis_heading)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.results_analysis_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.size(14.dp))

            OutlinedTextField(
                value = rawResults,
                onValueChange = { rawResults = it },
                label = { Text(stringResource(R.string.ph_raw_results)) },
                minLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(12.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            Button(
                onClick = {
                    errorMessage = null
                    if (rawResults.isBlank()) {
                        errorMessage = rawResultsRequiredLabel
                        return@Button
                    }
                    isGenerating = true
                    scope.launch {
                        try {
                            val resp = NetworkModule.backendApi.generateResultsAnalysis(
                                GenerateResultsAnalysisRequest(rawResults, lang),
                            )
                            content = parseAiContent<ResultsAnalysisContent>(resp.contentRaw)
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
                    Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.btn_generate_results_analysis))
                }
            }

            val c = content
            if (c != null) {
                Spacer(modifier = Modifier.size(20.dp))
                Card {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SectionLabel(stringResource(R.string.ra_summary_label))
                        Text(c.overallSummary, style = MaterialTheme.typography.bodyMedium)
                        SectionLabel(stringResource(R.string.ra_strengths_label))
                        BulletList(c.strengths)
                        SectionLabel(stringResource(R.string.ra_weaknesses_label))
                        BulletList(c.weaknesses)
                        SectionLabel(stringResource(R.string.ra_at_risk_label))
                        BulletList(c.atRiskStudents)
                        SectionLabel(stringResource(R.string.ra_recommendations_label))
                        BulletList(c.recommendations)
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}
