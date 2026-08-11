package com.zakiy.platform.ui.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
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
import com.zakiy.platform.network.dto.FriendSummary
import com.zakiy.platform.network.dto.UserSearchResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(onOpenProfile: (String) -> Unit) {
    var friends by remember { mutableStateOf<List<FriendSummary>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        friends = runCatching { NetworkModule.backendApi.friends().friends }.getOrDefault(emptyList())
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.friends_heading)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    scope.launch {
                        searchResults = if (it.length >= 2) {
                            runCatching { NetworkModule.backendApi.searchUsers(it).users }.getOrDefault(emptyList())
                        } else emptyList()
                    }
                },
                placeholder = { Text(stringResource(R.string.search_users_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val list = if (query.length >= 2) searchResults.map { it.userId to it.username } else friends.map { it.userId to it.username }
            LazyColumn {
                items(list, key = { it.first }) { (userId, username) ->
                    ListItem(
                        headlineContent = { Text(username) },
                        modifier = Modifier.clickable { onOpenProfile(userId) },
                    )
                }
            }
        }
    }
}
