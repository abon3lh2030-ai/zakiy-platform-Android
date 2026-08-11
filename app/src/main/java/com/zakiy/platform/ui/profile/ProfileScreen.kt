package com.zakiy.platform.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.ProfileResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(userId: String?, authManager: AuthManager, onBack: () -> Unit) {
    val myUserId by authManager.userId.collectAsStateWithLifecycle()
    var profile by remember { mutableStateOf<ProfileResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val targetId = userId ?: myUserId

    LaunchedEffect(targetId) {
        if (targetId != null) {
            profile = runCatching { NetworkModule.backendApi.profile(targetId) }.getOrNull()
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_profile)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (profile != null) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(profile!!.username, style = MaterialTheme.typography.headlineSmall)
                    profile!!.schoolName?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    profile!!.bio?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    profile!!.performance?.let {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                        Text("${stringResource(R.string.stat_attempts)}: ${it.attemptsCount}")
                        Text("${stringResource(R.string.stat_avg)}: ${it.avgScore}%")
                    }
                }
            }
        }
    }
}
