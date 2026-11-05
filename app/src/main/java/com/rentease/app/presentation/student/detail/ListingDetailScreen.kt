package com.rentease.app.presentation.student.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.PropertyDetail
import com.rentease.app.domain.model.NearbyMaps
import com.rentease.app.domain.model.NearbyCategory
import androidx.compose.material3.Surface
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import android.content.Intent
import com.rentease.app.presentation.common.FeaturedPropertyCard
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import com.rentease.app.domain.model.BookableRoom
import com.rentease.app.domain.model.BookingQuote
import com.rentease.app.domain.model.LeaseOptions
import com.rentease.app.presentation.common.DatePickerField
import com.rentease.app.presentation.common.DropdownField
import com.rentease.app.presentation.common.PropertyGallery
import com.rentease.app.presentation.common.RentEaseCard

@Composable
fun ListingDetailScreen(
    onBack: () -> Unit,
    onViewBookings: () -> Unit,
    onSignInRequired: () -> Unit = {},
    onOpenListing: (slug: String) -> Unit = {},
    viewModel: ListingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showBookingDialog by remember { mutableStateOf(false) }
    // Close the sheet once the request has been accepted; the confirmation card takes over.
    LaunchedEffect(uiState.bookingSubmittedReference) {
        if (uiState.bookingSubmittedReference != null) showBookingDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.detail?.summary?.title ?: stringResource(R.string.listing_title_fallback)) },
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
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            uiState.detail != null -> {
                val detail = uiState.detail!!
                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

                    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).clip(RoundedCornerShape(20.dp))) {
                        PropertyGallery(
                            images = detail.images,
                            title = detail.summary.title,
                            fallbackKey = detail.summary.slug
                        )
                    }

                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${stringResource(R.string.price_per_month, detail.summary.pricePerMonth.toInt())} · " +
                                    stringResource(R.string.deposit_label, detail.deposit.toInt()),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { if (viewModel.isGuest) onSignInRequired() else viewModel.toggleFavourite() }) {
                                Icon(
                                    imageVector = if (detail.summary.isFavourited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = stringResource(R.string.save_listing_action),
                                    tint = if (detail.summary.isFavourited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            "${detail.address}, ${detail.summary.city} · " +
                                stringResource(R.string.km_from_campus, String.format("%.1f", detail.summary.distanceKm)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (uiState.rooms.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            RentEaseCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.rooms_title), style = MaterialTheme.typography.titleMedium)
                                    uiState.rooms.forEach { room ->
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(room.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                                Text(
                                                    "R${room.pricePerMonth.toInt()}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.about_this_place), style = MaterialTheme.typography.titleMedium)
                        Text(detail.description, style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.amenities_heading), style = MaterialTheme.typography.titleMedium)
                        Text(detail.amenities.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(16.dp))
                        LandlordSection(name = detail.landlordName, verified = detail.summary.verified)

                        Spacer(Modifier.height(16.dp))
                        NearbySection(detail)

                        Spacer(Modifier.height(16.dp))
                        RentEaseCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(R.string.reviews_heading, detail.reviews.size), style = MaterialTheme.typography.titleMedium)
                                detail.reviews.forEach { review ->
                                    Text("${review.authorName} — ${review.rating}★", style = MaterialTheme.typography.bodyMedium)
                                    if (review.body.isNotBlank()) {
                                        Text(review.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        if (uiState.similar.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.similar_heading), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(uiState.similar, key = { it.id }) { other ->
                                    FeaturedPropertyCard(
                                        property = other,
                                        onClick = { onOpenListing(other.slug) },
                                        modifier = Modifier.width(280.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        if (uiState.bookingSubmittedReference != null) {
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.booking_sent_title), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(R.string.booking_submitted_message, uiState.bookingSubmittedReference ?: ""),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Button(onClick = onViewBookings, modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(R.string.booking_view_my_bookings))
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { if (viewModel.isGuest) onSignInRequired() else showBookingDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    stringResource(
                                        if (viewModel.isGuest) R.string.guest_signin_to_book else R.string.request_booking_button
                                    )
                                )
                            }
                        }
                    }
                }

                if (showBookingDialog) {
                    BookingRequestDialog(
                        propertyRent = detail.summary.pricePerMonth,
                        deposit = detail.deposit,
                        rooms = uiState.rooms,
                        hasProofOnFile = uiState.hasProofOnFile,
                        isSubmitting = uiState.isSubmittingBooking,
                        error = uiState.bookingError,
                        onDismiss = { showBookingDialog = false },
                        onSubmit = { moveInDate, leaseMonths, roomId, message, idDoc, regDoc ->
                            viewModel.submitBooking(context, moveInDate, leaseMonths, roomId, message, idDoc, regDoc)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingRequestDialog(
    propertyRent: Double,
    deposit: Double,
    rooms: List<BookableRoom>,
    hasProofOnFile: Boolean,
    isSubmitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (moveInDate: String, leaseMonths: Int, roomId: String?, message: String?, idDoc: Uri, regDoc: Uri?) -> Unit
) {
    var moveInDate by remember { mutableStateOf("") } // ISO yyyy-MM-dd, chosen via DatePickerField
    var leaseMonths by remember { mutableStateOf(LeaseOptions.DEFAULT_MONTHS) }
    var roomKey by remember { mutableStateOf(ANY_ROOM) }
    var message by remember { mutableStateOf("") }
    var idDocumentUri by remember { mutableStateOf<Uri?>(null) }
    var proofOfRegistrationUri by remember { mutableStateOf<Uri?>(null) }

    val pickIdDoc = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) idDocumentUri = it }
    val pickRegDoc = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) proofOfRegistrationUri = it }

    val selectedRoom = rooms.firstOrNull { it.id == roomKey }
    val quote = BookingQuote.of(propertyRent, deposit, selectedRoom)
    val rentLabel = "R${quote.monthlyRent.toInt()}"
    val depositLabel = "R${quote.deposit.toInt()}"
    val needsProofUpload = !hasProofOnFile
    val canSubmit = !isSubmitting && moveInDate.isNotBlank() && idDocumentUri != null &&
        (!needsProofUpload || proofOfRegistrationUri != null)

    val anyRoomLabel = stringResource(R.string.booking_room_any)
    val roomOptions = listOf(ANY_ROOM to anyRoomLabel) + rooms.map {
        it.id to stringResource(R.string.booking_room_option, it.name, it.roomType, it.pricePerMonth.toInt())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.booking_dialog_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.booking_dialog_disclaimer), style = MaterialTheme.typography.bodyMedium)
                DatePickerField(
                    value = moveInDate,
                    onValueChange = { moveInDate = it },
                    label = stringResource(R.string.move_in_date_label),
                    minDateMillis = java.time.LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                )
                DropdownField(
                    label = stringResource(R.string.lease_period_label),
                    options = LeaseOptions.MONTHS.map { it.toString() to stringResource(R.string.booking_lease_option, it) },
                    selectedKey = leaseMonths.toString(),
                    onSelected = { leaseMonths = it.toInt() }
                )
                DropdownField(
                    label = stringResource(R.string.booking_room_label),
                    options = roomOptions,
                    selectedKey = roomKey,
                    onSelected = { roomKey = it }
                )
                OutlinedTextField(
                    message, { message = it },
                    label = { Text(stringResource(R.string.message_to_landlord_label)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.booking_monthly_rent))
                            Text(rentLabel, style = MaterialTheme.typography.titleSmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.booking_deposit_due))
                            Text(depositLabel, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                Text(stringResource(R.string.booking_documents_heading), style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = { pickIdDoc.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (idDocumentUri == null) stringResource(R.string.upload_student_id) else stringResource(R.string.student_id_selected))
                }
                if (hasProofOnFile) {
                    Text(stringResource(R.string.booking_proof_on_file), style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedButton(onClick = { pickRegDoc.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (proofOfRegistrationUri == null) stringResource(R.string.upload_proof_of_registration) else stringResource(R.string.proof_of_registration_selected))
                    }
                }
                Text(stringResource(R.string.document_hint), style = MaterialTheme.typography.bodySmall)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = {
                    onSubmit(
                        moveInDate, leaseMonths, selectedRoom?.id, message.trim().ifBlank { null },
                        idDocumentUri!!, proofOfRegistrationUri
                    )
                }
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.height(18.dp)) else Text(stringResource(R.string.submit_request_button))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}

private const val ANY_ROOM = "any"

@Composable
private fun LandlordSection(name: String, verified: Boolean) {
    Text(stringResource(R.string.landlord_heading), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    RentEaseCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        name.trim().take(1).uppercase().ifBlank { "?" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name.ifBlank { stringResource(R.string.landlord_unnamed) }, style = MaterialTheme.typography.titleSmall)
                if (verified) {
                    Text(stringResource(R.string.verification_status_verified), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    stringResource(R.string.landlord_booking_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NearbySection(detail: PropertyDetail) {
    val context = LocalContext.current
    fun open(uri: String) {
        // No maps app installed -> nothing to open; ignore instead of crashing.
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
    }
    val city = detail.summary.city
    Text(stringResource(R.string.nearby_heading), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(R.string.nearby_intro),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NearbyCategory.entries.forEach { category ->
            AssistChip(
                onClick = { open(NearbyMaps.searchUri(category, detail.latitude, detail.longitude, detail.address, city)) },
                label = {
                    Text(
                        stringResource(
                            when (category) {
                                NearbyCategory.TRANSPORT -> R.string.nearby_transport
                                NearbyCategory.SHOPS -> R.string.nearby_shops
                                NearbyCategory.FOOD -> R.string.nearby_food
                                NearbyCategory.HEALTH -> R.string.nearby_health
                            }
                        )
                    )
                }
            )
        }
        AssistChip(
            onClick = { open(NearbyMaps.locationUri(detail.summary.title, detail.latitude, detail.longitude, detail.address, city)) },
            label = { Text(stringResource(R.string.nearby_show_map)) }
        )
    }
}
