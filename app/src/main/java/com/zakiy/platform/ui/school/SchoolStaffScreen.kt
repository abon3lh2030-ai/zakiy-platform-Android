package com.zakiy.platform.ui.school

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.AddStaffRequest
import com.zakiy.platform.ui.components.CredentialBox
import kotlinx.coroutines.launch

/** المعلمون أو إداريو المدرسة - نفس الشاشة تُعاد استخدامها للدورين (فرق
 * الـ endpoint بس). إضافة/حذف إداري محجوزة على مدير المدرسة (school_admin)
 * نفسه بس - نفس القيد المطبّق بالباك إند. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolStaffScreen(isTeachers: Boolean, authManager: AuthManager, onBack: () -> Unit) {
    val role by authManager.role.collectAsStateWithLifecycle()
    val canManage = isTeachers || role == "school_admin"
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var credential by remember { mutableStateOf<Pair<String, String>?>(null) }
    var members by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // userId to username
    val scope = rememberCoroutineScope()
    val genericError = stringResource(R.string.error_generic)

    suspend fun load() {
        members = if (isTeachers) {
            runCatching { NetworkModule.backendApi.schoolTeachers().teachers.map { it.userId to it.username } }.getOrDefault(emptyList())
        } else {
            runCatching { NetworkModule.backendApi.schoolAdministration().administration.map { it.userId to it.username } }.getOrDefault(emptyList())
        }
    }
    LaunchedEffect(isTeachers) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isTeachers) R.string.tab_teachers else R.string.admin_staff_heading)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (canManage) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.username_label)) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.email_label)) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val result = runCatching {
                                if (isTeachers) NetworkModule.backendApi.schoolAddTeacher(AddStaffRequest(name.trim(), email.trim()))
                                else NetworkModule.backendApi.schoolAddAdministration(AddStaffRequest(name.trim(), email.trim()))
                            }.getOrNull()
                            if (result != null) {
                                credential = result.email to result.password
                                name = ""; email = ""
                                load()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(if (isTeachers) R.string.btn_add_teacher else R.string.btn_add_admin_staff)) }
                credential?.let { CredentialBox("✅", it.first, it.second) }
                Spacer(modifier = Modifier.size(12.dp))
            }

            LazyColumn {
                items(members, key = { it.first }) { (userId, username) ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(username, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = {
                                scope.launch {
                                    val reset = runCatching { NetworkModule.backendApi.schoolResetAccountPassword(userId) }.getOrNull()
                                    if (reset != null) credential = reset.identifier to reset.password
                                }
                            }) { Text(stringResource(R.string.btn_reset_password)) }
                            if (canManage) {
                                Spacer(modifier = Modifier.size(6.dp))
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        runCatching { NetworkModule.backendApi.schoolDeleteAccount(userId) }
                                        load()
                                    }
                                }) { Text(stringResource(R.string.delete)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
