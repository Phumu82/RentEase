package com.rentease.app.presentation.student.maintenance

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.R
import com.rentease.app.data.repository.DocumentUploadRepository
import com.rentease.app.data.repository.PrivateUploadResult
import com.rentease.app.domain.model.DocumentRules
import com.rentease.app.domain.model.BookingStatus
import com.rentease.app.domain.model.MaintenancePriority
import com.rentease.app.domain.model.MaintenanceRequestInput
import com.rentease.app.domain.model.MaintenanceTicket
import com.rentease.app.domain.model.StudentBookingSummary
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Bookings a maintenance request can reasonably be logged against — same set
 *  the web app queries (`bookings.status in (approved, active, completed)`). */
private val ELIGIBLE_STATUSES = setOf(BookingStatus.APPROVED, BookingStatus.ACTIVE, BookingStatus.COMPLETED)

data class StudentMaintenanceUiState(
    val isLoading: Boolean = true,
    val eligibleBookings: List<StudentBookingSummary> = emptyList(),
    val requests: List<MaintenanceTicket> = emptyList(),
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitSuccess: Boolean = false,
    /** One-shot signed URL of a ticket photo for the screen to open. */
    val photoUrlToOpen: String? = null,
    val photoOpenFailed: Boolean = false
)

@HiltViewModel
class StudentMaintenanceViewModel @Inject constructor(
    private val repository: ListingsRepository,
    private val documentUploadRepository: DocumentUploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentMaintenanceUiState())
    val uiState: StateFlow<StudentMaintenanceUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val bookingsResult = repository.getMyBookings()
            val requestsResult = repository.getMyMaintenanceRequests()

            val eligibleBookings = (bookingsResult as? ListingsResult.Success)?.data
                .orEmpty()
                .filter { it.status in ELIGIBLE_STATUSES }

            val requests = (requestsResult as? ListingsResult.Success)?.data.orEmpty()

            val error = (requestsResult as? ListingsResult.Failure)?.message
                ?: (bookingsResult as? ListingsResult.Failure)?.message

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                eligibleBookings = eligibleBookings,
                requests = requests,
                error = error
            )
        }
    }

    fun clearSubmitState() {
        _uiState.value = _uiState.value.copy(submitError = null, submitSuccess = false)
    }

    /** Uploads the optional photo (if any), then inserts the request — mirrors
     *  ListingDetailViewModel.submitBooking's upload-then-create shape. */
    fun submit(
        context: Context,
        bookingId: String,
        title: String,
        description: String,
        category: String,
        priority: MaintenancePriority,
        photoUri: Uri?
    ) {
        val booking = _uiState.value.eligibleBookings.firstOrNull { it.id == bookingId } ?: return

        if (title.trim().length < 4) {
            _uiState.value = _uiState.value.copy(submitError = context.getString(R.string.maintenance_validation_title_short))
            return
        }
        if (description.trim().length < 10) {
            _uiState.value = _uiState.value.copy(submitError = context.getString(R.string.maintenance_validation_description_short))
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null, submitSuccess = false)

        viewModelScope.launch {
            // Same as the web: private `booking-documents` bucket, `{userId}/maintenance/...`,
            // and the object PATH is what gets stored in maintenance_requests.photo_url.
            var photoPath: String? = null
            if (photoUri != null) {
                when (val upload = documentUploadRepository.uploadPrivate(context, photoUri, "booking-documents", "maintenance")) {
                    is PrivateUploadResult.Success -> photoPath = upload.path
                    is PrivateUploadResult.Rejected -> {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            submitError = context.getString(
                                when (upload.problem) {
                                    DocumentRules.Problem.EMPTY -> R.string.document_error_empty
                                    DocumentRules.Problem.UNSUPPORTED_TYPE -> R.string.document_error_type
                                    DocumentRules.Problem.TOO_LARGE -> R.string.document_error_too_large
                                }
                            )
                        )
                        return@launch
                    }
                    PrivateUploadResult.Failed -> {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            submitError = context.getString(R.string.error_upload_documents)
                        )
                        return@launch
                    }
                }
            }

            val outcome = repository.createMaintenanceRequest(
                MaintenanceRequestInput(
                    propertyId = booking.propertyId,
                    bookingId = booking.id,
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    photoUrl = photoPath
                )
            )

            when (outcome) {
                is ListingsResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = true)
                    load()
                }
                is ListingsResult.Failure -> _uiState.value = _uiState.value.copy(
                    isSubmitting = false, submitError = outcome.message
                )
            }
        }
    }

    /** Resolves a stored photo path into a short-lived signed URL for viewing. */
    fun openPhoto(path: String) {
        viewModelScope.launch {
            val url = documentUploadRepository.createSignedUrl("booking-documents", path)
            _uiState.value = if (url != null) _uiState.value.copy(photoUrlToOpen = url, photoOpenFailed = false)
            else _uiState.value.copy(photoOpenFailed = true)
        }
    }

    fun photoOpened() { _uiState.value = _uiState.value.copy(photoUrlToOpen = null) }
}
