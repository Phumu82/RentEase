package com.rentease.app.presentation.student.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.rentease.app.presentation.common.PropertyPhoto
import com.rentease.app.domain.model.PropertySummary
import com.rentease.app.domain.repository.SyncState
import com.rentease.app.presentation.theme.RentEaseGold

/**
 * The concrete, demonstrable offline-mode screen (Part 1 §3.9). This is real
 * offline behaviour, not a simulated banner: [SavedListingsViewModel] reads
 * from Room via [com.rentease.app.data.repository.SavedListingsRepositoryImpl],
 * toggling a save while offline writes to the outbox table and is replayed by
 * FavouriteSyncWorker (WorkManager, NetworkType.CONNECTED constraint) once
 * connectivity returns — turn on airplane mode and save/unsave a listing here
 * to see it.
 */
@Composable
fun SavedListingsScreen(
    onPropertyClick: (slug: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SavedListingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.saved_listings_title)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (!uiState.isOnline) {
                OfflineBanner()
            }
            SyncStatusRow(uiState.syncState, onRetry = { viewModel.retrySync() })

            if (uiState.listings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (uiState.isOnline) stringResource(R.string.no_saved_listings) else stringResource(R.string.offline_banner),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    items(uiState.listings, key = { it.id }) { property ->
                        SavedListingRow(
                            property = property,
                            onClick = { onPropertyClick(property.slug) },
                            onUnsave = { viewModel.toggleSaved(property) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.height(0.dp))
        Text(
            "  " + stringResource(R.string.offline_banner),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SyncStatusRow(state: SyncState, onRetry: () -> Unit) {
    when (state) {
        is SyncState.Idle -> Unit
        is SyncState.Pending -> Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Sync, contentDescription = null, tint = RentEaseGold)
            Text(
                "  " + stringResource(R.string.changes_waiting_to_sync, state.count),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        is SyncState.Syncing -> Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
            Text("  " + stringResource(R.string.syncing), style = MaterialTheme.typography.bodyMedium)
        }
        is SyncState.Error -> Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry_button)) }
        }
    }
}

@Composable
private fun SavedListingRow(property: PropertySummary, onClick: () -> Unit, onUnsave: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PropertyPhoto(
                url = property.coverImageUrl, contentDescription = property.title,
                fallbackKey = property.slug,
                modifier = Modifier.height(64.dp).fillMaxWidth(0.3f)
            )
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(property.title, style = MaterialTheme.typography.titleMedium)
                Text("${property.city} · " + stringResource(R.string.price_per_month, property.pricePerMonth.toInt()), style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onUnsave) {
                Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.remove_from_saved))
            }
        }
    }
}
