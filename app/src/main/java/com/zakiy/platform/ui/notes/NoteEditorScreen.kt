package com.zakiy.platform.ui.notes

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.ChecklistItem
import com.zakiy.platform.network.dto.NoteDetail
import com.zakiy.platform.network.dto.NoteFolder
import com.zakiy.platform.network.dto.UpdateNoteRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(noteId: String, onBack: () -> Unit) {
    var note by remember { mutableStateOf<NoteDetail?>(null) }
    var folders by remember { mutableStateOf<List<NoteFolder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasLoaded by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        folders = runCatching { NetworkModule.backendApi.noteFolders().folders }.getOrDefault(emptyList())
        note = runCatching { NetworkModule.backendApi.note(noteId) }.getOrNull()
        isLoading = false
        hasLoaded = true
    }

    // حفظ فوري (مو مؤجّل) - لتغييرات منفصلة (مجلد/نوع/تثبيت/عناصر تو-دو)
    fun saveNow(patch: UpdateNoteRequest) {
        scope.launch { runCatching { NetworkModule.backendApi.updateNote(noteId, patch) } }
    }

    // حفظ مؤجّل (debounce) - للعنوان والمحتوى اللي يُكتبون باستمرار
    LaunchedEffect(note?.title) {
        if (!hasLoaded) return@LaunchedEffect
        val current = note ?: return@LaunchedEffect
        delay(600)
        runCatching { NetworkModule.backendApi.updateNote(noteId, UpdateNoteRequest(title = current.title)) }
    }
    LaunchedEffect(note?.content) {
        if (!hasLoaded) return@LaunchedEffect
        val current = note ?: return@LaunchedEffect
        delay(600)
        runCatching { NetworkModule.backendApi.updateNote(noteId, UpdateNoteRequest(content = current.content)) }
    }
    // نفس مبدأ العنوان/المحتوى - يغطي تعديل نص أي عنصر تو-دو (تبديل الحالة/
    // الحذف/الإضافة يحفظون فورًا بأنفسهم عبر saveNow، هذا يغطي الكتابة المستمرة)
    LaunchedEffect(note?.checklistItems) {
        if (!hasLoaded) return@LaunchedEffect
        val current = note ?: return@LaunchedEffect
        delay(600)
        runCatching { NetworkModule.backendApi.updateNote(noteId, UpdateNoteRequest(checklistItems = current.checklistItems)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val current = note
                    Text(current?.title?.ifBlank { stringResource(R.string.notes_untitled) } ?: stringResource(R.string.notes_untitled))
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    val current = note
                    if (current != null) {
                        IconButton(onClick = {
                            val updated = current.copy(isPinned = !current.isPinned)
                            note = updated
                            saveNow(UpdateNoteRequest(isPinned = updated.isPinned))
                        }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (current.isPinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) { padding ->
        val current = note
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            current == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.notes_empty))
            }
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                TextField(
                    value = current.title,
                    onValueChange = { note = current.copy(title = it) },
                    placeholder = { Text(stringResource(R.string.note_title_placeholder)) },
                    textStyle = MaterialTheme.typography.titleLarge,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.size(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(onClick = { showFolderMenu = true }) {
                            Text(folders.firstOrNull { it.id == current.folderId }?.name ?: stringResource(R.string.notes_folder_unfiled))
                        }
                        DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.notes_folder_unfiled)) }, onClick = {
                                showFolderMenu = false
                                note = current.copy(folderId = null)
                                saveNow(UpdateNoteRequest(folderId = ""))
                            })
                            folders.forEach { folder ->
                                DropdownMenuItem(text = { Text(folder.name) }, onClick = {
                                    showFolderMenu = false
                                    note = current.copy(folderId = folder.id)
                                    saveNow(UpdateNoteRequest(folderId = folder.id))
                                })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    NoteTypeToggle(
                        noteType = current.noteType,
                        onChange = { newType ->
                            note = current.copy(noteType = newType)
                            saveNow(UpdateNoteRequest(noteType = newType))
                        },
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))

                if (current.noteType == "checklist") {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(current.checklistItems.size) { index ->
                            val item = current.checklistItems[index]
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Checkbox(
                                    checked = item.done,
                                    onCheckedChange = { checked ->
                                        val items = current.checklistItems.toMutableList()
                                        items[index] = item.copy(done = checked)
                                        note = current.copy(checklistItems = items)
                                        saveNow(UpdateNoteRequest(checklistItems = items))
                                    },
                                )
                                TextField(
                                    value = item.text,
                                    onValueChange = { text ->
                                        val items = current.checklistItems.toMutableList()
                                        items[index] = item.copy(text = text)
                                        note = current.copy(checklistItems = items)
                                    },
                                    textStyle = if (item.done) {
                                        MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough)
                                    } else {
                                        MaterialTheme.typography.bodyMedium
                                    },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = {
                                    val items = current.checklistItems.toMutableList()
                                    items.removeAt(index)
                                    note = current.copy(checklistItems = items)
                                    saveNow(UpdateNoteRequest(checklistItems = items))
                                }) { Icon(Icons.Filled.Delete, contentDescription = null) }
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = newChecklistText,
                                    onValueChange = { newChecklistText = it },
                                    placeholder = { Text(stringResource(R.string.note_checklist_add_placeholder)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = {
                                    val text = newChecklistText.trim()
                                    if (text.isNotEmpty()) {
                                        val items = current.checklistItems.toMutableList()
                                        items.add(ChecklistItem(text = text, done = false))
                                        note = current.copy(checklistItems = items)
                                        newChecklistText = ""
                                        saveNow(UpdateNoteRequest(checklistItems = items))
                                    }
                                }) { Icon(Icons.Filled.Add, contentDescription = null) }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = current.content,
                        onValueChange = { note = current.copy(content = it) },
                        placeholder = { Text(stringResource(R.string.note_content_placeholder)) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.notes_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        runCatching { NetworkModule.backendApi.deleteNote(noteId) }
                        onBack()
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun NoteTypeToggle(noteType: String, onChange: (String) -> Unit) {
    Row {
        listOf("text" to R.string.note_type_text, "checklist" to R.string.note_type_checklist).forEach { (type, labelRes) ->
            val isActive = noteType == type
            Surface(
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                modifier = Modifier.clickable { if (!isActive) onChange(type) }.padding(end = 6.dp),
            ) {
                Text(
                    stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
