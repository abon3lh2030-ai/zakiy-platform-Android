package com.zakiy.platform.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(authManager: AuthManager, onGoToLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val genericError = stringResource(R.string.error_generic)
    val mismatchError = stringResource(R.string.err_password_mismatch)
    val minLengthError = stringResource(R.string.err_password_min)
    val fillFieldsError = stringResource(R.string.err_fill_fields)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.signup), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.size(24.dp))

        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text(stringResource(R.string.username_label)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.size(12.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text(stringResource(R.string.email_label)) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.size(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.size(12.dp))
        OutlinedTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.confirm_password_label)) },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.size(20.dp))

        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.size(12.dp))
        }

        Button(
            onClick = {
                errorMessage = null
                when {
                    username.isBlank() || email.isBlank() || password.isBlank() -> errorMessage = fillFieldsError
                    password.length < 6 -> errorMessage = minLengthError
                    password != confirmPassword -> errorMessage = mismatchError
                    else -> {
                        isLoading = true
                        scope.launch {
                            val result = authManager.signUp(username.trim(), email.trim(), password)
                            isLoading = false
                            if (result.isFailure) errorMessage = genericError
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.btn_signup))
        }
        Spacer(modifier = Modifier.size(8.dp))
        TextButton(onClick = onGoToLogin) {
            Text(stringResource(R.string.link_go_login))
        }
    }
}
