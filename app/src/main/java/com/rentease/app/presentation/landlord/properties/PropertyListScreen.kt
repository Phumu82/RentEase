package com.rentease.app.presentation.landlord.properties

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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import com.rentease.app.domain.model.LandlordProperty
import com.rentease.app.domain.model.PropertyStatus
import com.rentease.app.presentation.common.label

@Composable
fun PropertyListScreen(
    onAddProperty: () -> Unit,
    onEditProperty: (propertyId: String) -> Unit,
    onManageRooms: (propertyId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: PropertyListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.my_properties_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProperty) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_property_title)) }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.properties.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PropertyPhoto(
                    url = null,
                    contentDescription = null,
                    fallbackKey = "landlord-empty",
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)),
                    showSampleLabel = false
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_properties_yet), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.no_properties_photos_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddProperty, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_first_property_button))
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                items(uiState.properties, key = { it.id }) { property ->
                    PropertyRow(
                        property = property,
                        onEdit = { onEditProperty(property.id) },
                        onManageRooms = { onManageRooms(property.id) },
                        onTogglePublish = { viewModel.togglePublished(property) },
                        onDelete = { viewModel.deleteProperty(property) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    property: LandlordProperty,
    onEdit: () -> Unit,
    onManageRooms: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(property.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(property.status.label(), style = MaterialTheme.typography.bodyMedium)
            }
            PropertyPhoto(
                url = property.coverImageUrl,
                contentDescription = property.title,
                fallbackKey = property.id,
                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text("${property.city}, ${property.province} · " + stringResource(R.string.price_per_month, property.pricePerMonth.toInt()), style = MaterialTheme.typography.bodyMedium)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.published_label), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = property.status == PropertyStatus.PUBLISHED,
                        onCheckedChange = { onTogglePublish() },
                        // Only draft/published can be flipped from here — pending/rejected/archived
                        // are controlled by the admin moderation workflow.
                        enabled = property.status == PropertyStatus.PUBLISHED || property.status == PropertyStatus.DRAFT
                    )
                }
                Row {
                    TextButton(onClick = onManageRooms) { Text(stringResource(R.string.rooms_link)) }
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.edit_button)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete_button)) }
                }
            }
        }
    }
}

