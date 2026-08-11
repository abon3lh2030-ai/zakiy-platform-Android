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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.CreateClassRequest
import com.zakiy.platform.network.dto.SchoolClass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolClassesScreen() {
    var classes by remember { mutableStateOf<List<SchoolClass>>(emptyList()) }
    var newClassName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        classes = runCatching { NetworkModule.backendApi.schoolClasses().classes }.getOrDefault(emptyList())
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_classes)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row {
                OutlinedTextField(
                    value = newClassName, onValueChange = { newClassName = it },
                    label = { Text("اسم الفصل") }, modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedButton(onClick = {
                    val name = newClassName.trim()
                    if (name.isEmpty()) return@OutlinedButton
                    scope.launch {
                        runCatching { NetworkModule.backendApi.schoolCreateClass(CreateClassRequest(name)) }
                        newClassName = ""
                        load()
                    }
                }) { Text(stringResource(R.string.add_book)) }
            }
            Spacer(modifier = Modifier.size(12.dp))
            LazyColumn {
                items(classes, key = { it.id }) { schoolClass ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(schoolClass.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = {
                                scope.launch {
                                    runCatching { NetworkModule.backendApi.schoolDeleteClass(schoolClass.id) }
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
