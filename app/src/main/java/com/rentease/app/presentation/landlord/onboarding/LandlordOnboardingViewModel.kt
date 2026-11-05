package com.rentease.app.presentation.landlord.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.LandlordDocumentKind
import com.rentease.app.domain.model.LandlordOnboardingInput
import com.rentease.app.domain.model.LandlordOnboardingProfile
import com.rentease.app.domain.model.OnboardingField
import com.rentease.app.domain.repository.OnboardingRepository
import com.rentease.app.domain.repository.OnboardingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LandlordOnboardingUiState(
    val isLoading: Boolean = true,
    /** Form pre-fill, loaded once so document uploads never reset typed edits. */
    val initial: LandlordOnboardingProfile? = null,
    /** Live verification view (status, notes, documents) — refreshed after every save/upload. */
    val current: LandlordOnboardingProfile? = null,
    val isSaving: Boolean = false,
    val uploading: LandlordDocumentKind? = null,
    val invalidFields: Set<OnboardingField> = emptySet(),
    val error: String? = null,
    val saved: Boolean = false,
    val documentUrlToOpen: String? = null
)

@HiltViewModel
class LandlordOnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandlordOnboardingUiState())
    val uiState: StateFlow<LandlordOnboardingUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = onboardingRepository.getLandlordOnboarding()) {
                is OnboardingResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false, initial = result.data, current = result.data
                )
                is OnboardingResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                is OnboardingResult.Invalid -> Unit
            }
        }
    }

    fun save(input: LandlordOnboardingInput) {
        _uiState.value = _uiState.value.copy(isSaving = true, error = null, invalidFields = emptySet())
        viewModelScope.launch {
            when (val result = onboardingRepository.saveLandlordOnboarding(input)) {
                is OnboardingResult.Success -> _uiState.value = _uiState.value.copy(isSaving = false, current = result.data, saved = true)
                is OnboardingResult.Invalid -> _uiState.value = _uiState.value.copy(isSaving = false, invalidFields = result.fields)
                is OnboardingResult.Failure -> _uiState.value = _uiState.value.copy(isSaving = false, error = result.message)
            }
        }
    }

    fun upload(kind: LandlordDocumentKind, uri: Uri) {
        _uiState.value = _uiState.value.copy(uploading = kind, error = null)
        viewModelScope.launch {
            when (val result = onboardingRepository.uploadLandlordDocument(kind, uri)) {
                is OnboardingResult.Success -> _uiState.value = _uiState.value.copy(uploading = null, current = result.data)
                is OnboardingResult.Failure -> _uiState.value = _uiState.value.copy(uploading = null, error = result.message)
                is OnboardingResult.Invalid -> _uiState.value = _uiState.value.copy(uploading = null)
            }
        }
    }

    fun open(kind: LandlordDocumentKind) {
        val path = _uiState.value.current?.documents?.get(kind) ?: return
        viewModelScope.launch {
            when (val result = onboardingRepository.getDocumentUrl(path)) {
                is OnboardingResult.Success -> _uiState.value = _uiState.value.copy(documentUrlToOpen = result.data)
                is OnboardingResult.Failure -> _uiState.value = _uiState.value.copy(error = result.message)
                is OnboardingResult.Invalid -> Unit
            }
        }
    }

    fun documentOpened() { _uiState.value = _uiState.value.copy(documentUrlToOpen = null) }
}
