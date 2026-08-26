package com.zakiy.platform.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.AuthManager
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.AdminUpdateSchoolRequest
import com.zakiy.platform.network.dto.CreateSchoolRequest
import com.zakiy.platform.network.dto.School
import com.zakiy.platform.ui.components.CredentialBox
import com.zakiy.platform.ui.components.DashboardMenuRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(authManager: AuthManager, onOpenAiAssistant: () -> Unit, onOpenMadrasati: () -> Unit) {
    var schools by remember { mutableStateOf<List<School>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var maxAccounts by remember { mutableStateOf("50") }
    var credential by remember { mutableStateOf<Pair<String, String>?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        schools = runCatching { NetworkModule.backendApi.adminSchools().schools }.getOrDefault(emptyList())
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.admin_dash_heading)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DashboardMenuRow(Icons.Filled.SmartToy, Color(0xFF2E8B77), stringResource(R.string.ai_assistant), onOpenAiAssistant)
            DashboardMenuRow(Icons.Filled.OpenInBrowser, Color(0xFF6D4AFF), stringResource(R.string.madrasati_heading), onOpenMadrasati)
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المدرسة") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedTextField(
                    value = adminEmail, onValueChange = { adminEmail = it }, label = { Text("إيميل مدير المدرسة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedTextField(
                    value = maxAccounts, onValueChange = { maxAccounts = it.filter(Char::isDigit) }, label = { Text("الحد الأقصى للحسابات") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val result = runCatching {
                                NetworkModule.backendApi.adminCreateSchool(
                                    CreateSchoolRequest(name.trim(), adminEmail.trim(), maxAccounts.toIntOrNull() ?: 0),
                                )
                            }.getOrNull()
                            if (result != null) {
                                credential = result.schoolAdmin.email to result.schoolAdmin.password
                                name = ""; adminEmail = ""
                                load()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("إنشاء المدرسة") }

                credential?.let { CredentialBox("✅ بيانات دخول مدير المدرسة (تظهر مرة وحدة بس)", it.first, it.second) }
                Spacer(modifier = Modifier.size(12.dp))

                LazyColumn {
                    items(schools, key = { it.id }) { school ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(school.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${school.accountsUsed ?: 0} / ${school.maxAccounts} · ${if (school.isActive) "مفعّلة" else "موقوفة"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Row {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            runCatching {
                                                NetworkModule.backendApi.adminUpdateSchool(school.id, AdminUpdateSchoolRequest(isActive = !school.isActive))
                                            }
                                            load()
                                        }
                                    }) { Text(if (school.isActive) "إيقاف" else "تفعيل") }
                                    Spacer(modifier = Modifier.size(8.dp))
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            val reset = runCatching { NetworkModule.backendApi.adminResetSchoolAdminPassword(school.id) }.getOrNull()
                                            if (reset != null) credential = reset.email to reset.password
                                        }
                                    }) { Text(stringResource(R.string.btn_reset_password)) }
                                    Spacer(modifier = Modifier.size(8.dp))
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            runCatching { NetworkModule.backendApi.adminDeleteSchool(school.id) }
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
}
