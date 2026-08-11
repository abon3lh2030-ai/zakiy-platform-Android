package com.zakiy.platform.network

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** اتصال Socket.IO وحيد مشترك بكل التطبيق - نفس بروتوكول غرف الدراسة
 * اللحظية بالباك إند بالضبط (نفس أحداث join_room/room_state/board_stroke/
 * chat_message/quiz_started/leaderboard_update...، مطابقة لما بُني به
 * الموقع وiOS). يُستخدم لتنبيهات لحظية عامة (register_user) ولغرف الدراسة. */
object SocketManager {
    private var socket: Socket? = null

    private val _unreadNotifications = MutableStateFlow(0)
    val unreadNotifications: StateFlow<Int> = _unreadNotifications.asStateFlow()

    private val _newNotificationSignal = MutableStateFlow(0L)
    val newNotificationSignal: StateFlow<Long> = _newNotificationSignal.asStateFlow()

    fun connectIfNeeded() {
        if (socket?.connected() == true) return
        val opts = IO.Options.builder().setTransports(arrayOf("websocket")).build()
        socket = IO.socket(java.net.URI.create(ApiConfig.SOCKET_URL), opts)
        socket?.on(Socket.EVENT_CONNECT) {
            TokenHolder.accessToken?.let { token ->
                socket?.emit("register_user", JSONObject().put("token", token))
            }
        }
        socket?.on("new_notification") {
            _newNotificationSignal.value = System.currentTimeMillis()
        }
        socket?.connect()
    }

    fun registerCurrentUser() {
        TokenHolder.accessToken?.let { token ->
            socket?.emit("register_user", JSONObject().put("token", token))
        }
    }

    fun setUnreadCount(count: Int) {
        _unreadNotifications.value = count
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    // ---- غرف الدراسة (يُستخدم بشاشة الغرفة) ----
    fun on(event: String, listener: Emitter.Listener) {
        socket?.on(event, listener)
    }

    fun off(event: String) {
        socket?.off(event)
    }

    fun emit(event: String, data: JSONObject) {
        socket?.emit(event, data)
    }

    fun raw(): Socket? = socket
}
