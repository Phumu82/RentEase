package com.rentease.app.presentation.student.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.ReviewInput
import com.rentease.app.domain.model.StudentBookingSummary
import com.rentease.app.domain.repository.AuthRepository
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyBookingsUiState(
    val isLoading: Boolean = true,
    val bookings: List<StudentBookingSummary> = emptyList(),
    val error: String? = null,
    val cancellingId: String? = null,
    val cancelMessage: String? = null,
    val reviewedBookingIds: Set<String> = emptySet(),
    val isSubmittingReview: Boolean = false,
    val reviewError: String? = null,
    val reviewSubmitted: Boolean = false
)

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val repository: ListingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyBookingsUiState())
    val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getMyBookings()) {
                is ListingsResult.Success -> {
                    // Best-effort: if this lookup fails we simply offer "Write a review" on every
                    // eligible booking, and the server's one-review-per-booking rule (HTTP 409)
                    // still prevents duplicates.
                    val reviewedIds = (repository.getMyReviewedBookingIds() as? ListingsResult.Success)?.data.orEmpty()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, bookings = result.data, reviewedBookingIds = reviewedIds
                    )
                }
                is ListingsResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun clearReviewState() {
        _uiState.value = _uiState.value.copy(reviewError = null, reviewSubmitted = false)
    }

    /** Posts a review tied to [booking]. Input is validated by the dialog before
     *  this is called (rating 1..5, non-trivial body). */
    fun submitReview(booking: StudentBookingSummary, rating: Int, title: String, body: String) {
        _uiState.value = _uiState.value.copy(isSubmittingReview = true, reviewError = null, reviewSubmitted = false)
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            val authorName = when {
                user == null -> ""
                user.fullName.isNotBlank() -> user.fullName
                else -> user.email.substringBefore("@")
            }
            val input = ReviewInput(
                propertyId = booking.propertyId,
                bookingId = booking.id,
                authorName = authorName,
                rating = rating,
                title = title.trim(),
                body = body.trim()
            )
            when (val result = repository.createReview(input)) {
                is ListingsResult.Success -> _uiState.value = _uiState.value.copy(
                    isSubmittingReview = false,
                    reviewSubmitted = true,
                    reviewedBookingIds = _uiState.value.reviewedBookingIds + booking.id
                )
                is ListingsResult.Failure -> _uiState.value = _uiState.value.copy(
                    isSubmittingReview = false, reviewError = result.message
                )
            }
        }
    }

    /** Cancels a pending booking, then refreshes the list — same effect as the
     *  web app's cancel button (a plain status update, only ever offered for
     *  "pending" bookings, matching `student.bookings.tsx`). */
    fun cancel(bookingId: String) {
        _uiState.value = _uiState.value.copy(cancellingId = bookingId, cancelMessage = null)
        viewModelScope.launch {
            when (val result = repository.cancelBooking(bookingId)) {
                is ListingsResult.Success -> {
                    _uiState.value = _uiState.value.copy(cancellingId = null)
                    load()
                }
                is ListingsResult.Failure -> _uiState.value = _uiState.value.copy(
                    cancellingId = null, cancelMessage = result.message
                )
            }
        }
    }
}
