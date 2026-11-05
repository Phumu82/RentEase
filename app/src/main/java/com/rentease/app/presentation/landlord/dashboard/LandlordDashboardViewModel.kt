package com.rentease.app.presentation.landlord.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.LandlordAnalytics
import com.rentease.app.domain.model.LandlordProfile
import com.rentease.app.domain.model.VerificationProgress
import com.rentease.app.domain.model.VerificationProgressCalculator
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LandlordDashboardUiState(
    val isLoading: Boolean = true,
    val profile: LandlordProfile? = null,
    val analytics: LandlordAnalytics? = null,
    val progress: VerificationProgress? = null,
    val error: String? = null
)

@HiltViewModel
class LandlordDashboardViewModel @Inject constructor(
    private val repository: LandlordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandlordDashboardUiState())
    val uiState: StateFlow<LandlordDashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val profileResult = repository.getVerificationStatus()
            val analyticsResult = repository.getAnalytics()
            val propertiesResult = repository.getMyProperties()

            val profile = (profileResult as? LandlordResult.Success)?.data
            val analytics = (analyticsResult as? LandlordResult.Success)?.data
            val properties = (propertiesResult as? LandlordResult.Success)?.data
            // Progress needs both the profile and the property list; without either we
            // show no card rather than a misleading one.
            val progress = if (profile != null && properties != null)
                VerificationProgressCalculator.compute(profile, properties) else null
            val error = (profileResult as? LandlordResult.Failure)?.message
                ?: (analyticsResult as? LandlordResult.Failure)?.message

            _uiState.value = _uiState.value.copy(
                isLoading = false, profile = profile, analytics = analytics, progress = progress, error = error
            )
        }
    }
}
