package com.rentease.app.presentation.student.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.presentation.common.LIST_REFRESH_MS
import com.rentease.app.presentation.common.LiveRefresh
import com.rentease.app.domain.model.ChatMessage
import com.rentease.app.domain.model.ConversationSummary

@Composable
fun StudentConversationListScreen(
    onOpenThread: (conversationId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: StudentConversationListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // The list re-checks less often than an open chat: each check can touch several conversations.
    LiveRefresh(intervalMs = LIST_REFRESH_MS, onTick = viewModel::refresh)

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.messages_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.conversations.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_conversations_yet))
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                items(uiState.conversations, key = { it.id }) { conversation ->
                    ConversationRow(conversation, onClick = { onOpenThread(conversation.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: ConversationSummary, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(conversation.counterpartName, style = MaterialTheme.typography.titleMedium)
                if (conversation.subject.isNotBlank()) Text(conversation.subject, style = MaterialTheme.typography.bodyMedium)
            }
            if (conversation.unreadCount > 0) Badge { Text(conversation.unreadCount.toString()) }
        }
    }
}

@Composable
fun StudentThreadScreen(
    onBack: () -> Unit,
    viewModel: StudentThreadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var draft by remember { mutableStateOf("") }
    val myUserId = uiState.currentUserId
    LiveRefresh(onTick = viewModel::refresh)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.conversation_title)) }) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(stringResource(R.string.message_input_label)) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.sendMessage(draft); draft = "" }, enabled = !uiState.isSending) {
                    Icon(Icons.Filled.Send, contentDescription = stringResource(R.string.send_action))
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(8.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
                items(uiState.messages, key = { it.id }) { message -> MessageBubble(message, isMine = message.senderId == myUserId) }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, isMine: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(4.dp)) {
            Text(message.body, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
