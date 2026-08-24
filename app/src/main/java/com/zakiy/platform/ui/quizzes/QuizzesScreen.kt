package com.zakiy.platform.ui.quizzes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.QuizSummary
import kotlinx.coroutines.launch

/** قائمة الاختبارات - نفس هيكلة AssignmentsScreen بالضبط: عنصر واحد بالقائمة
 * يحمل حقول المعلم (isPublished/submittedCount/totalCount) وحقول الطالب
 * (submitted/isGraded/grade)، كل جهة تعرض اللي يخصها بس. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzesScreen(authManager: AuthManager, onOpenQuiz: (String, Boolean) -> Unit, onCreateQuiz: () -> Unit) {
    val role by authManager.role.collectAsStateWithLifecycle()
    val isTeacher = role == "teacher"

    var quizzes by remember { mutableStateOf<List<QuizSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        isLoading = true
        quizzes = runCatching {
            if (isTeacher) NetworkModule.backendApi.teacherQuizzes().quizzes
            else NetworkModule.backendApi.studentQuizzes().quizzes
        }.getOrDefault(emptyList())
        isLoading = false
    }

    LaunchedEffect(isTeacher) { reload() }

    // نعيد التحميل كل مرة الشاشة ترجع للواجهة (بعد إنشاء/تعديل/نشر/حذف
    // اختبار من شاشة فرعية) - نفس فكرة onResume، بدون تمرير savedStateHandle
    // عبر كل شاشة فرعية.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { reload() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.quizzes_heading)) }) },
        floatingActionButton = {
            if (isTeacher) {
                FloatingActionButton(onClick = onCreateQuiz) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            quizzes.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.quizzes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                items(quizzes, key = { it.id }) { quiz ->
                    QuizCard(quiz = quiz, isTeacher = isTeacher, onClick = { onOpenQuiz(quiz.id, isTeacher) })
                }
            }
        }
    }
}

@Composable
private fun QuizCard(quiz: QuizSummary, isTeacher: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(quiz.title, style = MaterialTheme.typography.titleSmall)
                Text(quiz.subject, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isTeacher) {
                if (quiz.isPublished == true) {
                    val done = quiz.submittedCount ?: 0
                    val total = quiz.totalCount ?: 0
                    Text(
                        stringResource(R.string.quiz_submitted_count, done, total),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        stringResource(R.string.quiz_status_draft),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                val (text, color) = when {
                    quiz.submitted != true -> stringResource(R.string.quiz_status_not_taken) to MaterialTheme.colorScheme.error
                    quiz.isGraded == true -> stringResource(R.string.quiz_grade_shown, quiz.grade.orEmpty()) to MaterialTheme.colorScheme.primary
                    else -> stringResource(R.string.quiz_status_awaiting_grade) to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(text, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}
