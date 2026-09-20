package com.zakiy.platform.ui.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.SocketManager
import com.zakiy.platform.network.TokenHolder
import com.zakiy.platform.ui.common.HandwritingRecognizerDialog
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class RoomBoardText(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val fontSize: Float,
    val color: String = "#12315F",
)

private fun JSONObject.toRoomBoardText(): RoomBoardText? {
    if (optString("mode") != "text" || optString("text").isBlank()) return null
    return RoomBoardText(
        id = optString("id").ifBlank { UUID.randomUUID().toString() },
        text = optString("text"),
        x = optDouble("x", 30.0).toFloat(),
        y = optDouble("y", 40.0).toFloat(),
        fontSize = optDouble("fontSize", 20.0).toFloat(),
        color = optString("color", "#12315F"),
    )
}

/** غرفة دراسة لحظية (جماعية أو درس مباشر) - نفس بروتوكول Socket.IO
 * بالباك إند بالضبط (join_room/room_state/chat_message/leaderboard_update).
 * ملاحظة: الصوت الجماعي (WebRTC) والسبورة التفاعلية مو مفعّلين كاملين
 * بهذا الإصدار (أزرار الواجهة جاهزة وترسل الأحداث الصحيحة، بس معالجة
 * WebRTC نفسها تحتاج إكمال واختبار على جهاز حقيقي بـ Android Studio -
 * ما قدرت أبنيه/أختبره بهذي البيئة). الشات والاختبار الجماعي ولوحة
 * المتصدرين شغّالين كاملين. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(roomCode: String, roomType: String, isCreator: Boolean, authManager: AuthManager, onLeave: () -> Unit) {
    val username by authManager.username.collectAsStateWithLifecycle()
    val clientId = remember { UUID.randomUUID().toString() }

    var isHost by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var leaderboard by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var micOn by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }
    var sharedSummary by remember { mutableStateOf("") }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var summarySourceText by remember { mutableStateOf("") }
    var isGeneratingSummary by remember { mutableStateOf(false) }
    var summaryError by remember { mutableStateOf<String?>(null) }
    var boardTexts by remember { mutableStateOf<List<RoomBoardText>>(emptyList()) }
    var selectedBoardTextId by remember { mutableStateOf<String?>(null) }
    var showHandwriting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val genericError = stringResource(R.string.error_generic)
    val language = java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" }

    DisposableEffect(roomCode) {
        SocketManager.connectIfNeeded()

        val onRoomState = io.socket.emitter.Emitter.Listener { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@Listener
            isHost = data.optBoolean("is_host", false)
            sharedSummary = data.optString("shared_summary").takeUnless { it == "null" }.orEmpty()
            val strokes = data.optJSONArray("board_strokes") ?: JSONArray()
            boardTexts = (0 until strokes.length()).mapNotNull { strokes.optJSONObject(it)?.toRoomBoardText() }
        }
        val onJoinError = io.socket.emitter.Emitter.Listener { args ->
            joinError = (args.getOrNull(0) as? JSONObject)?.optString("error")
        }
        val onChatMessage = io.socket.emitter.Emitter.Listener { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@Listener
            val name = data.optString("name")
            val message = data.optString("message")
            messages = messages + (name to message)
        }
        val onLeaderboard = io.socket.emitter.Emitter.Listener { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@Listener
            val arr = data.optJSONArray("leaderboard") ?: return@Listener
            val list = mutableListOf<Pair<String, Int>>()
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                list.add(row.optString("name") to row.optInt("score"))
            }
            leaderboard = list
        }
        val onSummaryShared = io.socket.emitter.Emitter.Listener { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@Listener
            sharedSummary = data.optString("summary")
        }
        val onBoardStroke = io.socket.emitter.Emitter.Listener { args ->
            val stroke = (args.getOrNull(0) as? JSONObject)?.optJSONObject("stroke")?.toRoomBoardText() ?: return@Listener
            boardTexts = boardTexts.filterNot { it.id == stroke.id } + stroke
        }
        val onBoardUpdate = io.socket.emitter.Emitter.Listener { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@Listener
            val id = data.optString("id")
            val patch = data.optJSONObject("patch") ?: return@Listener
            boardTexts = boardTexts.map { item ->
                if (item.id != id) item else item.copy(
                    x = if (patch.has("x")) patch.optDouble("x").toFloat() else item.x,
                    y = if (patch.has("y")) patch.optDouble("y").toFloat() else item.y,
                    fontSize = if (patch.has("fontSize")) patch.optDouble("fontSize").toFloat() else item.fontSize,
                )
            }
        }
        val onBoardClear = io.socket.emitter.Emitter.Listener { boardTexts = emptyList(); selectedBoardTextId = null }

        SocketManager.on("room_state", onRoomState)
        SocketManager.on("join_error", onJoinError)
        SocketManager.on("chat_message", onChatMessage)
        SocketManager.on("leaderboard_update", onLeaderboard)
        SocketManager.on("summary_shared", onSummaryShared)
        SocketManager.on("board_stroke", onBoardStroke)
        SocketManager.on("board_update_stroke", onBoardUpdate)
        SocketManager.on("board_clear", onBoardClear)

        val payload = JSONObject().apply {
            put("room_code", roomCode)
            put("name", username.ifBlank { "طالب" })
            put("client_id", clientId)
            TokenHolder.accessToken?.let { put("token", it) }
        }
        SocketManager.emit("join_room", payload)

        onDispose {
            SocketManager.off("room_state")
            SocketManager.off("join_error")
            SocketManager.off("chat_message")
            SocketManager.off("leaderboard_update")
            SocketManager.off("summary_shared")
            SocketManager.off("board_stroke")
            SocketManager.off("board_update_stroke")
            SocketManager.off("board_clear")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(roomCode) },
                navigationIcon = { IconButton(onClick = onLeave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { micOn = !micOn }) {
                        Icon(if (micOn) Icons.Filled.Mic else Icons.Filled.MicOff, contentDescription = null)
                    }
                    IconButton(onClick = {
                        SocketManager.emit("raise_hand", JSONObject().put("room_code", roomCode))
                    }) { Icon(Icons.Filled.PanTool, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            if (joinError != null) {
                Text(joinError!!, color = MaterialTheme.colorScheme.error)
            }
            if (sharedSummary.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.shared_summary_label), style = MaterialTheme.typography.titleMedium)
                        Text(sharedSummary, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (isHost && roomType in listOf("quiz", "classroom")) {
                Button(
                    onClick = { showSummaryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.prepare_room_summary)) }
                Button(
                    onClick = { SocketManager.emit("start_quiz", JSONObject().put("room_code", roomCode)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.btn_start_quiz_for_all)) }
            }

            if (roomType == "classroom") {
                if (isHost) {
                    Button(onClick = { showHandwriting = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Draw, contentDescription = null)
                        Text(stringResource(R.string.handwriting_board), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Card(modifier = Modifier.fillMaxWidth().height(280.dp).padding(vertical = 8.dp)) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                        boardTexts.forEach { note ->
                            Text(
                                text = note.text,
                                fontSize = note.fontSize.sp,
                                color = runCatching { Color(android.graphics.Color.parseColor(note.color)) }.getOrDefault(Color(0xFF12315F)),
                                modifier = Modifier
                                    .offset { IntOffset(note.x.roundToInt(), note.y.roundToInt()) }
                                    .border(if (selectedBoardTextId == note.id) 1.dp else 0.dp, MaterialTheme.colorScheme.primary)
                                    .padding(3.dp)
                                    .pointerInput(note.id, isHost) {
                                        if (!isHost) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = { selectedBoardTextId = note.id },
                                            onDragEnd = {
                                                boardTexts.firstOrNull { it.id == note.id }?.let { updated ->
                                                    SocketManager.emit("board_update_stroke", JSONObject().put("room_code", roomCode).put("id", updated.id).put("patch", JSONObject().put("x", updated.x).put("y", updated.y)))
                                                }
                                            },
                                        ) { change, drag ->
                                            change.consume()
                                            boardTexts = boardTexts.map { if (it.id == note.id) it.copy(x = (it.x + drag.x).coerceAtLeast(0f), y = (it.y + drag.y).coerceAtLeast(0f)) else it }
                                        }
                                    },
                            )
                        }
                    }
                }
                if (isHost && selectedBoardTextId != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(onClick = { resizeSelectedBoardText(boardTexts, selectedBoardTextId, -4f, roomCode) { boardTexts = it } }) { Text("A−") }
                        Button(onClick = { resizeSelectedBoardText(boardTexts, selectedBoardTextId, 4f, roomCode) { boardTexts = it } }, modifier = Modifier.padding(start = 8.dp)) { Text("A+") }
                    }
                }
            }

            if (leaderboard.isNotEmpty()) {
                Text(stringResource(R.string.leaderboard_label), style = MaterialTheme.typography.titleMedium)
                leaderboard.forEach { (name, score) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name)
                        Text(score.toString())
                    }
                }
            }

            Text(stringResource(R.string.chat_label), style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(messages) { (name, message) ->
                    Text("$name: $message", modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    val text = input.trim()
                    if (text.isEmpty()) return@Button
                    SocketManager.emit(
                        "send_chat_message",
                        JSONObject().put("room_code", roomCode).put("message", text),
                    )
                    input = ""
                }) { Text("→") }
            }
        }
    }

    if (showSummaryDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGeneratingSummary) showSummaryDialog = false },
            title = { Text(stringResource(R.string.prepare_room_summary)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = summarySourceText,
                        onValueChange = { summarySourceText = it },
                        label = { Text(stringResource(R.string.room_summary_source_hint)) },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    summaryError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = summarySourceText.isNotBlank() && !isGeneratingSummary,
                    onClick = {
                        isGeneratingSummary = true
                        summaryError = null
                        scope.launch {
                            try {
                                val result = NetworkModule.backendApi.summarize(
                                    mapOf("text" to summarySourceText.trim(), "lang" to language),
                                )
                                sharedSummary = result.summary
                                SocketManager.emit(
                                    "share_summary",
                                    JSONObject().put("room_code", roomCode).put("summary", result.summary),
                                )
                                showSummaryDialog = false
                            } catch (_: Exception) {
                                summaryError = genericError
                            } finally {
                                isGeneratingSummary = false
                            }
                        }
                    },
                ) {
                    if (isGeneratingSummary) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(R.string.generate_and_share_summary))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSummaryDialog = false }, enabled = !isGeneratingSummary) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showHandwriting) {
        HandwritingRecognizerDialog(
            requestContext = "board",
            onDismiss = { showHandwriting = false },
            onUseText = { text ->
                text.lineSequence().map(String::trim).filter(String::isNotBlank).take(24).forEachIndexed { index, line ->
                    val note = RoomBoardText(UUID.randomUUID().toString(), line.take(180), 24f, (30 + index * 26).toFloat(), 20f)
                    boardTexts = boardTexts + note
                    SocketManager.emit("board_stroke", JSONObject().put("room_code", roomCode).put("stroke", JSONObject().put("id", note.id).put("mode", "text").put("text", note.text).put("x", note.x).put("y", note.y).put("fontSize", note.fontSize).put("color", note.color)))
                }
                showHandwriting = false
            },
        )
    }
}

private fun resizeSelectedBoardText(
    notes: List<RoomBoardText>,
    selectedId: String?,
    delta: Float,
    roomCode: String,
    update: (List<RoomBoardText>) -> Unit,
) {
    val resized = notes.map { if (it.id == selectedId) it.copy(fontSize = (it.fontSize + delta).coerceIn(10f, 72f)) else it }
    update(resized)
    resized.firstOrNull { it.id == selectedId }?.let { note ->
        SocketManager.emit("board_update_stroke", JSONObject().put("room_code", roomCode).put("id", note.id).put("patch", JSONObject().put("fontSize", note.fontSize)))
    }
}
