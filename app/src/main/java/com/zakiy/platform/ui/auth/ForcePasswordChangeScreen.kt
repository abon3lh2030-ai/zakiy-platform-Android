package com.zakiy.platform.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import kotlinx.coroutines.launch

/** بوابة صلبة - حساب مؤسسي بكلمة سر مؤقتة لازم يغيّرها قبل أي شي ثاني،
 * بدون أي طريق تخطّي (نفس مبدأ الموقع وiOS بالضبط - ما فيها زر رجوع). */
@Composable
fun ForcePasswordChangeScreen(authManager: AuthManager) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val genericError = stringResource(R.string.error_generic)
    val mismatchError = stringResource(R.string.err_password_mismatch)
    val minLengthError = stringResource(R.string.err_password_min)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.force_pw_heading), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            stringResource(R.string.force_pw_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(24.dp))

        OutlinedTextField(
            value = newPassword, onValueChange = { newPassword = it },
            label = { Text(stringResource(R.string.new_password_placeholder)) },
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
                    newPassword.length < 6 -> errorMessage = minLengthError
                    newPassword != confirmPassword -> errorMessage = mismatchError
                    else -> {
                        isLoading = true
                        scope.launch {
                            val result = authManager.updatePassword(newPassword)
                            if (result.isSuccess) {
                                authManager.completeForcedPasswordChange()
                            } else {
                                errorMessage = genericError
                            }
                            isLoading = false
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.save))
        }
    }
}
