package com.rentease.app.presentation.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.Complaint
import com.rentease.app.domain.model.ComplaintInput
import com.rentease.app.domain.repository.SupportRepository
import com.rentease.app.domain.repository.SupportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportUiState(
    val isLoading: Boolean = true,
    val complaints: List<Complaint> = emptyList(),
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitSuccess: Boolean = false
)

/** Backs both the student and landlord "Support" screen — same UI, same
 *  repository, since a complaint belongs to whoever is signed in regardless
 *  of role (mirrors the web app's single shared `/support` route). */
@HiltViewModel
class SupportViewModel @Inject constructor(
    private val repository: SupportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getMyComplaints()) {
                is SupportResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, complaints = result.data)
                is SupportResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun submit(subject: String, category: String, description: String) {
        _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null, submitSuccess = false)
        viewModelScope.launch {
            when (val result = repository.submitComplaint(ComplaintInput(subject, category, description))) {
                is SupportResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = true)
                    load()
                }
                is SupportResult.Failure -> _uiState.value = _uiState.value.copy(isSubmitting = false, submitError = result.message)
            }
        }
    }
}
