package com.rentease.app.presentation.admin.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun AdminDashboardScreen(
    onNavigateToVerifications: () -> Unit,
    onNavigateToModeration: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToAuditLog: () -> Unit,
    onNavigateToComplaints: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_dashboard_title)) },
                actions = {
                    TextButton(onClick = onNavigateToSettings) { Text(stringResource(R.string.settings_title)) }
                    TextButton(onClick = onLogout) { Text(stringResource(R.string.logout_button)) }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }

            uiState.stats?.let { stats ->
                Text(stringResource(R.string.platform_overview_heading), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.stat_pending_verifications), stats.pendingLandlordVerifications.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.stat_pending_listings), stats.pendingPropertyModerations.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.stat_students), stats.totalStudents.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.stat_landlords), stats.totalLandlords.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                StatCard(stringResource(R.string.stat_suspended_users), stats.suspendedUsers.toString(), Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.manage_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Built here (a composable scope): stringResource cannot be called inside the grid's non-composable content lambda.
            val tiles = listOf(
                        stringResource(R.string.nav_landlord_verification) to onNavigateToVerifications,
                        stringResource(R.string.nav_property_moderation) to onNavigateToModeration,
                        stringResource(R.string.nav_user_management) to onNavigateToUsers,
                        stringResource(R.string.nav_booking_oversight) to onNavigateToBookings,
                        stringResource(R.string.nav_audit_log) to onNavigateToAuditLog,
                        stringResource(R.string.admin_complaints_title) to onNavigateToComplaints
            )
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().height(280.dp)) {
                items(tiles) { (label, onClick) ->
                    Card(modifier = Modifier.padding(6.dp).fillMaxWidth().height(90.dp), shape = RoundedCornerShape(12.dp)) {
                        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                            TextButton(onClick = onClick) { Text(label, style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
