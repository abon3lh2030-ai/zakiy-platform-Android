package com.zakiy.platform.ui.study

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTextScreen(studyState: StudyFlowState, onNavigateToSummary: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val genericError = stringResource(R.string.error_generic)
    // نفس لغة الواجهة الحالية تُرسل للباك إند عشان ردود الذكاء الاصطناعي تجي بنفس اللغة
    val lang = java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.extracted_text_label)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(studyState.extractedText, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.size(16.dp))
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.size(8.dp))
            }
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val result = NetworkModule.backendApi.summarize(
                                mapOf("text" to studyState.extractedText, "lang" to lang),
                            )
                            studyState.summary = result.summary
                            onNavigateToSummary()
                        } catch (e: Exception) {
                            errorMessage = genericError
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && studyState.extractedText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_summarize))
            }
        }
    }
}
