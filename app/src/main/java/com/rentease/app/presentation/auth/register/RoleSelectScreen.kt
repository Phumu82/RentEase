package com.rentease.app.presentation.auth.register

import com.rentease.app.presentation.common.RentEaseLogo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.University
import com.rentease.app.presentation.common.FeaturedPropertyCard
import com.rentease.app.presentation.common.HeroBanner
import com.rentease.app.presentation.common.PropertyPhoto

/**
 * The app's first screen, laid out like the website's home page: hero photo with search,
 * sign-up / sign-in, then the properties themselves ("Handpicked accommodation near campus")
 * as a scrolling list, and campuses. Admin is never offered here — it stays behind the hidden
 * Routes.ADMIN_ACCESS link.
 */
@Composable
fun RoleSelectScreen(
    onStudentSelected: () -> Unit,
    onLandlordSelected: () -> Unit,
    onSignInInstead: () -> Unit,
    onLearnMore: () -> Unit = {},
    onBrowseListings: () -> Unit = {},
    onOpenListing: (slug: String) -> Unit = {},
    onSearch: (keyword: String) -> Unit = {},
    onOpenUniversity: (University) -> Unit = {},
    onOpenUniversities: () -> Unit = {},
    featuredViewModel: FeaturedListingsViewModel = hiltViewModel()
) {
    val featured by featuredViewModel.uiState.collectAsState()
    var keyword by rememberSaveable { mutableStateOf("") }

    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    HeroBanner(
                        title = stringResource(R.string.home_hero_title),
                        subtitle = stringResource(R.string.home_hero_subtitle),
                        height = 240.dp,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    )
                    RentEaseLogo(
                        size = 44.dp,
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { onSearch(keyword.trim()) }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.home_search_button))
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(keyword.trim()) }),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStudentSelected, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.School, contentDescription = null)
                        Text("  " + stringResource(R.string.role_student))
                    }
                    OutlinedButton(onClick = onLandlordSelected, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.House, contentDescription = null)
                        Text("  " + stringResource(R.string.role_landlord))
                    }
                    TextButton(onClick = onSignInInstead, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.already_have_account))
                    }
                }
            }

            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    Text(stringResource(R.string.home_featured_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.home_featured_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when {
                featured.isLoading -> item {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                featured.failed -> item {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.featured_failed), color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = featuredViewModel::load) { Text(stringResource(R.string.retry_button)) }
                    }
                }
                featured.properties.isEmpty() -> item {
                    Text(stringResource(R.string.featured_empty), modifier = Modifier.padding(16.dp))
                }
                else -> items(featured.properties, key = { it.id }) { property ->
                    FeaturedPropertyCard(
                        property = property,
                        onClick = { onOpenListing(property.slug) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            item {
                Button(onClick = onBrowseListings, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.home_see_all))
                }
            }

            if (featured.universities.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.home_universities_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = onOpenUniversities) { Text(stringResource(R.string.home_see_all_universities)) }
                    }
                }
                item {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(featured.universities, key = { it.id }) { uni ->
                            Card(modifier = Modifier.width(220.dp).clickable { onOpenUniversity(uni) }) {
                                PropertyPhoto(
                                    url = null,
                                    contentDescription = uni.name,
                                    fallbackKey = uni.id,
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    showSampleLabel = false
                                )
                                Column(Modifier.padding(12.dp)) {
                                    Text(uni.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                    Text(
                                        stringResource(R.string.uni_place, uni.city, uni.province),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                TextButton(onClick = onLearnMore, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.pub_more_about))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
