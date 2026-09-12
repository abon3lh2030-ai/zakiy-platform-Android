package com.zakiy.platform.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.*
import com.zakiy.platform.util.uriToMultipart
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCurriculumScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var paths by remember { mutableStateOf<List<CurriculumPath>>(emptyList()) }
    var pathName by remember { mutableStateOf("") }
    var selectedPath by remember { mutableStateOf("") }
    var bookTitle by remember { mutableStateOf("") }
    var bookText by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    suspend fun load() { paths = runCatching { NetworkModule.backendApi.adminCurriculumPaths().paths }.getOrDefault(emptyList()) }
    LaunchedEffect(Unit) { load() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching {
                val upload = NetworkModule.backendApi.upload(uriToMultipart(context, uri, "file"), "library".toRequestBody("text/plain".toMediaTypeOrNull()))
                bookText = NetworkModule.backendApi.extract(mapOf("filename" to upload.filename)).text
                bookTitle = upload.filename.substringBeforeLast('.')
            }.onFailure { message = it.message }
            busy = false
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("مسارات الكتب المدرسية") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                OutlinedTextField(pathName, { pathName = it }, label = { Text("اسم المسار") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { scope.launch { runCatching { NetworkModule.backendApi.adminCreateCurriculumPath(CurriculumPathRequest(pathName.trim())) }; pathName = ""; load() } }, enabled = pathName.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("إنشاء المسار") }
                Text("رفع كتاب للمسار", style = MaterialTheme.typography.titleMedium)
                paths.forEach { path -> FilterChip(selected = selectedPath == path.id, onClick = { selectedPath = path.id }, label = { Text(path.name) }) }
                OutlinedButton(onClick = { picker.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) { Text("اختيار ملف PDF") }
                if (bookText != null) {
                    OutlinedTextField(bookTitle, { bookTitle = it }, label = { Text("اسم الكتاب") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch { val text = bookText ?: return@launch; busy = true; runCatching { NetworkModule.backendApi.adminAddCurriculumBook(selectedPath, CurriculumBookRequest(bookTitle.trim(), text)) }.onFailure { message = it.message }; bookText = null; bookTitle = ""; load(); busy = false } }, enabled = selectedPath.isNotEmpty() && bookTitle.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("رفع الكتاب") }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            items(paths, key = { it.id }) { path ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth()) { Text(path.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); IconButton(onClick = { scope.launch { NetworkModule.backendApi.adminDeleteCurriculumPath(path.id); load() } }) { Icon(Icons.Filled.Delete, null) } }
                    path.books.forEach { book -> Row(Modifier.fillMaxWidth()) { Text(book.title, modifier = Modifier.weight(1f)); IconButton(onClick = { scope.launch { NetworkModule.backendApi.adminDeleteCurriculumBook(book.id); load() } }) { Icon(Icons.Filled.Delete, null) } } }
                } }
            }
        }
    }
}
