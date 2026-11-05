package com.rentease.app.presentation.admin.complaints

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
import androidx.compose.material3.OutlinedTextField
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
import com.rentease.app.domain.model.Complaint
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.presentation.common.label

@Composable
fun AdminComplaintsScreen(
    onBack: () -> Unit,
    viewModel: AdminComplaintsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.admin_complaints_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.complaints.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_complaints_queue))
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    Text(stringResource(R.string.admin_complaints_description), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                items(uiState.complaints, key = { it.id }) { complaint ->
                    ComplaintRow(
                        complaint = complaint,
                        isUpdating = uiState.updatingId == complaint.id,
                        onDecide = { status, response -> viewModel.decide(complaint, status, response) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ComplaintRow(
    complaint: Complaint,
    isUpdating: Boolean,
    onDecide: (status: MaintenanceStatus, response: String) -> Unit
) {
    var response by remember(complaint.id) { mutableStateOf(complaint.adminResponse.orEmpty()) }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(complaint.subject, style = MaterialTheme.typography.titleMedium)
            Text(
                listOfNotNull(complaint.reporterName, complaint.reporterEmail).joinToString(" \u00b7 ").ifBlank { complaint.category },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(complaint.category, style = MaterialTheme.typography.bodyMedium)
            Text(complaint.description, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.status_label, complaint.status.label()), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = response,
                onValueChange = { response = it },
                label = { Text(stringResource(R.string.admin_response_field_label)) },
                enabled = !isUpdating,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = { onDecide(MaintenanceStatus.IN_PROGRESS, response) }, enabled = !isUpdating) {
                    Text(stringResource(R.string.mark_in_progress_button))
                }
                TextButton(onClick = { onDecide(MaintenanceStatus.RESOLVED, response) }, enabled = !isUpdating) {
                    Text(stringResource(R.string.resolve_button))
                }
                TextButton(onClick = { onDecide(MaintenanceStatus.CLOSED, response) }, enabled = !isUpdating) {
                    Text(stringResource(R.string.close_button))
                }
            }
        }
    }
}
