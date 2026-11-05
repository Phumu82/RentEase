package com.rentease.app.presentation.info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.PropertySummary
import com.rentease.app.domain.model.University
import com.rentease.app.domain.model.UniversityDirectory
import com.rentease.app.presentation.common.PropertyPhoto

@Composable
private fun UniversityStats(uni: University) {
    val place = stringResource(R.string.uni_place, uni.city, uni.province)
    Text(place, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (uni.students > 0) {
        Text(stringResource(R.string.uni_students_count, uni.students), style = MaterialTheme.typography.bodySmall)
    }
    if (uni.averageRent > 0) {
        Text(
            stringResource(R.string.uni_average_rent, uni.averageRent.toInt()),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun LoadFailed(message: String, onRetry: () -> Unit) {
    Text(message, color = MaterialTheme.colorScheme.error)
    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry_button)) }
}

@Composable
fun UniversitiesScreen(
    onBack: () -> Unit,
    onOpen: (University) -> Unit,
    viewModel: UniversitiesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val shown = UniversityDirectory.filter(state.universities, state.keyword)
    InfoScaffold(R.string.pub_universities, onBack) {
        Text(stringResource(R.string.uni_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.uni_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = state.keyword,
            onValueChange = viewModel::setKeyword,
            label = { Text(stringResource(R.string.uni_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> LoadFailed(state.error.orEmpty(), viewModel::load)
            shown.isEmpty() -> Text(stringResource(R.string.uni_none_found))
            else -> shown.forEach { uni ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(uni) }) {
                    PropertyPhoto(
                        url = null,
                        contentDescription = uni.name,
                        fallbackKey = uni.id,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        showSampleLabel = false
                    )
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(uni.name, style = MaterialTheme.typography.titleMedium)
                        UniversityStats(uni)
                    }
                }
            }
        }
    }
}

@Composable
fun UniversityDetailScreen(
    onBack: () -> Unit,
    onBrowseListings: (University) -> Unit,
    onOpenProperty: (slug: String) -> Unit,
    viewModel: UniversityDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    InfoScaffold(R.string.pub_universities, onBack) {
        val uni = state.university
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> LoadFailed(state.error.orEmpty(), viewModel::load)
            state.notFound || uni == null -> Text(stringResource(R.string.uni_not_found), style = MaterialTheme.typography.titleMedium)
            else -> {
                Text(stringResource(R.string.uni_accommodation_near, uni.name), style = MaterialTheme.typography.headlineSmall)
                UniversityStats(uni)
                Button(onClick = { onBrowseListings(uni) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.uni_browse_near, uni.shortName))
                }
                Text(stringResource(R.string.uni_available_now), style = MaterialTheme.typography.titleLarge)
                if (state.nearby.isEmpty()) {
                    Text(stringResource(R.string.uni_no_listings))
                } else {
                    state.nearby.forEach { NearbyCard(it) { onOpenProperty(it.slug) } }
                }
            }
        }
    }
}

@Composable
private fun NearbyCard(property: PropertySummary, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(property.title, style = MaterialTheme.typography.titleMedium)
                Text(property.city, style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.price_per_month, property.pricePerMonth.toInt()), style = MaterialTheme.typography.titleSmall)
        }
    }
}
