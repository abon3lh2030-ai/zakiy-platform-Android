package com.zakiy.platform.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.LibraryBook
import com.zakiy.platform.util.uriToMultipart
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(authManager: AuthManager, onOpenBook: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var books by remember { mutableStateOf<List<LibraryBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingText by remember { mutableStateOf<String?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val genericError = stringResource(R.string.error_generic)

    suspend fun load() {
        isLoading = true
        books = runCatching { NetworkModule.backendApi.libraryBooks().books }.getOrDefault(emptyList())
        isLoading = false
    }
    LaunchedEffect(Unit) { load() }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isUploading = true
        scope.launch {
            try {
                val part = uriToMultipart(context, uri, "file")
                val contextPart = "library".toRequestBody("text/plain".toMediaTypeOrNull())
                val uploaded = NetworkModule.backendApi.upload(part, contextPart)
                val extracted = NetworkModule.backendApi.extract(mapOf("filename" to uploaded.filename))
                pendingUri = uri
                pendingText = extracted.text
                titleInput = uploaded.filename.substringBeforeLast(".")
            } catch (e: Exception) {
                errorMessage = genericError
            } finally {
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_library)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { filePicker.launch("application/pdf") }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_book))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading || isUploading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                books.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.library_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    items(books, key = { it.id }) { book ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(book.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onOpenBook(book.id) }) {
                                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.open_for_study))
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        runCatching { NetworkModule.backendApi.deleteLibraryBook(book.id) }
                                        load()
                                    }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingText != null) {
        AlertDialog(
            onDismissRequest = { pendingText = null },
            title = { Text(stringResource(R.string.add_book)) },
            text = {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(stringResource(R.string.book_title_placeholder)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val text = pendingText.orEmpty()
                    val title = titleInput.trim()
                    pendingText = null
                    if (title.isNotBlank()) {
                        scope.launch {
                            runCatching {
                                NetworkModule.backendApi.createLibraryBook(mapOf("title" to title, "extracted_text" to text))
                            }
                            load()
                        }
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pendingText = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
