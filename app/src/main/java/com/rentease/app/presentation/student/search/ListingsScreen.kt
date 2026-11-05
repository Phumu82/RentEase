package com.rentease.app.presentation.student.search

import com.rentease.app.presentation.common.ConfigWarningBanner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rentease.app.presentation.common.FeaturedPropertyCard
import com.rentease.app.domain.model.AmenityCatalog
import com.rentease.app.domain.model.PropertyComparison
import com.rentease.app.domain.model.PropertySortOption
import com.rentease.app.domain.model.ProvinceCatalog
import com.rentease.app.domain.model.SearchFilters
import com.rentease.app.domain.model.University

@Composable
fun ListingsScreen(
    onPropertyClick: (slug: String) -> Unit,
    onBack: (() -> Unit)? = null,
    onCompare: (slugs: List<String>) -> Unit,
    onLogout: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSignInRequired: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Slugs ticked for side-by-side comparison (2-3 listings).
    val compareSelection = remember { mutableStateOf(listOf<String>()) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (compareSelection.value.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (compareSelection.value.size >= PropertyComparison.MAX_ITEMS)
                            stringResource(R.string.compare_max_reached)
                        else stringResource(R.string.compare_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { compareSelection.value = emptyList() }) {
                        Text(stringResource(R.string.compare_clear))
                    }
                    Button(
                        onClick = { onCompare(compareSelection.value) },
                        enabled = PropertyComparison.canCompare(compareSelection.value)
                    ) { Text(stringResource(R.string.compare_selected_count, compareSelection.value.size)) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = if (onBack == null) 16.dp else 4.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button))
                    }
                }
                Text(
                    stringResource(R.string.find_accommodation_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (viewModel.isGuest) {
                    TextButton(onClick = onSignInRequired) { Text(stringResource(R.string.sign_in_button)) }
                } else {
                    AccountMenu(
                        onNavigateToSaved = onNavigateToSaved,
                        onNavigateToBookings = onNavigateToBookings,
                        onNavigateToMaintenance = onNavigateToMaintenance,
                        onNavigateToMessages = onNavigateToMessages,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToSettings = onNavigateToSettings,
                        onLogout = onLogout
                    )
                }
            }
            if (viewModel.isGuest) {
                Text(
                    stringResource(R.string.guest_banner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            OutlinedTextField(
                value = uiState.filters.keyword,
                onValueChange = { keyword -> viewModel.updateFilters(uiState.filters.copy(keyword = keyword)) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            var filtersExpanded by remember { mutableStateOf(false) }
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filtersExpanded,
                        onClick = { filtersExpanded = !filtersExpanded },
                        label = { Text(stringResource(R.string.filters_button_label)) },
                        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                item { SortChip(selected = uiState.sort, onSelect = { viewModel.updateSort(it) }) }
                item {
                    FilterChip(
                        selected = uiState.filters.availableOnly,
                        onClick = { viewModel.updateFilters(uiState.filters.copy(availableOnly = !uiState.filters.availableOnly)) },
                        label = { Text(stringResource(R.string.filter_available_only)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filters.furnishedOnly,
                        onClick = { viewModel.updateFilters(uiState.filters.copy(furnishedOnly = !uiState.filters.furnishedOnly)) },
                        label = { Text(stringResource(R.string.filter_furnished_only)) }
                    )
                }
            }

            if (filtersExpanded) {
                FilterPanel(
                    filters = uiState.filters,
                    universities = uiState.universities,
                    onFiltersChange = { viewModel.updateFilters(it) }
                )
            }

            Spacer(Modifier.height(8.dp))

            ConfigWarningBanner(Modifier.padding(horizontal = 16.dp))
            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }

            if (uiState.isLoading && uiState.results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_listings_match), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(uiState.results, key = { it.id }) { property ->
                        FeaturedPropertyCard(
                            property = property,
                            onClick = { onPropertyClick(property.slug) },
                            onFavouriteToggle = { if (viewModel.isGuest) onSignInRequired() else viewModel.toggleFavourite(property) },
                            compareSelected = property.slug in compareSelection.value,
                            onCompareToggle = {
                                compareSelection.value =
                                    PropertyComparison.toggleSelection(compareSelection.value, property.slug)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    item {
                        if (uiState.canLoadMore) {
                            TextButton(
                                onClick = { viewModel.loadNextPage() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (uiState.isLoading) stringResource(R.string.loading) else stringResource(R.string.load_more_button)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(selected: PropertySortOption, onSelect: (PropertySortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = false,
            onClick = { expanded = true },
            label = { Text(sortOptionLabel(selected)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PropertySortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(sortOptionLabel(option)) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

/** The signed-in shortcuts (saved, bookings, ...) tucked into one menu so the list header stays clean. */
@Composable
private fun AccountMenu(
    onNavigateToSaved: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                R.string.saved_nav_label to onNavigateToSaved,
                R.string.bookings_nav_label to onNavigateToBookings,
                R.string.student_maintenance_title to onNavigateToMaintenance,
                R.string.messages_title to onNavigateToMessages,
                R.string.notifications_title to onNavigateToNotifications,
                R.string.settings_title to onNavigateToSettings,
                R.string.logout_button to onLogout
            ).forEach { (label, action) ->
                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { expanded = false; action() })
            }
        }
    }
}

@Composable
private fun sortOptionLabel(option: PropertySortOption): String = when (option) {
    PropertySortOption.NEWEST -> stringResource(R.string.sort_newest)
    PropertySortOption.PRICE_ASC -> stringResource(R.string.sort_price_asc)
    PropertySortOption.PRICE_DESC -> stringResource(R.string.sort_price_desc)
    PropertySortOption.RATING -> stringResource(R.string.sort_rating)
    PropertySortOption.DISTANCE -> stringResource(R.string.sort_distance)
}

/** University/province/max-rent/furnished/amenities filters — mirrors the web
 *  app's filter panel (`listings.index.tsx`) exactly, including the maximum
 *  rent slider's 2000..12000 range in steps of 500. */
@Composable
private fun FilterPanel(
    filters: SearchFilters,
    universities: List<University>,
    onFiltersChange: (SearchFilters) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            // University
            var universityMenuOpen by remember { mutableStateOf(false) }
            Box {
                val universityLabel = universities.firstOrNull { it.id == filters.universityId }?.name
                    ?: stringResource(R.string.filter_any_university)
                OutlinedButton(onClick = { universityMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(universityLabel)
                }
                DropdownMenu(expanded = universityMenuOpen, onDismissRequest = { universityMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_any_university)) },
                        onClick = { onFiltersChange(filters.copy(universityId = null)); universityMenuOpen = false }
                    )
                    universities.forEach { university ->
                        DropdownMenuItem(
                            text = { Text(university.name) },
                            onClick = { onFiltersChange(filters.copy(universityId = university.id)); universityMenuOpen = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Province
            var provinceMenuOpen by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { provinceMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(filters.province ?: stringResource(R.string.filter_any_province))
                }
                DropdownMenu(expanded = provinceMenuOpen, onDismissRequest = { provinceMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_any_province)) },
                        onClick = { onFiltersChange(filters.copy(province = null)); provinceMenuOpen = false }
                    )
                    ProvinceCatalog.ALL.forEach { province ->
                        DropdownMenuItem(
                            text = { Text(province) },
                            onClick = { onFiltersChange(filters.copy(province = province)); provinceMenuOpen = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Maximum rent
            val maxRent = filters.maxRent ?: 12000.0
            Text(stringResource(R.string.filter_max_rent_label, maxRent.toInt()))
            Slider(
                value = maxRent.toFloat(),
                onValueChange = { onFiltersChange(filters.copy(maxRent = it.toDouble())) },
                valueRange = 2000f..12000f,
                steps = 19 // (12000-2000)/500 - 1, matching the web app's step=500
            )

            Spacer(Modifier.height(4.dp))

            // Furnished only
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = filters.furnishedOnly,
                    onCheckedChange = { onFiltersChange(filters.copy(furnishedOnly = it)) }
                )
                Text(stringResource(R.string.filter_furnished_only))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = filters.availableOnly,
                    onCheckedChange = { onFiltersChange(filters.copy(availableOnly = it)) }
                )
                Text(stringResource(R.string.filter_available_only))
            }

            Spacer(Modifier.height(8.dp))

            // Amenities
            Text(stringResource(R.string.filter_amenities_label), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            LazyRow {
                items(AmenityCatalog.ALL) { amenity ->
                    FilterChip(
                        selected = amenity in filters.amenities,
                        onClick = {
                            val updated = if (amenity in filters.amenities) {
                                filters.amenities - amenity
                            } else {
                                filters.amenities + amenity
                            }
                            onFiltersChange(filters.copy(amenities = updated))
                        },
                        label = { Text(amenity) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onFiltersChange(SearchFilters()) }) {
                Text(stringResource(R.string.filter_clear_all))
            }
        }
    }
}
