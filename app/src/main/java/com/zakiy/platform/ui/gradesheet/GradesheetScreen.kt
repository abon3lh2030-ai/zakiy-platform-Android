package com.zakiy.platform.ui.gradesheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.GradesheetStudentRow
import com.zakiy.platform.network.dto.SchoolClassSummary
import com.zakiy.platform.network.dto.UpdateGradesheetRowRequest
import com.zakiy.platform.network.dto.toApiErrorMessage
import kotlinx.coroutines.launch

/** كشف الدرجات (معلم بس) - نفس /api/teacher/gradesheet بالموقع بالضبط:
 * اختيار فصل (نفس نمط اختيار الفصل بـ AssignmentCreateSheet/QuizEditScreen)
 * ثم قائمة طلاب الفصل، كل طالب بطاقة فيها حقلا إدخال (المشاركة/المهام
 * الأدائية) وزر حفظ لهما بس، وثلاث قيم للقراءة فقط (الواجبات/الاختبارات
 * محسوبة تلقائيًا من التصحيح، والمجموع محسوب بالباك إند) - المعلم ما يقدر
 * يعدّل المجموع أبدًا. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesheetScreen() {
    var classes by remember { mutableStateOf<List<SchoolClassSummary>>(emptyList()) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var classMenuExpanded by remember { mutableStateOf(false) }
    var students by remember { mutableStateOf<List<GradesheetStudentRow>>(emptyList()) }
    var isLoadingClasses by remember { mutableStateOf(true) }
    var isLoadingTable by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reloadTable(classId: String) {
        isLoadingTable = true
        errorMessage = null
        runCatching { NetworkModule.backendApi.teacherGradesheet(classId).students }
            .onSuccess { students = it }
            .onFailure {
                students = emptyList()
                errorMessage = it.toApiErrorMessage(errorMessage ?: "")
            }
        isLoadingTable = false
    }

    LaunchedEffect(Unit) {
        isLoadingClasses = true
        classes = runCatching { NetworkModule.backendApi.teacherRoster().classes }.getOrDefault(emptyList())
        selectedClassId = classes.firstOrNull()?.id
        isLoadingClasses = false
        selectedClassId?.let { reloadTable(it) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.gradesheet_heading)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            Text(
                stringResource(R.string.gradesheet_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))

            if (classes.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(classes.firstOrNull { it.id == selectedClassId }?.name ?: stringResource(R.string.gradesheet_class_label))
                    TextButton(onClick = { classMenuExpanded = true }) { Text("▾") }
                    DropdownMenu(expanded = classMenuExpanded, onDismissRequest = { classMenuExpanded = false }) {
                        classes.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    selectedClassId = c.id
                                    classMenuExpanded = false
                                    scope.launch { reloadTable(c.id) }
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            when {
                isLoadingClasses || isLoadingTable -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                classes.isEmpty() || students.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.gradesheet_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(students, key = { it.userId }) { student ->
                        GradesheetRow(
                            student = student,
                            classId = selectedClassId!!,
                            onSaved = { scope.launch { reloadTable(selectedClassId!!) } },
                            onError = { errorMessage = it },
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GradesheetRow(
    student: GradesheetStudentRow,
    classId: String,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    var participationText by remember(student.userId) { mutableStateOf(formatNumber(student.participation)) }
    var performanceText by remember(student.userId) { mutableStateOf(formatNumber(student.performanceTasks)) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(student.fullName ?: student.username, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.size(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = participationText,
                    onValueChange = { participationText = it },
                    label = { Text(stringResource(R.string.gradesheet_participation_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedTextField(
                    value = performanceText,
                    onValueChange = { performanceText = it },
                    label = { Text(stringResource(R.string.gradesheet_performance_tasks_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Button(
                onClick = {
                    val participation = participationText.toDoubleOrNull()
                    val performance = performanceText.toDoubleOrNull()
                    isSaving = true
                    scope.launch {
                        runCatching {
                            NetworkModule.backendApi.updateGradesheetRow(
                                student.userId,
                                UpdateGradesheetRowRequest(classId = classId, participation = participation, performanceTasks = performance),
                            )
                        }.onSuccess { onSaved() }
                            .onFailure { onError(it.toApiErrorMessage("")) }
                        isSaving = false
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text(stringResource(R.string.btn_save_gradesheet_row))
            }

            Spacer(modifier = Modifier.size(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                GradesheetStat(
                    label = stringResource(R.string.gradesheet_assignments_avg_label),
                    value = formatAvg(student.assignmentsAvg, student.assignmentsCount),
                )
                GradesheetStat(
                    label = stringResource(R.string.gradesheet_quizzes_avg_label),
                    value = formatAvg(student.quizzesAvg, student.quizzesCount),
                )
                GradesheetStat(
                    label = stringResource(R.string.gradesheet_total_label),
                    value = formatNumber(student.total),
                    bold = true,
                )
            }
        }
    }
}

@Composable
private fun GradesheetStat(label: String, value: String, bold: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}

/** يشيل الصفر العشري الزائد (7.0 -> "7") نفس عرض الأرقام بالموقع. */
private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

private fun formatAvg(avg: Double?, count: Int): String =
    "${avg?.let(::formatNumber) ?: "—"} ($count)"
