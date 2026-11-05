package com.rentease.app.presentation.landlord.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R

@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.analytics_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.analytics == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: stringResource(R.string.no_results_generic))
            }
            else -> {
                val a = uiState.analytics!!
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text(
                        stringResource(R.string.analytics_disclaimer),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(stringResource(R.string.stat_properties), a.totalProperties.toString(), Modifier.weight(1f))
                        StatCard(stringResource(R.string.stat_total_rooms), a.totalRoomsAvailable.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(stringResource(R.string.stat_occupied_rooms), a.occupiedRoomsEstimate.toString(), Modifier.weight(1f))
                        StatCard(stringResource(R.string.stat_occupancy_rate), "${a.occupancyRatePercent.toInt()}%", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(stringResource(R.string.stat_contracted_income), "R${a.contractedMonthlyIncome.toInt()}", Modifier.weight(1f))
                        StatCard(stringResource(R.string.stat_pending_requests), a.pendingBookingRequests.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(stringResource(R.string.stat_collected), "R${a.collectedToDate.toInt()}", Modifier.weight(1f))
                        StatCard(stringResource(R.string.stat_open_maintenance), a.openMaintenanceTickets.toString(), Modifier.weight(1f))
                    }

                    if (a.incomeByProperty.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Text(stringResource(R.string.income_by_property_heading), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        a.incomeByProperty.forEach { row ->
                            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(12.dp)) {
                                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(row.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            stringResource(R.string.income_tenants, row.tenants),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(stringResource(R.string.price_per_month, row.monthlyRent.toInt()), style = MaterialTheme.typography.titleSmall)
                                }
                            }
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
