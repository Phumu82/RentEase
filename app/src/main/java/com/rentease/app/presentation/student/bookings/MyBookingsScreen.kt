package com.rentease.app.presentation.student.bookings

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.R
import com.rentease.app.domain.model.BookingStatus
import com.rentease.app.domain.model.StudentBookingSummary
import com.rentease.app.presentation.common.label
import com.rentease.app.presentation.theme.RentEaseGold

/** Same eligibility rule as student maintenance requests: a review only makes
 *  sense once the booking was actually approved (the DB itself doesn't enforce
 *  a status, only "one review per booking"). */
private val REVIEWABLE_STATUSES = setOf(BookingStatus.APPROVED, BookingStatus.ACTIVE, BookingStatus.COMPLETED)

private const val MIN_REVIEW_LENGTH = 10

@Composable
fun MyBookingsScreen(
    onBack: () -> Unit,
    viewModel: MyBookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var reviewingBooking by remember { mutableStateOf<StudentBookingSummary?>(null) }

    // Close the dialog once the review has been posted successfully.
    LaunchedEffect(uiState.reviewSubmitted) {
        if (uiState.reviewSubmitted) reviewingBooking = null
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.my_bookings_title)) }) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.bookings.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_bookings_yet))
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                uiState.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                uiState.cancelMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) } }
                if (uiState.reviewSubmitted) {
                    item {
                        Text(stringResource(R.string.review_submitted_confirmation), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(uiState.bookings, key = { it.id }) { booking ->
                    BookingRow(
                        booking = booking,
                        isCancelling = uiState.cancellingId == booking.id,
                        onCancel = { viewModel.cancel(booking.id) },
                        canReview = booking.status in REVIEWABLE_STATUSES && booking.id !in uiState.reviewedBookingIds,
                        onWriteReview = {
                            viewModel.clearReviewState()
                            reviewingBooking = booking
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    reviewingBooking?.let { booking ->
        ReviewDialog(
            propertyTitle = booking.propertyTitle,
            isSubmitting = uiState.isSubmittingReview,
            error = uiState.reviewError,
            onSubmit = { rating, title, body -> viewModel.submitReview(booking, rating, title, body) },
            onDismiss = {
                reviewingBooking = null
                viewModel.clearReviewState()
            }
        )
    }
}

@Composable
private fun BookingRow(
    booking: StudentBookingSummary,
    isCancelling: Boolean,
    onCancel: () -> Unit,
    canReview: Boolean,
    onWriteReview: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(booking.propertyTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(booking.status.label(), style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.booking_reference_label, booking.reference), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.lease_summary_label, booking.moveInDate, booking.leaseMonths, booking.monthlyRent.toInt()),
                style = MaterialTheme.typography.bodyMedium
            )
            // Mirrors student.bookings.tsx: cancellation is only offered while pending.
            if (booking.status == BookingStatus.PENDING) {
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onCancel, enabled = !isCancelling) {
                        Text(stringResource(R.string.cancel_booking_button))
                    }
                }
            }
            // A booking is either pending (cancellable) or approved+ (reviewable), never both.
            if (canReview) {
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onWriteReview) {
                        Text(stringResource(R.string.write_review_button))
                    }
                }
            }
        }
    }
}

/** Rating (1-5 stars) + optional title + body. Submit stays disabled until the
 *  input is valid, so the ViewModel never needs its own validation strings. */
@Composable
private fun ReviewDialog(
    propertyTitle: String,
    isSubmitting: Boolean,
    error: String?,
    onSubmit: (rating: Int, title: String, body: String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val canSubmit = !isSubmitting && rating in 1..5 && body.trim().length >= MIN_REVIEW_LENGTH

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(stringResource(R.string.write_review_button)) },
        text = {
            Column {
                Text(propertyTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = stringResource(R.string.review_rating_star_description, star),
                                tint = if (star <= rating) RentEaseGold else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.review_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(stringResource(R.string.review_body_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(rating, title, body) }, enabled = canSubmit) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.review_submit_button))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
