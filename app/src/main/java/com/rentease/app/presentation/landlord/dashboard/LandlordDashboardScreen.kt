package com.rentease.app.presentation.landlord.dashboard

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
import androidx.compose.material3.Button
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
import com.rentease.app.presentation.common.HeroBanner
import androidx.compose.ui.draw.clip
import com.rentease.app.domain.model.VerificationStatus

@Composable
fun LandlordDashboardScreen(
    onNavigateToProperties: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToTenants: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onLogout: () -> Unit,
    viewModel: LandlordDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.landlord_dashboard_title)) },
                actions = {
                    TextButton(onClick = onNavigateToSettings) { Text(stringResource(R.string.settings_title)) }
                    TextButton(onClick = onLogout) { Text(stringResource(R.string.logout_button)) }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            HeroBanner(
                title = stringResource(R.string.landlord_dashboard_title),
                height = 130.dp,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(12.dp))

            uiState.profile?.let { profile ->
                VerificationBanner(profile.verificationStatus)
                if (profile.verificationStatus != VerificationStatus.VERIFIED || !profile.onboardingComplete) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onNavigateToOnboarding, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.landlord_open_onboarding))
                    }
                }
            }

            uiState.progress?.let { progress ->
                Spacer(Modifier.height(12.dp))
                VerificationProgressCard(progress)
            }

            Spacer(Modifier.height(16.dp))

            uiState.analytics?.let { analytics ->
                Text(stringResource(R.string.overview_heading), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.stat_properties), analytics.totalProperties.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.stat_occupancy), "${analytics.occupancyRatePercent.toInt()}%", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.stat_contracted_income), "R${analytics.contractedMonthlyIncome.toInt()}", Modifier.weight(1f))
                    StatCard(stringResource(R.string.stat_pending_requests), analytics.pendingBookingRequests.toString(), Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.manage_heading), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Built here (a composable scope): stringResource cannot be called inside the grid's non-composable content lambda.
            val tiles = listOf(
                        stringResource(R.string.nav_properties_rooms) to onNavigateToProperties,
                        stringResource(R.string.nav_booking_requests) to onNavigateToBookings,
                        stringResource(R.string.nav_tenants) to onNavigateToTenants,
                        stringResource(R.string.nav_maintenance) to onNavigateToMaintenance,
                        stringResource(R.string.nav_messages) to onNavigateToMessages,
                        stringResource(R.string.nav_notifications) to onNavigateToNotifications,
                        stringResource(R.string.nav_analytics) to onNavigateToAnalytics,
                        stringResource(R.string.support_title) to onNavigateToSupport
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(420.dp)
            ) {
                items(tiles) { (label, onClick) ->
                    Card(
                        modifier = Modifier.padding(6.dp).fillMaxWidth().height(90.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
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
private fun VerificationBanner(status: VerificationStatus) {
    val (message, isPositive) = when (status) {
        VerificationStatus.VERIFIED -> stringResource(R.string.verification_banner_verified) to true
        VerificationStatus.SUBMITTED -> stringResource(R.string.verification_banner_submitted) to false
        VerificationStatus.DOCUMENTS_REQUIRED -> stringResource(R.string.verification_banner_documents_required) to false
        VerificationStatus.REJECTED -> stringResource(R.string.verification_banner_rejected) to false
        VerificationStatus.PENDING -> stringResource(R.string.verification_banner_pending) to false
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyMedium
        )
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
