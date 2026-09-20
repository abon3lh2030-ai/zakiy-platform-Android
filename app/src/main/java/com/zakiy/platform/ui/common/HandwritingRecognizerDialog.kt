package com.zakiy.platform.ui.common

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zakiy.platform.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException

private const val MAX_HANDWRITING_BYTES = 15 * 1024 * 1024

@Composable
fun HandwritingRecognizerDialog(
    requestContext: String,
    onDismiss: () -> Unit,
    onUseText: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recognizedText by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val language = remember { java.util.Locale.getDefault().language.let { if (it == "ar") "ar" else "en" } }
    val genericError = stringResource(R.string.handwriting_failed)
    val tooLargeError = stringResource(R.string.handwriting_too_large)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "handwriting"
                val bytes = withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes() } }
                    ?: error(genericError)
                if (bytes.size > MAX_HANDWRITING_BYTES) error(tooLargeError)
                selectedName = name
                val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val file = MultipartBody.Part.createFormData("file", name, body)
                val response = NetworkModule.backendApi.recognizeHandwriting(
                    file,
                    requestContext.toRequestBody("text/plain".toMediaTypeOrNull()),
                    language.toRequestBody("text/plain".toMediaTypeOrNull()),
                )
                recognizedText = response.text
            } catch (error: Exception) {
                val backendMessage = if (error is HttpException) {
                    runCatching { JSONObject(error.response()?.errorBody()?.string().orEmpty()).optString("error") }.getOrNull()
                } else null
                errorMessage = backendMessage?.takeIf(String::isNotBlank) ?: error.message ?: genericError
            } finally {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(R.string.handwriting_title)) },
        text = {
            Column {
                Text(stringResource(R.string.handwriting_supported_hint))
                Button(
                    onClick = { picker.launch(arrayOf("image/*", "application/pdf")) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text(stringResource(R.string.handwriting_choose_file)) }
                if (selectedName.isNotBlank()) Text(selectedName, modifier = Modifier.padding(top = 8.dp))
                if (isLoading) CircularProgressIndicator(modifier = Modifier.padding(top = 14.dp))
                if (recognizedText.isNotBlank()) {
                    OutlinedTextField(
                        value = recognizedText,
                        onValueChange = { recognizedText = it },
                        label = { Text(stringResource(R.string.handwriting_review)) },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
                errorMessage?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUseText(recognizedText.trim()) },
                enabled = recognizedText.isNotBlank() && !isLoading,
            ) { Text(stringResource(R.string.handwriting_use)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text(stringResource(R.string.cancel)) }
        },
    )
}
