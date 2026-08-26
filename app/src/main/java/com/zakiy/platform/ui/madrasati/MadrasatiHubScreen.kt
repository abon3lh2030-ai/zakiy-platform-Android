package com.zakiy.platform.ui.madrasati

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.HomeworkHelpSummary
import com.zakiy.platform.network.dto.LessonPrepSummary
import com.zakiy.platform.network.dto.StudyPlanSummary
import kotlinx.coroutines.launch

private const val MADRASATI_URL = "https://schools.madrasati.sa"

/** بطاقة واحدة بشبكة "مدرستي" - أيقونة إيموجي + تسمية + شارة زاوية (مدرستي
 * الرمادية أو ذكيّ التيل). نفس بيانات madrasati-link-card بالموقع بالضبط. */
private data class MdCardItem(val icon: String, val label: String, val isZakiy: Boolean, val onClick: () -> Unit)

/** مركز مدرستي - قسمين واضحين (واجهة طالب/واجهة معلم)، كل قسم شبكة بطاقات:
 * اختصارات مدرستي الرسمية (بدون أي تكامل بيانات) + ميزات ذكيّ الموجودة أصلًا
 * (5 أدوات ذكاء اصطناعي مستقلة تمامًا + اختصارات لشاشات الواجبات/الاختبارات/
 * الجدول/المكتبة/المساعد الذكي المؤسسية). القسم الظاهر افتراضيًا يتحدد حسب
 * دور الحساب - نفس منطق 03-auth.js/28-madrasati.js بالضبط بالموقع. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MadrasatiHubScreen(
    authManager: AuthManager,
    onBack: () -> Unit,
    onOpenAssignments: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenGradesheet: () -> Unit,
    onOpenStudentSchedule: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenAiAssistant: () -> Unit,
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

    val role by authManager.role.collectAsStateWithLifecycle()
    val schoolId by authManager.schoolId.collectAsStateWithLifecycle()
    val educationLevel by authManager.educationLevel.collectAsStateWithLifecycle()

    // نفس شروط 03-auth.js بالضبط: معلم مؤسسي أو حساب فردي اختار "معلم" وقت
    // التسجيل يشوف قسم المعلم بس، إداري المدرسة يشوف الاثنين (يشرف على
    // الطرفين)، وأي شي ثاني (طالب مؤسسي أو فردي عادي/متخرج) يشوف قسم الطالب بس
    val isInstStudent = role == "student"
    val isInstTeacher = role == "teacher"
    val isMadrasatiTeacher = role == "teacher" || (role == null && educationLevel == "معلم")
    val isMadrasatiAdminRole = role == "school_admin" || role == "school_administration"
    val showTeacherSection = isMadrasatiTeacher || isMadrasatiAdminRole
    val showStudentSection = !isMadrasatiTeacher || isMadrasatiAdminRole

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

    fun openMadrasati() {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(MADRASATI_URL))
    }

    // ⚠️ روابط بطاقات "مدرستي" الثمانية كلها مؤقتًا تفتح الصفحة الرئيسية
    // العامة (schools.madrasati.sa) - لازم تُستبدل لاحقًا بالرابط المباشر
    // الصحيح لكل صفحة فرعية (تسليم الواجبات/الدرجات/الجدول/الحضور...) بمجرد
    // ما تتوفر - نفس الوضع المؤقت بالضبط بالموقع (28-madrasati.js)
    val officialStudentCards = listOf(
        MdCardItem("📤", stringResource(R.string.md_submit_assignments), false, ::openMadrasati),
        MdCardItem("📊", stringResource(R.string.md_view_grades), false, ::openMadrasati),
        MdCardItem("🗓️", stringResource(R.string.md_study_schedule), false, ::openMadrasati),
        MdCardItem("✅", stringResource(R.string.md_attendance), false, ::openMadrasati),
    )
    val officialTeacherCards = listOf(
        MdCardItem("📤", stringResource(R.string.md_manage_assignments), false, ::openMadrasati),
        MdCardItem("✏️", stringResource(R.string.md_record_grades), false, ::openMadrasati),
        MdCardItem("🗓️", stringResource(R.string.md_class_schedule), false, ::openMadrasati),
        MdCardItem("✅", stringResource(R.string.md_record_attendance), false, ::openMadrasati),
    )

    val sAssignments = stringResource(R.string.assignments)
    val sQuizzes = stringResource(R.string.quizzes)
    val sMySchedule = stringResource(R.string.nav_my_schedule)
    val sLibrary = stringResource(R.string.nav_library)
    val sAiHelp = stringResource(R.string.nav_ai_help)
    val sHomeworkHelpLabel = stringResource(R.string.md_homework_help_label)
    val sStudyPlanLabel = stringResource(R.string.md_study_plan_label)
    val sGradesheetLabel = stringResource(R.string.md_gradesheet_label)
    val sLessonPrepLabel = stringResource(R.string.md_lesson_prep_label)
    val sEnrichmentLabel = stringResource(R.string.md_enrichment_label)
    val sResultsAnalysisLabel = stringResource(R.string.md_results_analysis_label)

    val studentZakiyCards = buildList {
        if (isInstStudent) add(MdCardItem("📚", sAssignments, true, onOpenAssignments))
        if (isInstStudent) add(MdCardItem("📝", sQuizzes, true, onOpenQuizzes))
        if (isInstStudent && schoolId != null) add(MdCardItem("🗓️", sMySchedule, true, onOpenStudentSchedule))
        add(MdCardItem("📚", sLibrary, true, onOpenLibrary))
        add(MdCardItem("🤖", sAiHelp, true, onOpenAiAssistant))
        add(MdCardItem("📚", sHomeworkHelpLabel, true, onNewHomeworkHelp))
        add(MdCardItem("🗓️", sStudyPlanLabel, true, onNewStudyPlan))
    }
    val teacherZakiyCards = buildList {
        if (isInstTeacher) add(MdCardItem("📚", sAssignments, true, onOpenAssignments))
        if (isInstTeacher) add(MdCardItem("📝", sQuizzes, true, onOpenQuizzes))
        if (isInstTeacher) add(MdCardItem("📋", sGradesheetLabel, true, onOpenGradesheet))
        add(MdCardItem("🧠", sLessonPrepLabel, true, onNewLessonPrep))
        add(MdCardItem("🌟", sEnrichmentLabel, true, onOpenEnrichment))
        add(MdCardItem("📊", sResultsAnalysisLabel, true, onOpenResultsAnalysis))
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
            Spacer(modifier = Modifier.size(18.dp))

            if (showStudentSection) {
                Text(stringResource(R.string.student_interface_heading), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(10.dp))
                MdCardsGrid(officialStudentCards + studentZakiyCards)

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
            }

            if (showStudentSection && showTeacherSection) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            } else if (showStudentSection) {
                Spacer(modifier = Modifier.size(24.dp))
            }

            if (showTeacherSection) {
                Text(stringResource(R.string.teacher_interface_heading), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(10.dp))
                MdCardsGrid(officialTeacherCards + teacherZakiyCards)

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
                Spacer(modifier = Modifier.size(24.dp))
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(stringResource(R.string.madrasati_link_desc), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.size(10.dp))
                    Button(onClick = ::openMadrasati, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.btn_open_madrasati))
                    }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

/** شبكة بطاقتين بالصف - نفس تباعد/زوايا بطاقات المنصة الافتراضية (بدون
 * تصميم جديد)، البطاقة الأخيرة الفردية تاخذ نص العرض بدون ما تتمدد لعرض كامل. */
@Composable
private fun MdCardsGrid(items: List<MdCardItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) { MdCard(item) }
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MdCard(item: MdCardItem) {
    Card(onClick = item.onClick, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 14.dp, start = 10.dp, end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(item.icon, fontSize = 26.sp)
                Spacer(modifier = Modifier.size(6.dp))
                Text(item.label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
            }
            MdBadge(
                label = if (item.isZakiy) stringResource(R.string.md_badge_zakiy) else stringResource(R.string.md_badge_madrasati),
                isZakiy = item.isZakiy,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
    }
}

/** شارة زاوية صغيرة (كبسولة) - رمادي محايد لـ"مدرستي"، تيل هوية ذكيّ (لون
 * primary بالثيم) لـ"ذكيّ" - نفس نمط FilterChipView المستخدم بدفتر الملاحظات. */
@Composable
private fun MdBadge(label: String, isZakiy: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (isZakiy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isZakiy) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
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
