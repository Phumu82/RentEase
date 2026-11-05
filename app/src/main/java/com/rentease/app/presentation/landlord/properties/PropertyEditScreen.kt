package com.rentease.app.presentation.landlord.properties

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.presentation.common.DropdownField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import com.rentease.app.domain.model.PropertyPhotos
import com.rentease.app.presentation.common.PropertyPhoto
import com.rentease.app.domain.model.AmenityCatalog
import com.rentease.app.domain.model.PropertyInput

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertyEditScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: PropertyEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var bedrooms by remember { mutableStateOf("1") }
    var bathrooms by remember { mutableStateOf("1") }
    var furnished by remember { mutableStateOf(false) }
    var universityId by remember { mutableStateOf<String?>(null) }
    var distance by remember { mutableStateOf("") }
    var roomsAvailable by remember { mutableStateOf("1") }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }

    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) viewModel.addPhotos(context, uris)
    }

    LaunchedEffect(uiState.existing) {
        uiState.existing?.let { existing ->
            title = existing.title
            description = existing.description
            address = existing.address
            city = existing.city
            province = existing.province
            price = existing.pricePerMonth.toString()
            deposit = existing.deposit.toString()
            bedrooms = existing.bedrooms.toString()
            bathrooms = existing.bathrooms.toString()
            furnished = existing.furnished
            selectedAmenities = existing.amenities.toSet()
            universityId = existing.universityId
            distance = existing.distanceKm.takeIf { it > 0 }?.toString().orEmpty()
            roomsAvailable = existing.roomsAvailable.toString()
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onSaved()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (uiState.isEditMode) stringResource(R.string.edit_property_title) else stringResource(R.string.add_property_title)) }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {

            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.title_label)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.description_label)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(address, { address = it }, label = { Text(stringResource(R.string.address_label)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            DropdownField(
                label = stringResource(R.string.property_university_label),
                options = uiState.universities.map { it.id to it.name },
                selectedKey = universityId,
                onSelected = { id ->
                    universityId = id
                    // Pre-fill the location from the university so the listing is searchable by area.
                    uiState.universities.firstOrNull { it.id == id }?.let { uni ->
                        if (city.isBlank()) city = uni.city
                        if (province.isBlank()) province = uni.province
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                distance, { distance = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text(stringResource(R.string.property_distance_label)) },
                supportingText = { Text(stringResource(R.string.property_distance_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                roomsAvailable, { roomsAvailable = it.filter { ch -> ch.isDigit() }.take(3) },
                label = { Text(stringResource(R.string.property_rooms_available_label)) },
                supportingText = { Text(stringResource(R.string.property_rooms_available_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(city, { city = it }, label = { Text(stringResource(R.string.city_label)) }, modifier = Modifier.weight(1f))
                Spacer(Modifier.height(0.dp))
                OutlinedTextField(province, { province = it }, label = { Text(stringResource(R.string.province_label)) }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(price, { price = it }, label = { Text(stringResource(R.string.price_month_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(deposit, { deposit = it }, label = { Text(stringResource(R.string.deposit_field_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(bedrooms, { bedrooms = it }, label = { Text(stringResource(R.string.bedrooms_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(bathrooms, { bathrooms = it }, label = { Text(stringResource(R.string.bathrooms_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.furnished_label))
                Switch(checked = furnished, onCheckedChange = { furnished = it })
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.amenities_heading), style = MaterialTheme.typography.titleMedium)
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                AmenityCatalog.ALL.forEach { amenity ->
                    FilterChip(
                        selected = amenity in selectedAmenities,
                        onClick = {
                            selectedAmenities = if (amenity in selectedAmenities) selectedAmenities - amenity else selectedAmenities + amenity
                        },
                        label = { Text(amenity) },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.photos_heading), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.photos_hint, PropertyPhotos.MAX_PHOTOS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (uiState.photos.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { pickPhotos.launch("image/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.photos_empty_title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.photos_empty_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(uiState.photos, key = { _, url -> url }) { index, url ->
                        Card(modifier = Modifier.width(160.dp)) {
                            Box {
                                PropertyPhoto(
                                    url = url,
                                    contentDescription = stringResource(R.string.photos_item_description, index + 1),
                                    fallbackKey = url,
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    showSampleLabel = false
                                )
                                if (index == 0) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(stringResource(R.string.photos_cover_badge)) },
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removePhoto(url) },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color(0x99000000), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.photos_remove), tint = Color.White)
                                }
                            }
                            if (index != 0) {
                                TextButton(onClick = { viewModel.makeCover(url) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.photos_make_cover))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { pickPhotos.launch("image/*") },
                enabled = !uiState.isUploadingImage && PropertyPhotos.remainingSlots(uiState.photos) > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isUploadingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.photos_uploading))
                } else {
                    Text(stringResource(R.string.photos_add_button, uiState.photos.size, PropertyPhotos.MAX_PHOTOS))
                }
            }

            uiState.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    viewModel.save(
                        PropertyInput(
                            title = title, description = description, address = address, city = city,
                            province = province, universityId = universityId, distanceKm = distance.toDoubleOrNull() ?: 0.0, roomsAvailable = roomsAvailable.toIntOrNull() ?: 0,
                            pricePerMonth = price.toDoubleOrNull() ?: 0.0,
                            deposit = deposit.toDoubleOrNull() ?: 0.0,
                            bedrooms = bedrooms.toIntOrNull() ?: 1,
                            bathrooms = bathrooms.toIntOrNull() ?: 1,
                            furnished = furnished,
                            amenities = selectedAmenities.toList(),
                            coverImageUrl = uiState.photos.firstOrNull()
                        ),
                        context
                    )
                },
                enabled = !uiState.isSaving && title.isNotBlank() && city.isNotBlank() && price.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.height(18.dp)) else Text(stringResource(R.string.save_property_button))
            }
        }
    }
}
