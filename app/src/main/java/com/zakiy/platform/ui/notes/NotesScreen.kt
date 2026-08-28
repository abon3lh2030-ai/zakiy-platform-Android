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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Surface
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
import com.zakiy.platform.network.dto.CreateNoteRequest
import com.zakiy.platform.network.dto.NoteFolder
import com.zakiy.platform.network.dto.NoteSummary
import kotlinx.coroutines.launch

private sealed class FolderFilter {
    object All : FolderFilter()
    object Unfiled : FolderFilter()
    data class ById(val id: String) : FolderFilter()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(onOpenNote: (String) -> Unit, onBack: () -> Unit) {
    var folders by remember { mutableStateOf<List<NoteFolder>>(emptyList()) }
    var notes by remember { mutableStateOf<List<NoteSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf<FolderFilter>(FolderFilter.All) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderPendingDelete by remember { mutableStateOf<NoteFolder?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reloadNotes() {
        val folderId = (activeFilter as? FolderFilter.ById)?.id
        notes = runCatching {
            NetworkModule.backendApi.notes(folderId = folderId, query = query.ifBlank { null }).notes
        }.getOrDefault(emptyList())
        if (activeFilter is FolderFilter.Unfiled) {
            notes = notes.filter { it.folderId == null }
        }
    }

    suspend fun reloadAll() {
        isLoading = true
        folders = runCatching { NetworkModule.backendApi.noteFolders().folders }.getOrDefault(emptyList())
        reloadNotes()
        isLoading = false
    }

    LaunchedEffect(Unit) { reloadAll() }
    LaunchedEffect(query, activeFilter) { reloadNotes() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_heading)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    val folderId = (activeFilter as? FolderFilter.ById)?.id
                    val created = runCatching {
                        NetworkModule.backendApi.createNote(CreateNoteRequest(folderId = folderId))
                    }.getOrNull()
                    if (created != null) onOpenNote(created.id)
                }
            }) { Icon(Icons.Filled.Add, contentDescription = null) }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.notes_search_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )

            LazyRow(modifier = Modifier.padding(horizontal = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChipView(
                        stringResource(R.string.notes_folder_all), activeFilter is FolderFilter.All,
                        onClick = { activeFilter = FolderFilter.All },
                    )
                }
                item {
                    FilterChipView(
                        stringResource(R.string.notes_folder_unfiled), activeFilter is FolderFilter.Unfiled,
                        onClick = { activeFilter = FolderFilter.Unfiled },
                    )
                }
                items(folders, key = { it.id }) { folder ->
                    FilterChipView(
                        label = folder.name,
                        isActive = (activeFilter as? FolderFilter.ById)?.id == folder.id,
                        onClick = { activeFilter = FolderFilter.ById(folder.id) },
                        onDelete = { folderPendingDelete = folder },
                    )
                }
                item {
                    FilterChipView(
                        "+ " + stringResource(R.string.notes_new_folder), false,
                        onClick = { showNewFolderDialog = true },
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))

            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                notes.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.notes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(note = note, folderName = folders.firstOrNull { it.id == note.folderId }?.name, onClick = { onOpenNote(note.id) })
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false; newFolderName = "" },
            title = { Text(stringResource(R.string.notes_new_folder)) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text(stringResource(R.string.note_folder_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newFolderName.trim()
                    showNewFolderDialog = false
                    newFolderName = ""
                    if (name.isNotEmpty()) {
                        scope.launch {
                            runCatching { NetworkModule.backendApi.createNoteFolder(mapOf("name" to name)) }
                            reloadAll()
                        }
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false; newFolderName = "" }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    folderPendingDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderPendingDelete = null },
            title = { Text(stringResource(R.string.notes_delete_folder_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    folderPendingDelete = null
                    scope.launch {
                        runCatching { NetworkModule.backendApi.deleteNoteFolder(folder.id) }
                        if (activeFilter == FolderFilter.ById(folder.id)) activeFilter = FolderFilter.All
                        reloadAll()
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { folderPendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun FilterChipView(label: String, isActive: Boolean, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            if (onDelete != null) {
                Spacer(modifier = Modifier.size(6.dp))
                Text("✕", style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable(onClick = onDelete))
            }
        }
    }
}

@Composable
private fun NoteCard(note: NoteSummary, folderName: String?, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.title.trim().ifEmpty { stringResource(R.string.notes_untitled) },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (note.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                if (folderName != null) {
                    Text(folderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (note.isPinned) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
