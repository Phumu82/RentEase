package com.rentease.app.presentation.landlord.tenants

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.presentation.common.label

@Composable
fun TenantsScreen(
    onBack: () -> Unit,
    viewModel: TenantsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tenants_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.tenants.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_tenants_yet))
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(uiState.tenants, key = { it.id }) { tenant ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(tenant.studentName, style = MaterialTheme.typography.titleMedium)
                            Text(tenant.propertyTitle + (tenant.roomName?.let { " · " + stringResource(R.string.room_assigned_label, it) } ?: ""), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.lease_summary_label, tenant.moveInDate, tenant.leaseMonths, tenant.monthlyRent.toInt()), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.status_label, tenant.status.label()), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
