package com.rentease.app.presentation.landlord.maintenance

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.presentation.common.OpenUrlEffect
import com.rentease.app.presentation.common.label
import com.rentease.app.domain.model.MaintenanceTicket

@Composable
fun MaintenanceScreen(
    onBack: () -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    OpenUrlEffect(uiState.photoUrlToOpen, viewModel::photoOpened)

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.maintenance_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.tickets.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_maintenance_tickets)) }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                if (uiState.photoOpenFailed) item { Text(stringResource(R.string.onboarding_error_open_document), color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }
                items(uiState.tickets, key = { it.id }) { ticket ->
                    TicketRow(
                        ticket = ticket,
                        onStatusChange = { status -> viewModel.updateStatus(ticket, status) },
                        onViewPhoto = viewModel::openPhoto
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TicketRow(ticket: MaintenanceTicket, onStatusChange: (MaintenanceStatus) -> Unit, onViewPhoto: (String) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(ticket.title, style = MaterialTheme.typography.titleMedium)
            Text("${ticket.propertyTitle} · ${ticket.studentName}", style = MaterialTheme.typography.bodyMedium)
            Text(ticket.description, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.maintenance_priority_category, ticket.priority.label(), ticket.category), style = MaterialTheme.typography.bodyMedium)

            ticket.photoUrl?.let { path ->
                TextButton(onClick = { onViewPhoto(path) }) { Text(stringResource(R.string.maintenance_view_photo)) }
            }

            Spacer(Modifier.height(8.dp))
            Box {
                TextButton(onClick = { menuOpen = true }) { Text(stringResource(R.string.status_label, ticket.status.label())) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MaintenanceStatus.entries.forEach { status ->
                        DropdownMenuItem(text = { Text(status.label()) }, onClick = { onStatusChange(status); menuOpen = false })
                    }
                }
            }
        }
    }
}

