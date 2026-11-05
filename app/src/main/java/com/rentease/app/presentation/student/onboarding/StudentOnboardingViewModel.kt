package com.rentease.app.presentation.student.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.OnboardingField
import com.rentease.app.domain.model.StudentOnboardingInput
import com.rentease.app.domain.model.StudentOnboardingProfile
import com.rentease.app.domain.model.University
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

data class StudentOnboardingUiState(
    val isLoading: Boolean = true,
    /** Values the form is pre-filled with (loaded once, so uploads don't reset typed edits). */
    val initial: StudentOnboardingProfile? = null,
    val universities: List<University> = emptyList(),
    val proofPath: String? = null,
    val isSaving: Boolean = false,
    val isUploading: Boolean = false,
    val invalidFields: Set<OnboardingField> = emptySet(),
    val error: String? = null,
    val saved: Boolean = false,
    /** One-shot: a signed URL the screen should open, then clear via [StudentOnboardingViewModel.documentOpened]. */
    val documentUrlToOpen: String? = null
)

@HiltViewModel
class StudentOnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val listingsRepository: ListingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentOnboardingUiState())
    val uiState: StateFlow<StudentOnboardingUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val universities = (listingsRepository.getUniversities() as? ListingsResult.Success)?.data.orEmpty()
            when (val result = onboardingRepository.getStudentOnboarding()) {
                is OnboardingResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    initial = result.data,
                    universities = universities,
                    proofPath = result.data.proofOfRegistrationPath
                )
                is OnboardingResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false, universities = universities, error = result.message
                )
                is OnboardingResult.Invalid -> Unit
            }
        }
    }

    fun save(input: StudentOnboardingInput) {
        _uiState.value = _uiState.value.copy(isSaving = true, error = null, invalidFields = emptySet())
        viewModelScope.launch {
            when (val result = onboardingRepository.saveStudentOnboarding(input)) {
                is OnboardingResult.Success -> _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
                is OnboardingResult.Invalid -> _uiState.value = _uiState.value.copy(isSaving = false, invalidFields = result.fields)
                is OnboardingResult.Failure -> _uiState.value = _uiState.value.copy(isSaving = false, error = result.message)
            }
        }
    }

    fun uploadProof(uri: Uri) {
        _uiState.value = _uiState.value.copy(isUploading = true, error = null)
        viewModelScope.launch {
            when (val result = onboardingRepository.uploadProofOfRegistration(uri)) {
                is OnboardingResult.Success -> _uiState.value = _uiState.value.copy(
                    isUploading = false, proofPath = result.data.proofOfRegistrationPath
                )
                is OnboardingResult.Failure -> _uiState.value = _uiState.value.copy(isUploading = false, error = result.message)
                is OnboardingResult.Invalid -> _uiState.value = _uiState.value.copy(isUploading = false)
            }
        }
    }

    fun openProof() {
        val path = _uiState.value.proofPath ?: return
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
