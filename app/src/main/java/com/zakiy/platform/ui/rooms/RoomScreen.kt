package com.zakiy.platform.ui.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.SocketManager
import com.zakiy.platform.network.TokenHolder
import org.json.JSONObject
import java.util.UUID

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

    DisposableEffect(roomCode) {
        SocketManager.connectIfNeeded()

        val onRoomState = io.socket.emitter.Emitter.Listener { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@Listener
            isHost = data.optBoolean("is_host", false)
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

        SocketManager.on("room_state", onRoomState)
        SocketManager.on("join_error", onJoinError)
        SocketManager.on("chat_message", onChatMessage)
        SocketManager.on("leaderboard_update", onLeaderboard)

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
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(roomCode) },
                navigationIcon = { IconButton(onClick = onLeave) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
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
            if (isHost && roomType in listOf("quiz", "classroom")) {
                Button(
                    onClick = { SocketManager.emit("start_quiz", JSONObject().put("room_code", roomCode)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.btn_start_quiz_for_all)) }
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
}
