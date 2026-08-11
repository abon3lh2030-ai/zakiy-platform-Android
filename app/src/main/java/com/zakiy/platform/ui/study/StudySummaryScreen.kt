package com.zakiy.platform.ui.study

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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.GenerateQuizRequest
import com.zakiy.platform.network.dto.QuizQuestion
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySummaryScreen(studyState: StudyFlowState, onNavigateToQuiz: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var numQuestions by remember { mutableStateOf("5") }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val genericError = stringResource(R.string.error_generic)
    val lang = java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" }
    val json = remember { Json { ignoreUnknownKeys = true } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.summary_label)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(studyState.summary.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.size(16.dp))

            OutlinedButton(
                onClick = {
                    isSaving = true
                    savedMessage = null
                    scope.launch {
                        try {
                            NetworkModule.backendApi.createLibraryBook(
                                mapOf("title" to studyState.summary.orEmpty().take(60), "extracted_text" to studyState.extractedText),
                            )
                            savedMessage = "✅"
                        } catch (e: Exception) {
                            errorMessage = genericError
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.btn_save_to_library)) }
            Spacer(modifier = Modifier.size(12.dp))

            OutlinedTextField(
                value = numQuestions,
                onValueChange = { numQuestions = it.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.num_questions_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(12.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.size(8.dp))
            }

            Button(
                onClick = {
                    isGenerating = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val n = (numQuestions.toIntOrNull() ?: 5).coerceIn(5, 20)
                            val result = NetworkModule.backendApi.generateQuiz(
                                GenerateQuizRequest(text = studyState.extractedText, numQuestions = n, lang = lang),
                            )
                            val cleaned = result.quizRaw.trim()
                                .removePrefix("```json").removePrefix("```")
                                .removeSuffix("```").trim()
                            studyState.quizQuestions = json.decodeFromString<List<QuizQuestion>>(cleaned)
                            studyState.quizAnswers = emptyMap()
                            studyState.quizSubmitted = false
                            onNavigateToQuiz()
                        } catch (e: Exception) {
                            errorMessage = genericError
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_generate_quiz))
            }
        }
    }
}
