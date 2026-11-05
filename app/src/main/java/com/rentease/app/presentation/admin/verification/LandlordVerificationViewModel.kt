package com.rentease.app.presentation.admin.verification

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.R
import com.rentease.app.data.repository.DocumentUploadRepository
import com.rentease.app.domain.model.LandlordVerificationItem
import com.rentease.app.domain.model.VerificationDecision
import com.rentease.app.domain.model.VerificationFilter
import com.rentease.app.domain.model.VerificationReview
import com.rentease.app.domain.repository.AdminRepository
import com.rentease.app.domain.repository.AdminResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerificationQueueUiState(
    val isLoading: Boolean = true,
    val items: List<LandlordVerificationItem> = emptyList(),
    val filter: VerificationFilter = VerificationFilter.AWAITING_REVIEW,
    /** Admin's draft notes, per landlord. */
    val notes: Map<String, String> = emptyMap(),
    /** Landlord whose decision is being saved (buttons disabled meanwhile). */
    val busyUserId: String? = null,
    val error: String? = null,
    @StringRes val messageRes: Int? = null,
    val documentUrlToOpen: String? = null
) {
    val visible: List<LandlordVerificationItem> get() = items.filter { filter.matches(it.status) }
}

@HiltViewModel
class LandlordVerificationViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val documents: DocumentUploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationQueueUiState())
    val uiState: StateFlow<VerificationQueueUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, messageRes = null)
            when (val result = repository.getLandlordVerifications()) {
                is AdminResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, items = result.data)
                is AdminResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun setFilter(filter: VerificationFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun setNotes(userId: String, text: String) {
        if (text.length > VerificationReview.NOTES_MAX + 50) return
        _uiState.value = _uiState.value.copy(notes = _uiState.value.notes + (userId to text))
    }

    /** Opens a private document: a short-lived signed URL for storage paths (as the web does). */
    fun openDocument(path: String) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            // Older rows may hold a full URL; open it as-is.
            _uiState.value = _uiState.value.copy(documentUrlToOpen = path)
            return
        }
        viewModelScope.launch {
            val url = documents.createSignedUrl(DOCUMENT_BUCKET, path)
            _uiState.value = if (url != null) _uiState.value.copy(documentUrlToOpen = url, messageRes = null)
            else _uiState.value.copy(messageRes = R.string.error_document_open_failed)
        }
    }

    fun documentOpened() {
        _uiState.value = _uiState.value.copy(documentUrlToOpen = null)
    }

    fun decide(item: LandlordVerificationItem, decision: VerificationDecision) {
        if (_uiState.value.busyUserId != null) return
        val draft = _uiState.value.notes[item.userId].orEmpty()
        val problem = when (VerificationReview.check(item, decision, draft)) {
            VerificationReview.Check.Ok -> null
            VerificationReview.Check.NotesRequired -> R.string.error_verification_notes_required
            VerificationReview.Check.NotesTooLong -> R.string.error_verification_notes_too_long
            VerificationReview.Check.MissingDocuments -> R.string.error_verification_missing_documents
        }
        if (problem != null) {
            _uiState.value = _uiState.value.copy(messageRes = problem, error = null)
            return
        }
        _uiState.value = _uiState.value.copy(busyUserId = item.userId, messageRes = null, error = null)
        viewModelScope.launch {
            when (val result = repository.decideLandlord(item.userId, decision, VerificationReview.notesToSend(draft))) {
                is AdminResult.Success -> {
                    _uiState.value = _uiState.value.copy(busyUserId = null, notes = _uiState.value.notes - item.userId)
                    load()
                }
                is AdminResult.Failure -> _uiState.value = _uiState.value.copy(busyUserId = null, error = result.message)
            }
        }
    }

    companion object {
        /** Where onboarding and booking uploads live (private, `{uid}/folder/...`). */
        const val DOCUMENT_BUCKET = "booking-documents"
    }
}
