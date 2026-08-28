package com.zakiy.platform.ui.ai

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
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.AiConversationSummary
import kotlinx.coroutines.launch

/** قائمة محادثات المساعد الذكي - نقطة الدخول الرئيسية للميزة، متاحة لأي
 * حساب مسجّل دخول (فردي أو مؤسسي، بدون أي تقييد دور). تعرض كل محادثة
 * سابقة وتقدر تبدأ وحدة جديدة عبر زر +. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConversationsScreen(onOpenConversation: (String) -> Unit, onBack: () -> Unit) {
    var conversations by remember { mutableStateOf<List<AiConversationSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val genericError = stringResource(R.string.error_generic)

    suspend fun load() {
        isLoading = true
        runCatching { NetworkModule.backendApi.aiConversations().conversations }
            .onSuccess { conversations = it; errorMessage = null }
            .onFailure { errorMessage = genericError }
        isLoading = false
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_assistant)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isCreating) return@FloatingActionButton
                    isCreating = true
                    scope.launch {
                        val convo = runCatching { NetworkModule.backendApi.createAiConversation() }.getOrNull()
                        isCreating = false
                        if (convo != null) onOpenConversation(convo.id) else errorMessage = genericError
                    }
                },
            ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.ai_new_conversation)) }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                conversations.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.ai_conversations_empty),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
                    items(conversations, key = { it.id }) { convo ->
                        AiConversationRow(convo, onClick = { onOpenConversation(convo.id) })
                    }
                }
            }
            errorMessage?.let {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                    Card { Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun AiConversationRow(convo: AiConversationSummary, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    convo.title.ifBlank { stringResource(R.string.ai_new_conversation_title) },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (convo.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                if (convo.updatedAt.length >= 10) {
                    Text(convo.updatedAt.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                convo.bookTitle?.let {
                    Text("📚 $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
