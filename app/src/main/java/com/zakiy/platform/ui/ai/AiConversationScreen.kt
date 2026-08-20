package com.zakiy.platform.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.AiMessage
import com.zakiy.platform.network.dto.SendAiMessageRequest
import kotlinx.coroutines.launch

/** محادثة وحدة مع المساعد الذكي - سولفة حرة أو نتيجة تلخيص كتاب. زر الرجوع
 * فوق يسار يودّي لقائمة المحادثات (مطابق لسلوك زر ⋮ بالموقع)، وزر الكتاب
 * جمب صندوق الكتابة يفتح صفحة اختيار كتاب يلخّصه ذكيّ داخل نفس المحادثة.
 *
 * pendingBookTitle/pendingBookText: نتيجة راجعة من AiBookPickerScreen (عبر
 * savedStateHandle حق NavBackStackEntry - لا يوجد آلية callback مباشرة بين
 * وجهتين بـ Navigation Compose) - إذا موجودة نرسلها كطلب تلخيص فورًا، ثم
 * onConsumedPendingBook يمسحها عشان ما تتكرر لو رجعنا لنفس الشاشة. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConversationScreen(
    conversationId: String,
    pendingBookTitle: String?,
    pendingBookText: String?,
    onConsumedPendingBook: () -> Unit,
    onOpenBookPicker: () -> Unit,
    onBack: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<AiMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val genericError = stringResource(R.string.error_generic)
    val newChatTitle = stringResource(R.string.ai_new_conversation_title)
    val summarizeLabel = stringResource(R.string.ai_summarize_book)
    val lang = remember { java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" } }

    suspend fun scrollToBottom() {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    suspend fun sendPayload(body: SendAiMessageRequest, displayText: String) {
        errorMessage = null
        messages = messages + AiMessage(role = "user", content = displayText)
        scrollToBottom()
        isSending = true
        val result = runCatching { NetworkModule.backendApi.sendAiMessage(conversationId, body.copy(lang = lang)) }
        isSending = false
        result.onSuccess { res ->
            messages = messages + AiMessage(role = "assistant", content = res.reply)
            if (!res.title.isNullOrBlank()) title = res.title
            scrollToBottom()
        }.onFailure { errorMessage = genericError }
    }

    suspend fun load() {
        isLoading = true
        runCatching { NetworkModule.backendApi.aiConversation(conversationId) }
            .onSuccess {
                title = it.title
                messages = it.messages
                errorMessage = null
            }
            .onFailure { errorMessage = genericError }
        isLoading = false
    }
    LaunchedEffect(conversationId) { load() }

    LaunchedEffect(pendingBookTitle, pendingBookText) {
        val bookTitle = pendingBookTitle
        val bookText = pendingBookText
        if (!bookTitle.isNullOrBlank() && !bookText.isNullOrBlank()) {
            onConsumedPendingBook()
            sendPayload(SendAiMessageRequest(bookTitle = bookTitle, bookText = bookText), "📚 $summarizeLabel: $bookTitle")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title.ifBlank { newChatTitle }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                        items(messages) { message -> AiMessageBubble(message) }
                        if (isSending) {
                            item { AiTypingBubble() }
                        }
                    }
                }
            }
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onOpenBookPicker) {
                    Icon(Icons.Filled.MenuBook, contentDescription = summarizeLabel)
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.ai_message_placeholder)) },
                )
                Spacer(modifier = Modifier.size(4.dp))
                IconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isEmpty() || isSending) return@IconButton
                        input = ""
                        scope.launch { sendPayload(SendAiMessageRequest(content = text), text) }
                    },
                    enabled = input.isNotBlank() && !isSending,
                ) { Icon(Icons.Filled.Send, contentDescription = stringResource(R.string.ai_send)) }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(message: AiMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiTypingBubble() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Start) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
            CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(16.dp), strokeWidth = 2.dp)
        }
    }
}
