package com.zakiy.platform.ui.study

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.QuizAttemptRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyQuizScreen(studyState: StudyFlowState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val startTime = remember { mutableLongStateOf(System.currentTimeMillis()) }

    val score = studyState.quizQuestions.count { q ->
        val idx = studyState.quizQuestions.indexOf(q)
        studyState.quizAnswers[idx] == q.correctIndex
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quiz_submit)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (studyState.quizSubmitted) {
                Text(
                    "${stringResource(R.string.quiz_score_label)}: $score / ${studyState.quizQuestions.size}",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.size(16.dp))
            }

            studyState.quizQuestions.forEachIndexed { index, question ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("${index + 1}. ${question.question}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.size(8.dp))
                        question.choices.forEachIndexed { choiceIndex, choice ->
                            val selected = studyState.quizAnswers[index] == choiceIndex
                            val isCorrect = studyState.quizSubmitted && choiceIndex == question.correctIndex
                            val isWrongSelected = studyState.quizSubmitted && selected && choiceIndex != question.correctIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selected,
                                        enabled = !studyState.quizSubmitted,
                                        onClick = { studyState.quizAnswers = studyState.quizAnswers + (index to choiceIndex) },
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, onClick = null, enabled = !studyState.quizSubmitted)
                                Text(
                                    choice,
                                    color = when {
                                        isCorrect -> MaterialTheme.colorScheme.primary
                                        isWrongSelected -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                        if (studyState.quizSubmitted && !question.explanation.isNullOrBlank()) {
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(question.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))
            if (!studyState.quizSubmitted) {
                Button(
                    onClick = {
                        studyState.quizSubmitted = true
                        val timeTaken = ((System.currentTimeMillis() - startTime.longValue) / 1000).toInt()
                        val wrongTopics = studyState.quizQuestions
                            .filterIndexed { i, q -> studyState.quizAnswers[i] != q.correctIndex }
                            .map { it.question.take(40) }
                        scope.launch {
                            runCatching {
                                NetworkModule.backendApi.recordQuizAttempt(
                                    QuizAttemptRequest(
                                        mode = "quiz",
                                        score = studyState.quizQuestions.count { q ->
                                            studyState.quizAnswers[studyState.quizQuestions.indexOf(q)] == q.correctIndex
                                        },
                                        total = studyState.quizQuestions.size,
                                        timeTaken = timeTaken,
                                        wrongTopics = wrongTopics,
                                    ),
                                )
                            }
                        }
                    },
                    enabled = studyState.quizAnswers.size == studyState.quizQuestions.size,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.quiz_submit)) }
            }
        }
    }
}
