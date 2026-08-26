package com.zakiy.platform.ui.quizzes

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.zakiy.platform.network.dto.QuizAttemptDto
import com.zakiy.platform.network.dto.StudentQuizDetail
import com.zakiy.platform.network.dto.SubmitQuizRequest
import com.zakiy.platform.ui.common.PLATFORM_MADRASATI
import com.zakiy.platform.ui.common.QuestionAnswerCard
import com.zakiy.platform.ui.common.openMadrasatiLink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

/** يحوّل توقيت ISO من الباك إند إلى epoch millis - لو ما فيه معلومة منطقة
 * زمنية (Python isoformat() الافتراضي) نفترض UTC بإضافة Z. */
private fun parseIsoToEpochMillis(iso: String): Long? = runCatching {
    val hasZone = iso.endsWith("Z") || Regex("[+-]\\d{2}:?\\d{2}$").containsMatchIn(iso)
    val normalized = if (hasZone) iso else "${iso}Z"
    Instant.parse(normalized).toEpochMilli()
}.getOrNull()

private fun formatRemaining(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

/** شاشة أداء الاختبار من جهة الطالب - تستدعي /start عند الفتح (idempotent)،
 * تحسب مهلة العد التنازلي من started_at + المدة، وتسلّم تلقائيًا لو خلص
 * الوقت. لو الاختبار متسلّم أصلًا تعرض شاشة النتيجة على طول بدون /start.
 * لو platform="madrasati" يُحل بالكامل هناك - نعرض زر فتح مدرستي بس، بدون
 * أي استدعاء /start أو /submit (الباك إند أصلًا يرفضهم بـ 400). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTakeScreen(quizId: String, onBack: () -> Unit) {
    var detail by remember { mutableStateOf<StudentQuizDetail?>(null) }
    var attempt by remember { mutableStateOf<QuizAttemptDto?>(null) }
    var result by remember { mutableStateOf<QuizAttemptDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val answers = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun submit(auto: Boolean) {
        if (isSubmitting || result != null) return
        isSubmitting = true
        val submitted = runCatching {
            NetworkModule.backendApi.submitQuiz(quizId, SubmitQuizRequest(answers.toMap(), auto))
        }.getOrElse {
            errorMessage = it.message
            null
        }
        isSubmitting = false
        if (submitted != null) result = submitted
    }

    LaunchedEffect(Unit) {
        val d = runCatching { NetworkModule.backendApi.studentQuizDetail(quizId) }.getOrElse {
            errorMessage = it.message
            null
        }
        detail = d
        if (d != null && d.platform != PLATFORM_MADRASATI) {
            val existing = d.attempt
            if (existing != null && existing.submittedAt != null) {
                result = existing
            } else {
                val started = runCatching { NetworkModule.backendApi.startQuiz(quizId) }.getOrElse {
                    errorMessage = it.message
                    null
                }
                attempt = started
                started?.answers?.forEach { (k, v) -> answers[k] = v }
            }
        }
        isLoading = false
    }

    val currentAttempt = attempt
    val currentDetail = detail
    var remainingSeconds by remember { mutableStateOf<Long?>(null) }
    if (currentAttempt != null && result == null && currentDetail != null && currentDetail.platform != PLATFORM_MADRASATI) {
        LaunchedEffect(currentAttempt.id) {
            val startedMillis = currentAttempt.startedAt?.let { parseIsoToEpochMillis(it) } ?: System.currentTimeMillis()
            val deadline = startedMillis + (currentDetail.timeLimitMinutes ?: 0) * 60_000L
            while (true) {
                val remaining = deadline - System.currentTimeMillis()
                remainingSeconds = (remaining / 1000).coerceAtLeast(0)
                if (remaining <= 0) {
                    submit(true)
                    break
                }
                delay(1000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentDetail?.title.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            currentDetail == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(errorMessage ?: stringResource(R.string.error_generic))
            }
            currentDetail.platform == PLATFORM_MADRASATI -> MadrasatiQuizView(padding = padding, externalLink = currentDetail.externalLink)
            result != null -> QuizResultView(padding = padding, result = result!!)
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
                remainingSeconds?.let { seconds ->
                    Text(
                        stringResource(R.string.quiz_time_remaining, formatRemaining(seconds)),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (seconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                }

                currentDetail.questions.sortedBy { it.orderIndex }.forEach { question ->
                    QuestionAnswerCard(
                        questionText = question.questionText,
                        questionType = question.questionType,
                        choices = question.choices,
                        currentAnswer = answers[question.id],
                        onAnswerChange = { answers[question.id] = it },
                    )
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.size(8.dp))
                Button(
                    onClick = { scope.launch { submit(false) } },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_submit_quiz))
                }
            }
        }
    }
}

@Composable
private fun MadrasatiQuizView(padding: androidx.compose.foundation.layout.PaddingValues, externalLink: String?) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.madrasati_platform_notice), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (externalLink.isNullOrBlank()) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                stringResource(R.string.madrasati_no_link_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.size(20.dp))
        Button(onClick = { openMadrasatiLink(context, externalLink) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_open_madrasati))
        }
    }
}

@Composable
private fun QuizResultView(padding: androidx.compose.foundation.layout.PaddingValues, result: QuizAttemptDto) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.quiz_result_heading), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.size(16.dp))
        if (result.isGraded) {
            Text(
                stringResource(R.string.quiz_result_graded, result.grade.orEmpty()),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(stringResource(R.string.quiz_result_awaiting), style = MaterialTheme.typography.bodyLarge)
        }
        if (result.autoSubmitted) {
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                stringResource(R.string.quiz_auto_submitted_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
