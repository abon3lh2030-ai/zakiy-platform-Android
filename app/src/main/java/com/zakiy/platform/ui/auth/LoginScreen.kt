package com.zakiy.platform.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(authManager: AuthManager, onGoToSignUp: () -> Unit) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val genericError = stringResource(R.string.error_generic)
    val resetSentMessage = stringResource(R.string.reset_link_sent)
    val schoolResetBlocked = stringResource(R.string.school_reset_blocked)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_dark),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(32.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text(stringResource(R.string.login_identifier_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.hide_password else R.string.show_password,
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(20.dp))

        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
        }
        if (showForgotPassword) {
            TextButton(onClick = {
                resetEmail = identifier.takeIf { it.contains("@") }.orEmpty()
                resetMessage = null
                showResetDialog = true
            }) { Text(stringResource(R.string.forgot_password)) }
        }

        Button(
            onClick = {
                errorMessage = null
                if (identifier.isBlank() || password.isBlank()) {
                    errorMessage = genericError
                    return@Button
                }
                isLoading = true
                scope.launch {
                    val result = authManager.signIn(identifier.trim(), password)
                    isLoading = false
                    if (result.isFailure) {
                        errorMessage = genericError
                        showForgotPassword = true
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_login))
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))

        TextButton(onClick = onGoToSignUp) {
            Text(stringResource(R.string.link_go_signup))
        }
    }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.password_reset_title)) },
            text = {
                Column {
                    OutlinedTextField(value = resetEmail, onValueChange = { resetEmail = it }, label = { Text(stringResource(R.string.email_label)) }, singleLine = true)
                    resetMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = authManager.requestPasswordReset(resetEmail.trim().lowercase())
                        val exception = result.exceptionOrNull()
                        resetMessage = if (result.isSuccess) resetSentMessage
                        else if ((exception as? retrofit2.HttpException)?.code() == 403) schoolResetBlocked
                        else (exception?.message ?: genericError)
                    }
                }, enabled = resetEmail.contains("@")) { Text(stringResource(R.string.send_reset_link)) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
