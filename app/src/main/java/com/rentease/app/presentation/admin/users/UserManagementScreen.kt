package com.rentease.app.presentation.admin.users

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.AdminUserItem
import com.rentease.app.presentation.common.label

@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.user_management_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.users.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_users_found)) }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                items(uiState.users, key = { it.id }) { user ->
                    UserRow(user, onToggleSuspended = { viewModel.toggleSuspended(user) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: AdminUserItem, onToggleSuspended: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                Text("${user.email ?: "—"} · " + user.role.label(), style = MaterialTheme.typography.bodyMedium)
            }
            Text(if (user.isSuspended) stringResource(R.string.suspended_label) else stringResource(R.string.active_label), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = !user.isSuspended, onCheckedChange = { onToggleSuspended() })
        }
    }
}
