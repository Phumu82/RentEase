package com.rentease.app.presentation.student.dashboard

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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.presentation.common.HeroBanner
import androidx.compose.ui.draw.clip

/**
 * Student landing screen after login — mirrors the web app's `student.index`
 * dashboard (quick-glance stats + links) rather than dropping the student
 * straight into search. Search itself lives one tap away at
 * [com.rentease.app.presentation.student.search.ListingsScreen], reachable via
 * "Browse properties" below.
 */
@Composable
fun StudentDashboardScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onLogout: () -> Unit,
    viewModel: StudentDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.studentName?.let { stringResource(R.string.student_dashboard_greeting, it.substringBefore(" ")) }
                            ?: stringResource(R.string.student_dashboard_greeting_fallback)
                    )
                },
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

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            if (uiState.onboardingIncomplete) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.onboarding_banner_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.onboarding_banner_body), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onNavigateToOnboarding) { Text(stringResource(R.string.onboarding_banner_action)) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            HeroBanner(
                title = uiState.studentName?.let { stringResource(R.string.student_dashboard_greeting, it.substringBefore(" ")) }
                    ?: stringResource(R.string.student_dashboard_greeting_fallback),
                subtitle = stringResource(R.string.student_dashboard_subtitle),
                height = 150.dp,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(stringResource(R.string.student_dashboard_stat_bookings), uiState.bookingsCount.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.student_dashboard_stat_saved), uiState.savedCount.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.student_dashboard_stat_unread), uiState.unreadNotificationsCount.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            val totalPaidLabel = "R${uiState.totalPaid.toInt()}"
            StatCard(stringResource(R.string.student_dashboard_stat_total_paid), totalPaidLabel, Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))

            // Built here (a composable scope): stringResource cannot be called inside the grid's non-composable content lambda.
            val tiles = listOf(
                        stringResource(R.string.nav_browse_properties) to onNavigateToSearch,
                        stringResource(R.string.saved_nav_label) to onNavigateToSaved,
                        stringResource(R.string.bookings_nav_label) to onNavigateToBookings,
                        stringResource(R.string.nav_maintenance) to onNavigateToMaintenance,
                        stringResource(R.string.nav_messages) to onNavigateToMessages,
                        stringResource(R.string.nav_notifications) to onNavigateToNotifications,
                        stringResource(R.string.support_title) to onNavigateToSupport,
                        stringResource(R.string.nav_payments) to onNavigateToPayments
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(400.dp)
            ) {
                items(tiles) { (label, onClick) ->
                    Card(
                        modifier = Modifier.padding(6.dp).fillMaxWidth().height(90.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                            TextButton(onClick = onClick) { Text(label, style = MaterialTheme.typography.bodyMedium) }
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
