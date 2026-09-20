package com.zakiy.platform.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.UpdateProfileRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(authManager: AuthManager, onBack: () -> Unit) {
    var bio by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordMessage by remember { mutableStateOf<String?>(null) }
    val passwordTooShort = stringResource(R.string.password_too_short)
    val passwordsMismatch = stringResource(R.string.err_password_mismatch)
    val passwordUpdated = stringResource(R.string.password_updated)
    val currentPasswordWrong = stringResource(R.string.current_password_wrong)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.size(12.dp))
            OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text("School") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.size(20.dp))
            Button(
                onClick = {
                    scope.launch {
                        runCatching {
                            NetworkModule.backendApi.updateProfile(UpdateProfileRequest(bio = bio, schoolName = schoolName))
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
            Spacer(modifier = Modifier.size(28.dp))
            Text(stringResource(R.string.change_password))
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = { Text(stringResource(R.string.current_password)) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text(stringResource(R.string.new_password_placeholder)) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text(stringResource(R.string.confirm_password_label)) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            passwordMessage?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
            Button(
                onClick = {
                    scope.launch {
                        if (newPassword.length < 6 || newPassword != confirmPassword) {
                            passwordMessage = if (newPassword.length < 6) passwordTooShort else passwordsMismatch
                            return@launch
                        }
                        val result = authManager.updatePassword(currentPassword, newPassword)
                        passwordMessage = if (result.isSuccess) passwordUpdated else currentPasswordWrong
                        if (result.isSuccess) { currentPassword = ""; newPassword = ""; confirmPassword = "" }
                    }
                },
                enabled = currentPassword.isNotEmpty() && newPassword.length >= 6 && newPassword == confirmPassword,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.update_password)) }
        }
    }
}
