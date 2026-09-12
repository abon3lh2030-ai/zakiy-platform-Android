package com.zakiy.platform.ui.school

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.CurriculumPath
import com.zakiy.platform.network.dto.SchoolCurriculumRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolCurriculumScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var paths by remember { mutableStateOf<List<CurriculumPath>>(emptyList()) }
    var selected by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { runCatching { NetworkModule.backendApi.schoolCurriculumState() }.getOrNull()?.let { paths = it.paths; selected = it.selectedPathId.orEmpty() } }
    Scaffold(topBar = { TopAppBar(title = { Text("مسار كتب المدرسة") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { item {
            Text("اختر مسار الأدمن العام فتظهر كتبه تلقائيًا في مكتبة المدرسة. اختر بدون مسار لرفع كتب المدرسة الخاصة.")
            Row { RadioButton(selected.isEmpty(), { selected = "" }); Text("بدون مسار", modifier = Modifier.padding(top = 12.dp)) }
            paths.forEach { path -> Row { RadioButton(selected == path.id, { selected = path.id }); Text("${path.name} (${path.bookCount ?: 0} كتاب)", modifier = Modifier.padding(top = 12.dp)) } }
            Button(onClick = { scope.launch { runCatching { NetworkModule.backendApi.schoolSetCurriculumPath(SchoolCurriculumRequest(selected.ifEmpty { null })) }.onSuccess { message = "تم حفظ المسار" }.onFailure { message = it.message } } }, modifier = Modifier.fillMaxWidth()) { Text("حفظ المسار") }
            message?.let { Text(it) }
        } }
    }
}
