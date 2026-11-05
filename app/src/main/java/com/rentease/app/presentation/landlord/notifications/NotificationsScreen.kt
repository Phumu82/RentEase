package com.rentease.app.presentation.landlord.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.AppNotification

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.notifications_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.notifications.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_notifications_yet)) }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                items(uiState.notifications, key = { it.id }) { notification ->
                    NotificationRow(notification, onClick = { viewModel.markRead(notification) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    val isUnread = notification.readAt == null
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(notification.title, style = MaterialTheme.typography.titleMedium)
            if (notification.body.isNotBlank()) Text(notification.body, style = MaterialTheme.typography.bodyMedium)
            Text(notification.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
