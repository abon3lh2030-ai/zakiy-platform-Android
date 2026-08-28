package com.zakiy.platform.ui.sciencelab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.AiMessage
import com.zakiy.platform.network.dto.ScienceLabChatRequest
import com.zakiy.platform.network.dto.ScienceLabSummaryRequest
import com.zakiy.platform.network.dto.toApiErrorMessage
import com.zakiy.platform.ui.embeddedweb.EmbeddedWebView
import kotlinx.coroutines.launch

private enum class SlTab { Chemistry, Biology }
private sealed interface SlBioView {
    data object Categories : SlBioView
    data class Grid(val categoryId: String) : SlBioView
    data class Detail(val animalId: String?) : SlBioView // animalId == null -> جسم الإنسان
}

/** مختبر العلوم - الشاشة الرئيسية: تبديل بين تبويب الكيمياء والفيزياء
 * (مدمج بالمتصفح، مطابق للموقع بالضبط - website/src/js/30-science-lab.js
 * مشهد three.js) وتبويب الأحياء (Native بالكامل - مستكشف تصنيفات → حيوانات
 * → تفاصيل بصورة جسم تفاعلية). مساعد الذكاء الاصطناعي وزر تلخيص الجلسة
 * متاحين دايمًا بغض النظر عن التبويب المفتوح (نفس سلوك #slChatPanel
 * بالموقع - مو محصور بتبويب معين). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScienceLabScreen(authManager: AuthManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = remember { java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" } }
    val genericError = stringResource(R.string.error_generic)

    var tab by remember { mutableStateOf(SlTab.Biology) }
    var bioView by remember { mutableStateOf<SlBioView>(SlBioView.Categories) }
    var currentAnimalId by remember { mutableStateOf<String?>(null) }

    val sessionLog = remember { mutableStateListOf<String>() }
    fun logEvent(text: String) { sessionLog.add(text) }

    var chatMessages by remember { mutableStateOf<List<AiMessage>>(emptyList()) }
    var chatInteractionId by remember { mutableStateOf<String?>(null) }
    var chatInput by remember { mutableStateOf("") }
    var chatSending by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }

    var showSummaryDialog by remember { mutableStateOf(false) }
    var summaryLoading by remember { mutableStateOf(false) }
    var summaryText by remember { mutableStateOf("") }
    val summaryEmptyMsg = stringResource(R.string.sl_summary_empty)

    suspend fun sendChat(text: String) {
        val questionPrefix = context.getString(R.string.sl_log_question_prefix)
        logEvent("$questionPrefix: $text")
        chatMessages = chatMessages + AiMessage(role = "user", content = text)
        chatSending = true
        val animalContext = currentAnimalId?.let { id ->
            ScienceLabData.animals[id]?.let { context.getString(it.nameRes) }
        }.orEmpty()
        val body = ScienceLabChatRequest(message = text, lang = lang, context = animalContext, interactionId = chatInteractionId)
        val result = runCatching { NetworkModule.backendApi.scienceLabChat(body) }
        chatSending = false
        result.onSuccess { res ->
            chatInteractionId = res.interactionId
            chatMessages = chatMessages + AiMessage(role = "assistant", content = res.reply)
        }.onFailure {
            chatMessages = chatMessages + AiMessage(role = "assistant", content = it.toApiErrorMessage(genericError))
        }
    }

    fun openSummary() {
        showSummaryDialog = true
        if (sessionLog.isEmpty()) {
            summaryText = summaryEmptyMsg
            return
        }
        summaryLoading = true
        summaryText = ""
        scope.launch {
            val result = runCatching {
                NetworkModule.backendApi.scienceLabSummary(ScienceLabSummaryRequest(log = sessionLog.toList(), lang = lang))
            }
            summaryLoading = false
            result.onSuccess { summaryText = it.summary }.onFailure { summaryText = it.toApiErrorMessage(genericError) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sl_heading)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { openSummary() }) {
                        Icon(Icons.Filled.Summarize, contentDescription = stringResource(R.string.sl_show_summary))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showChatSheet = true }) {
                Icon(Icons.Filled.SmartToy, contentDescription = stringResource(R.string.sl_chat_heading))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SlTabToggle(
                selected = tab,
                onSelect = { tab = it },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    SlTab.Chemistry -> EmbeddedWebView(authManager = authManager, navigateJsFunction = "showScienceLabScreen")
                    SlTab.Biology -> SlBiologyExplorer(
                        bioView = bioView,
                        onBioViewChange = { bioView = it },
                        onAnimalOpened = { currentAnimalId = it },
                        onLog = ::logEvent,
                    )
                }
            }
        }
    }

    if (showChatSheet) {
        ModalBottomSheet(onDismissRequest = { showChatSheet = false }) {
            SlChatSheetContent(
                messages = chatMessages,
                sending = chatSending,
                input = chatInput,
                onInputChange = { chatInput = it },
                onSend = {
                    val text = chatInput.trim()
                    if (text.isEmpty() || chatSending) return@SlChatSheetContent
                    chatInput = ""
                    scope.launch { sendChat(text) }
                },
            )
        }
    }

    if (showSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showSummaryDialog = false },
            title = { Text(stringResource(R.string.sl_summary_title)) },
            text = {
                if (summaryLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(summaryText, modifier = Modifier.verticalScroll(rememberScrollState()))
                }
            },
            confirmButton = {
                TextButton(onClick = { showSummaryDialog = false }) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}

@Composable
private fun SlTabToggle(selected: SlTab, onSelect: (SlTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SlTabButton(
            label = stringResource(R.string.sl_tab_chemistry),
            isSelected = selected == SlTab.Chemistry,
            onClick = { onSelect(SlTab.Chemistry) },
            modifier = Modifier.weight(1f),
        )
        SlTabButton(
            label = stringResource(R.string.sl_tab_biology),
            isSelected = selected == SlTab.Biology,
            onClick = { onSelect(SlTab.Biology) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SlTabButton(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (isSelected) {
        Button(onClick = onClick, modifier = modifier) { Text(label, textAlign = TextAlign.Center) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label, textAlign = TextAlign.Center) }
    }
}

// ---------------------------------------------------------------------
// مستكشف الأحياء - تصنيفات → حيوانات → تفاصيل (صورة جسم + hotspots)
// ---------------------------------------------------------------------

@Composable
private fun SlBiologyExplorer(
    bioView: SlBioView,
    onBioViewChange: (SlBioView) -> Unit,
    onAnimalOpened: (String?) -> Unit,
    onLog: (String) -> Unit,
) {
    val context = LocalContext.current
    when (val view = bioView) {
        is SlBioView.Categories -> {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(stringResource(R.string.sl_bio_pick_category), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(12.dp))
                SlGrid(
                    items = ScienceLabData.categories.map { cat -> Triple(cat.icon, stringResource(cat.nameRes), cat) },
                ) { (icon, label, cat) ->
                    SlGridCard(icon = icon, label = label) {
                        val catPrefix = context.getString(R.string.sl_log_category_prefix)
                        onLog("$catPrefix: $label")
                        if (cat.id == "human") {
                            onAnimalOpened(null)
                            onBioViewChange(SlBioView.Detail(null))
                        } else {
                            onBioViewChange(SlBioView.Grid(cat.id))
                        }
                    }
                }
            }
        }
        is SlBioView.Grid -> {
            val category = ScienceLabData.categories.first { it.id == view.categoryId }
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                TextButton(onClick = { onBioViewChange(SlBioView.Categories) }) {
                    Text(stringResource(R.string.sl_back_to_categories))
                }
                Text(stringResource(category.nameRes), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(12.dp))
                SlGrid(
                    items = category.animalIds.mapNotNull { id -> ScienceLabData.animals[id] }
                        .map { a -> Triple(a.icon, stringResource(a.nameRes), a) },
                ) { (icon, label, animal) ->
                    SlGridCard(icon = icon, label = label) {
                        onAnimalOpened(animal.id)
                        onBioViewChange(SlBioView.Detail(animal.id))
                    }
                }
            }
        }
        is SlBioView.Detail -> {
            SlAnimalDetail(
                animalId = view.animalId,
                onBack = {
                    onBioViewChange(
                        view.animalId?.let { id ->
                            val category = ScienceLabData.categories.first { id in it.animalIds }
                            SlBioView.Grid(category.id)
                        } ?: SlBioView.Categories,
                    )
                },
                onLog = onLog,
            )
        }
    }
}

@Composable
private fun <T> SlGrid(items: List<T>, content: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item -> Box(modifier = Modifier.weight(1f)) { content(item) } }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SlGridCard(icon: String, label: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 14.dp, start = 10.dp, end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(icon, fontSize = 26.sp)
            Spacer(modifier = Modifier.size(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun SlAnimalDetail(animalId: String?, onBack: () -> Unit, onLog: (String) -> Unit) {
    val animal = animalId?.let { ScienceLabData.animals[it] }
    val bodyKey = animal?.bodyKey ?: "human"
    val bodyImage = ScienceLabData.bodyImages[bodyKey]

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        item { TextButton(onClick = onBack) { Text(stringResource(R.string.sl_back_to_categories)) } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(animal?.icon ?: "🧍", fontSize = 30.sp)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    stringResource(animal?.nameRes ?: R.string.sl_cat_human),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
        }
        if (animal != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        animal.factRes.forEach { factRes ->
                            Row {
                                Text("• ", style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(factRes), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
        if (bodyImage != null) {
            item {
                Text(stringResource(R.string.sl_body_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.size(8.dp))
                SlBodyImageWithHotspots(bodyImage = bodyImage, onLog = onLog)
                bodyImage.creditRes?.let {
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SlBodyImageWithHotspots(bodyImage: SlBodyImage, onLog: (String) -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var imageSize by remember(bodyImage.key) { mutableStateOf(IntSize.Zero) }
    var selectedPart by remember(bodyImage.key) { mutableStateOf<SlBodyPart?>(null) }

    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = bodyImage.imageModel,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().onSizeChanged { imageSize = it },
        )
        if (imageSize.width > 0) {
            bodyImage.hotspots.forEach { hotspot ->
                val cx = with(density) { (imageSize.width * hotspot.x / 100f).toDp() }
                val cy = with(density) { (imageSize.height * hotspot.y / 100f).toDp() }
                val isSelected = selectedPart == hotspot.part
                Box(
                    modifier = Modifier
                        .offset(x = cx - 13.dp, y = cy - 13.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                        )
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable {
                            selectedPart = hotspot.part
                            val prefix = context.getString(R.string.sl_log_part_prefix)
                            onLog("$prefix: ${context.getString(hotspot.part.nameRes)}")
                        },
                )
            }
        }
    }

    selectedPart?.let { part ->
        Spacer(modifier = Modifier.size(10.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(stringResource(part.nameRes), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.size(4.dp))
                Text(stringResource(part.descRes), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ---------------------------------------------------------------------
// المساعد الذكيّ - محتوى الـ Bottom Sheet
// ---------------------------------------------------------------------

@Composable
private fun SlChatSheetContent(
    messages: List<AiMessage>,
    sending: Boolean,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val listState = rememberLazyListState()
    Column(modifier = Modifier.fillMaxWidth().height(480.dp)) {
        Text(
            stringResource(R.string.sl_chat_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)) {
            items(messages) { message -> SlChatBubble(message) }
            if (sending) { item { SlChatTypingBubble() } }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.sl_chat_placeholder)) },
            )
            Spacer(modifier = Modifier.size(4.dp))
            IconButton(onClick = onSend, enabled = input.isNotBlank() && !sending) {
                Icon(Icons.Filled.Send, contentDescription = stringResource(R.string.ai_send))
            }
        }
    }
}

@Composable
private fun SlChatBubble(message: AiMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 280.dp),
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
private fun SlChatTypingBubble() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Start) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
            CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(16.dp), strokeWidth = 2.dp)
        }
    }
}
