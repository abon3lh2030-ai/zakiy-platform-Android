package com.zakiy.platform.ui.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.PerformanceResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen() {
    var data by remember { mutableStateOf<PerformanceResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        data = runCatching { NetworkModule.backendApi.performance() }.getOrNull()
        isLoading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_performance)) }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                data == null || data!!.attempts.isEmpty() ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.performance_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                else -> {
                    val attempts = data!!.attempts
                    val avg = attempts.map { if (it.total > 0) it.score * 100 / it.total else 0 }.average().toInt()
                    val best = attempts.maxOf { if (it.total > 0) it.score * 100 / it.total else 0 }
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCard(stringResource(R.string.stat_attempts), attempts.size.toString())
                            StatCard(stringResource(R.string.stat_avg), "$avg%")
                            StatCard(stringResource(R.string.stat_best), "$best%")
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                        Text("${stringResource(R.string.stat_study_minutes)}: ${data!!.totalStudyMinutes}", style = MaterialTheme.typography.bodyMedium)
                        if (data!!.weakTopics.isNotEmpty()) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
                            Text(stringResource(R.string.weak_topics_heading), style = MaterialTheme.typography.titleMedium)
                        }
                        LazyColumn {
                            items(data!!.weakTopics) { topic ->
                                Text("${topic.topic} (${topic.count})", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(modifier = Modifier.padding(4.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
