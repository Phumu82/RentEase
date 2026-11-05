package com.rentease.app.presentation.admin.moderation

import androidx.compose.foundation.layout.Arrangement
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
import coil.compose.AsyncImage
import com.rentease.app.domain.model.PropertyModerationItem

@Composable
fun PropertyModerationScreen(
    onBack: () -> Unit,
    viewModel: PropertyModerationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.property_moderation_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_listings_pending)) }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                items(uiState.items, key = { it.id }) { item ->
                    ModerationRow(item, onApprove = { viewModel.approve(item) }, onReject = { viewModel.reject(item) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ModerationRow(item: PropertyModerationItem, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            item.coverImageUrl?.let { url ->
                AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxWidth().height(140.dp))
            }
            Column(Modifier.padding(12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.listed_by_label, item.landlordName, item.city, item.province), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.price_per_month, item.pricePerMonth.toInt()), style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onReject) { Text(stringResource(R.string.reject_button)) }
                    TextButton(onClick = onApprove) { Text(stringResource(R.string.approve_button)) }
                }
            }
        }
    }
}
