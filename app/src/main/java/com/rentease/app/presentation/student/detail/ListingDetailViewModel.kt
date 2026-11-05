package com.rentease.app.presentation.student.detail

import com.rentease.app.R
import com.rentease.app.domain.model.PropertySummary
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.data.repository.DocumentUploadRepository
import com.rentease.app.data.repository.PrivateUploadResult
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.domain.model.BookableRoom
import com.rentease.app.domain.model.BookingQuote
import com.rentease.app.domain.model.BookingRequestInput
import com.rentease.app.domain.model.DocumentRules
import com.rentease.app.domain.model.PropertyDetail
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import com.rentease.app.domain.repository.OnboardingRepository
import com.rentease.app.domain.repository.OnboardingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListingDetailUiState(
    val isLoading: Boolean = true,
    val detail: PropertyDetail? = null,
    val error: String? = null,
    val isSubmittingBooking: Boolean = false,
    val bookingSubmittedReference: String? = null,
    val bookingError: String? = null,
    /** Available rooms for the booking sheet (cheapest first). */
    val rooms: List<BookableRoom> = emptyList(),
    /** True when the student already has a proof of registration on file from onboarding. */
    val hasProofOnFile: Boolean = false,
    /** Other listings in the same city; empty when none or the lookup failed. */
    val similar: List<PropertySummary> = emptyList()
)

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    private val listingsRepository: ListingsRepository,
    private val documentUploadRepository: DocumentUploadRepository,
    private val onboardingRepository: OnboardingRepository,
    tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** True when viewing without an account: public details only, no saving or booking. */
    val isGuest: Boolean = tokenManager.getUserId() == null

    private val slug: String = checkNotNull(savedStateHandle["slug"])

    private val _uiState = MutableStateFlow(ListingDetailUiState())
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = listingsRepository.getPropertyDetail(slug)) {
                is ListingsResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, detail = result.data)
                    loadSimilar(result.data.summary)
                    loadBookingOptions(result.data.summary.id)
                }
                is ListingsResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    private fun loadSimilar(summary: PropertySummary) {
        viewModelScope.launch {
            val similar = (listingsRepository.getSimilar(summary) as? ListingsResult.Success)?.data.orEmpty()
            _uiState.value = _uiState.value.copy(similar = similar)
        }
    }

    /** Rooms + whether a proof of registration is already on file. Best-effort: a failure
     *  only means "Any available room" is offered and the proof must be uploaded. */
    private suspend fun loadBookingOptions(propertyId: String) {
        val rooms = (listingsRepository.getAvailableRooms(propertyId) as? ListingsResult.Success)?.data.orEmpty()
        val proofPath = if (isGuest) null
        else (onboardingRepository.getStudentOnboarding() as? OnboardingResult.Success)?.data?.proofOfRegistrationPath
        _uiState.value = _uiState.value.copy(rooms = rooms, hasProofOnFile = !proofPath.isNullOrBlank())
    }

    fun toggleFavourite() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            when (val result = listingsRepository.toggleFavourite(detail.summary.id, detail.summary.isFavourited)) {
                is ListingsResult.Success -> _uiState.value = _uiState.value.copy(
                    detail = detail.copy(summary = detail.summary.copy(isFavourited = result.data))
                )
                is ListingsResult.Failure -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    /**
     * Uploads the booking documents into the private `booking-documents` bucket (ID always;
     * proof of registration only when none is on file from onboarding), then creates the
     * booking with status=pending — never charges the student at this stage. Guarded against
     * double taps; the repository guards against duplicate booking/payment rows on retries.
     */
    fun submitBooking(
        context: Context,
        moveInDate: String,
        leaseMonths: Int,
        roomId: String?,
        message: String?,
        idDocumentUri: Uri,
        proofOfRegistrationUri: Uri?
    ) {
        val detail = _uiState.value.detail ?: return
        if (_uiState.value.isSubmittingBooking) return
        _uiState.value = _uiState.value.copy(isSubmittingBooking = true, bookingError = null)

        viewModelScope.launch {
            val idPath = when (val r = documentUploadRepository.uploadPrivate(context, idDocumentUri, BUCKET, "booking-id")) {
                is PrivateUploadResult.Success -> r.path
                is PrivateUploadResult.Rejected -> return@launch fail(context.getString(rejectionRes(r.problem)))
                PrivateUploadResult.Failed -> return@launch fail(context.getString(R.string.error_upload_documents))
            }

            val proofPath = if (proofOfRegistrationUri != null) {
                when (val r = documentUploadRepository.uploadPrivate(context, proofOfRegistrationUri, BUCKET, "booking-proof")) {
                    is PrivateUploadResult.Success -> r.path
                    is PrivateUploadResult.Rejected -> return@launch fail(context.getString(rejectionRes(r.problem)))
                    PrivateUploadResult.Failed -> return@launch fail(context.getString(R.string.error_upload_documents))
                }
            } else {
                val onFile = (onboardingRepository.getStudentOnboarding() as? OnboardingResult.Success)?.data?.proofOfRegistrationPath
                onFile?.takeIf { it.isNotBlank() } ?: return@launch fail(context.getString(R.string.error_upload_documents))
            }

            val room = _uiState.value.rooms.firstOrNull { it.id == roomId }
            val quote = BookingQuote.of(detail.summary.pricePerMonth, detail.deposit, room)

            when (val outcome = listingsRepository.createBookingRequest(
                BookingRequestInput(
                    propertyId = detail.summary.id,
                    moveInDate = moveInDate,
                    leaseMonths = leaseMonths,
                    monthlyRent = quote.monthlyRent,
                    deposit = quote.deposit,
                    message = message,
                    idDocumentUrl = idPath,
                    proofOfRegistrationUrl = proofPath,
                    roomId = room?.id,
                    landlordId = detail.landlordId
                )
            )) {
                is ListingsResult.Success -> _uiState.value = _uiState.value.copy(
                    isSubmittingBooking = false, bookingSubmittedReference = outcome.data
                )
                is ListingsResult.Failure -> fail(outcome.message)
            }
        }
    }

    private fun fail(message: String) {
        _uiState.value = _uiState.value.copy(isSubmittingBooking = false, bookingError = message)
    }

    private fun rejectionRes(problem: DocumentRules.Problem): Int = when (problem) {
        DocumentRules.Problem.EMPTY -> R.string.document_error_empty
        DocumentRules.Problem.UNSUPPORTED_TYPE -> R.string.document_error_type
        DocumentRules.Problem.TOO_LARGE -> R.string.document_error_too_large
    }

    private companion object {
        const val BUCKET = "booking-documents"
    }
}
