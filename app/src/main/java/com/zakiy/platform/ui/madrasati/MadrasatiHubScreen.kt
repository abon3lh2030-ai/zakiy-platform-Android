package com.zakiy.platform.ui.madrasati

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.HomeworkHelpSummary
import com.zakiy.platform.network.dto.LessonPrepSummary
import com.zakiy.platform.network.dto.StudyPlanSummary
import kotlinx.coroutines.launch

private const val MADRASATI_URL = "https://schools.madrasati.sa"

/** مركز مدرستي - اختصار للموقع الرسمي + أدوات ذكيّ المستقلة للمعلم والطالب.
 * متاح لأي حساب مسجّل دخول بدون تقييد دور (نفس السلوك بالضبط لزر السايد بار
 * "مدرستي" بالموقع - يعرض قسمي المعلم والطالب سوا بدون فحص دور الحساب). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MadrasatiHubScreen(
    onBack: () -> Unit,
    onNewLessonPrep: () -> Unit,
    onOpenLessonPrep: (String) -> Unit,
    onOpenEnrichment: () -> Unit,
    onOpenResultsAnalysis: () -> Unit,
    onNewHomeworkHelp: () -> Unit,
    onOpenHomeworkHelp: (String) -> Unit,
    onNewStudyPlan: () -> Unit,
    onOpenStudyPlan: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var lessonPreps by remember { mutableStateOf<List<LessonPrepSummary>>(emptyList()) }
    var homeworkSessions by remember { mutableStateOf<List<HomeworkHelpSummary>>(emptyList()) }
    var studyPlans by remember { mutableStateOf<List<StudyPlanSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun reload() {
        isLoading = true
        lessonPreps = runCatching { NetworkModule.backendApi.lessonPreps().preparations }.getOrDefault(emptyList())
        homeworkSessions = runCatching { NetworkModule.backendApi.homeworkHelpSessions().sessions }.getOrDefault(emptyList())
        studyPlans = runCatching { NetworkModule.backendApi.studyPlans().plans }.getOrDefault(emptyList())
        isLoading = false
    }

    LaunchedEffect(Unit) { reload() }

    // نعيد التحميل كل مرة الشاشة ترجع للواجهة (بعد حفظ/حذف من شاشة فرعية)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { reload() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.madrasati_heading)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.madrasati_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.size(14.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(stringResource(R.string.madrasati_link_desc), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.size(10.dp))
                    Button(
                        onClick = {
                            val intent = CustomTabsIntent.Builder().build()
                            intent.launchUrl(context, Uri.parse(MADRASATI_URL))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.btn_open_madrasati))
                    }
                }
            }

            Spacer(modifier = Modifier.size(24.dp))
            Text(stringResource(R.string.teacher_tools_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.size(10.dp))
            ToolButton(Icons.Filled.Add, stringResource(R.string.btn_new_lesson_prep), primary = true, onClick = onNewLessonPrep)
            Spacer(modifier = Modifier.size(8.dp))
            ToolButton(Icons.Filled.AutoAwesome, stringResource(R.string.btn_open_enrichment), onClick = onOpenEnrichment)
            Spacer(modifier = Modifier.size(8.dp))
            ToolButton(Icons.Filled.Assessment, stringResource(R.string.btn_open_results_analysis), onClick = onOpenResultsAnalysis)

            Spacer(modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.saved_lesson_preps_heading), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.size(6.dp))
            when {
                isLoading -> LoadingRow()
                lessonPreps.isEmpty() -> Text(stringResource(R.string.lesson_prep_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> lessonPreps.forEach { prep ->
                    SavedItemCard(
                        title = prep.lessonTitle,
                        subtitle = listOfNotNull(prep.subject, prep.gradeLevel, prep.unit).joinToString(" · "),
                        onClick = { onOpenLessonPrep(prep.id) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text(stringResource(R.string.student_tools_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.size(10.dp))
            ToolButton(Icons.Filled.Add, stringResource(R.string.btn_new_homework_help), primary = true, onClick = onNewHomeworkHelp)
            Spacer(modifier = Modifier.size(8.dp))
            ToolButton(Icons.Filled.CalendarMonth, stringResource(R.string.btn_new_study_plan), onClick = onNewStudyPlan)

            Spacer(modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.saved_homework_help_heading), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.size(6.dp))
            when {
                isLoading -> LoadingRow()
                homeworkSessions.isEmpty() -> Text(stringResource(R.string.homework_help_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> homeworkSessions.forEach { session ->
                    SavedItemCard(
                        title = session.topic,
                        subtitle = "${session.subject} · ${session.gradeLevel}",
                        onClick = { onOpenHomeworkHelp(session.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.saved_study_plans_heading), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.size(6.dp))
            when {
                isLoading -> LoadingRow()
                studyPlans.isEmpty() -> Text(stringResource(R.string.study_plan_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> studyPlans.forEach { plan ->
                    SavedItemCard(
                        title = plan.subjects,
                        subtitle = plan.examDate.orEmpty(),
                        onClick = { onOpenStudyPlan(plan.id) },
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, primary: Boolean = false, onClick: () -> Unit) {
    if (primary) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(label)
        }
    }
}

@Composable
private fun SavedItemCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp))
    }
}
