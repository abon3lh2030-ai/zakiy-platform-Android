package com.zakiy.platform.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.ui.study.StudyFlowState
import com.zakiy.platform.util.uriToMultipart
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun HomeScreen(
    authManager: AuthManager,
    studyState: StudyFlowState,
    onNavigateToText: () -> Unit,
    onNavigateToGroupLobby: () -> Unit,
    onNavigateToClassroomLobby: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val genericError = stringResource(R.string.error_generic)

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isProcessing = true
        errorMessage = null
        scope.launch {
            try {
                val part = uriToMultipart(context, uri, fieldName = "file")
                val contextPart = "solo".toRequestBody("text/plain".toMediaTypeOrNull())
                val uploadResult = NetworkModule.backendApi.upload(part, contextPart)
                val extracted = NetworkModule.backendApi.extract(mapOf("filename" to uploadResult.filename))
                studyState.reset(extracted.text)
                onNavigateToText()
            } catch (e: Exception) {
                errorMessage = genericError
            } finally {
                isProcessing = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.hero_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            stringResource(R.string.hero_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(28.dp))

        Text(stringResource(R.string.mode_select_heading), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.size(12.dp))

        if (isProcessing) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.size(12.dp))
        }
        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.size(12.dp))
        }

        Button(
            onClick = { filePicker.launch("application/pdf") },
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.mode_solo)) }
        Spacer(modifier = Modifier.size(10.dp))

        Button(
            onClick = onNavigateToGroupLobby,
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.mode_room)) }
        Spacer(modifier = Modifier.size(10.dp))

        Button(
            onClick = onNavigateToClassroomLobby,
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.mode_classroom)) }
    }
}
