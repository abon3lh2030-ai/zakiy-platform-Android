package com.zakiy.platform.ui.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.zakiy.platform.network.dto.CreateRoomRequest
import kotlinx.coroutines.launch

@Composable
fun RoomLobbyScreen(roomType: String, authManager: AuthManager, onEnterRoom: (String, Boolean) -> Unit) {
    val isAuthenticated by authManager.isAuthenticated.collectAsStateWithLifecycle()
    val savedUsername by authManager.username.collectAsStateWithLifecycle()
    var guestName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val genericError = stringResource(R.string.error_generic)
    val title = if (roomType == "classroom") stringResource(R.string.room_type_classroom) else stringResource(R.string.group_room)

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.size(20.dp))

        if (!isAuthenticated) {
            OutlinedTextField(
                value = guestName,
                onValueChange = { guestName = it },
                label = { Text(stringResource(R.string.your_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(12.dp))
        }

        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.size(8.dp))
        }

        Button(
            onClick = {
                isCreating = true
                errorMessage = null
                scope.launch {
                    try {
                        val result = NetworkModule.backendApi.createRoom(CreateRoomRequest(roomType = roomType))
                        onEnterRoom(result.roomCode, true)
                    } catch (e: Exception) {
                        errorMessage = genericError
                    } finally {
                        isCreating = false
                    }
                }
            },
            enabled = !isCreating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isCreating) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.create_room))
        }

        Spacer(modifier = Modifier.size(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.size(24.dp))

        Text(stringResource(R.string.join_by_code), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.size(10.dp))
        OutlinedTextField(
            value = joinCode,
            onValueChange = { joinCode = it.uppercase() },
            label = { Text(stringResource(R.string.room_code_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Button(
            onClick = { if (joinCode.isNotBlank()) onEnterRoom(joinCode.trim(), false) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.btn_join_room)) }
    }
}
