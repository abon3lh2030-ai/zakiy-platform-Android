package com.zakiy.platform.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.DirectMessage
import com.zakiy.platform.network.dto.SendMessageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(otherUserId: String, otherUsername: String, authManager: AuthManager, onBack: () -> Unit) {
    val myUserId by authManager.userId.collectAsStateWithLifecycle()
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        messages = runCatching { NetworkModule.backendApi.messageThread(otherUserId).messages }.getOrDefault(emptyList())
    }
    LaunchedEffect(otherUserId) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUsername) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                items(messages) { message ->
                    val isMine = message.senderId == myUserId
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                    ) {
                        Card(colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        )) {
                            Text(
                                message.body,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(R.string.type_a_message)) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isEmpty()) return@IconButton
                        input = ""
                        scope.launch {
                            runCatching { NetworkModule.backendApi.sendMessage(SendMessageRequest(otherUserId, text)) }
                            load()
                        }
                    },
                ) { Icon(Icons.Filled.Send, contentDescription = null) }
            }
        }
    }
}
