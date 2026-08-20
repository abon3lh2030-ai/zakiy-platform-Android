package com.zakiy.platform.ui.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.zakiy.platform.network.dto.LibraryBook
import com.zakiy.platform.util.uriToMultipart
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/** اختيار كتاب يلخّصه المساعد الذكي - إما من مكتبتك أو رفع ملف PDF جديد.
 * onPicked يمرّر (عنوان الكتاب، النص المستخرج) لصفحة المحادثة عبر
 * savedStateHandle (راجع تسجيل المسار بـ MainNavHost/RoleNavHost). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBookPickerScreen(onPicked: (title: String, text: String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var books by remember { mutableStateOf<List<LibraryBook>>(emptyList()) }
    var isLoadingLibrary by remember { mutableStateOf(true) }
    var loadingBookId by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val genericError = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        isLoadingLibrary = true
        books = runCatching { NetworkModule.backendApi.libraryBooks().books }.getOrDefault(emptyList())
        isLoadingLibrary = false
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isUploading = true
        scope.launch {
            try {
                val part = uriToMultipart(context, uri, "file")
                val contextPart = "library".toRequestBody("text/plain".toMediaTypeOrNull())
                val uploaded = NetworkModule.backendApi.upload(part, contextPart)
                val extracted = NetworkModule.backendApi.extract(mapOf("filename" to uploaded.filename))
                onPicked(uploaded.filename.substringBeforeLast("."), extracted.text)
            } catch (e: Exception) {
                errorMessage = genericError
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_summarize_book)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.ai_source_library)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.ai_source_upload)) })
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }

            if (selectedTab == 0) {
                when {
                    isLoadingLibrary -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    books.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.library_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
                        items(books, key = { it.id }) { book ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable(enabled = loadingBookId == null) {
                                        loadingBookId = book.id
                                        scope.launch {
                                            val detail = runCatching { NetworkModule.backendApi.libraryBook(book.id) }.getOrNull()
                                            loadingBookId = null
                                            if (detail != null) onPicked(detail.title, detail.extractedText) else errorMessage = genericError
                                        }
                                    },
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(book.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    if (loadingBookId == book.id) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    if (isUploading) {
                        CircularProgressIndicator()
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(stringResource(R.string.ai_upload_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.size(16.dp))
                            Button(onClick = { filePicker.launch("application/pdf") }) {
                                Text(stringResource(R.string.ai_pick_file))
                            }
                        }
                    }
                }
            }
        }
    }
}
