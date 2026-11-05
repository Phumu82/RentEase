package com.rentease.app.presentation.student.compare

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.AmenityCatalog
import com.rentease.app.domain.model.PropertyComparison
import com.rentease.app.domain.model.PropertyDetail

/** Side-by-side comparison of 2-3 listings; the best value in each row is highlighted. */
@Composable
fun ComparisonScreen(
    onBack: () -> Unit,
    onPropertyClick: (String) -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compare_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button))
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.items.size < PropertyComparison.MIN_ITEMS -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.error ?: stringResource(R.string.compare_need_two), style = MaterialTheme.typography.bodyLarge)
            }
            else -> ComparisonTable(uiState.items, onPropertyClick, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ComparisonTable(
    items: List<PropertyDetail>,
    onPropertyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = PropertyComparison.summarize(items)
    val yes = stringResource(R.string.compare_yes)
    val no = stringResource(R.string.compare_no)

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        summary.lowestRent?.let { SummaryLine(stringResource(R.string.compare_lowest_rent, it.summary.title)) }
        summary.closest?.let { SummaryLine(stringResource(R.string.compare_closest, it.summary.title)) }
        summary.topRated?.let { SummaryLine(stringResource(R.string.compare_top_rated, it.summary.title)) }

        // Header: listing titles (tap to open the listing).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                Text(
                    item.summary.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).clickable { onPropertyClick(item.summary.slug) }
                )
            }
        }
        HorizontalDivider()

        CompareRow(
            label = stringResource(R.string.compare_row_rent),
            values = items.map { stringResource(R.string.price_per_month, it.summary.pricePerMonth.toInt()) },
            best = PropertyComparison.bestIndices(items.map { it.summary.pricePerMonth }, higherIsBetter = false)
        )
        CompareRow(
            label = stringResource(R.string.compare_row_deposit),
            values = items.map { "R${it.deposit.toInt()}" },
            best = PropertyComparison.bestIndices(items.map { it.deposit }, higherIsBetter = false)
        )
        CompareRow(
            label = stringResource(R.string.compare_row_distance),
            values = items.map { String.format("%.1f km", it.summary.distanceKm) },
            best = PropertyComparison.bestIndices(items.map { it.summary.distanceKm }, higherIsBetter = false)
        )
        CompareRow(
            label = stringResource(R.string.bedrooms_label),
            values = items.map { it.summary.bedrooms.toString() },
            best = PropertyComparison.bestIndices(items.map { it.summary.bedrooms.toDouble() }, higherIsBetter = true)
        )
        CompareRow(
            label = stringResource(R.string.bathrooms_label),
            values = items.map { it.summary.bathrooms.toString() },
            best = PropertyComparison.bestIndices(items.map { it.summary.bathrooms.toDouble() }, higherIsBetter = true)
        )
        CompareRow(
            label = stringResource(R.string.compare_row_rooms_available),
            values = items.map { it.summary.roomsAvailable.toString() },
            best = PropertyComparison.bestIndices(items.map { it.summary.roomsAvailable.toDouble() }, higherIsBetter = true)
        )
        CompareRow(
            label = stringResource(R.string.compare_row_rating),
            values = items.map { item ->
                PropertyComparison.ratingOrNull(item.summary)?.let { String.format("%.1f (%d)", it, item.summary.reviewCount) } ?: "—"
            },
            best = PropertyComparison.bestIndices(items.map { PropertyComparison.ratingOrNull(it.summary) }, higherIsBetter = true)
        )
        CompareRow(
            label = stringResource(R.string.furnished_label),
            values = items.map { if (it.summary.furnished) yes else no },
            best = emptySet()
        )
        CompareRow(
            label = stringResource(R.string.verification_status_verified),
            values = items.map { if (it.summary.verified) yes else no },
            best = emptySet()
        )

        HorizontalDivider()
        Text(stringResource(R.string.amenities_heading), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        // Every catalogue amenity + any custom amenity present on at least one listing.
        val allAmenities = (AmenityCatalog.ALL + items.flatMap { it.amenities }).distinct()
        allAmenities.forEach { amenity ->
            val has = items.map { item -> item.amenities.any { it.equals(amenity, ignoreCase = true) } }
            if (has.any { it }) {
                CompareRow(
                    label = amenity,
                    values = has.map { if (it) "✓" else "—" },
                    best = emptySet()
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
}

@Composable
private fun CompareRow(label: String, values: List<String>, best: Set<Int>) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEachIndexed { index, value ->
                val isBest = index in best
                Text(
                    text = if (isBest) "★ $value" else value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                    color = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
