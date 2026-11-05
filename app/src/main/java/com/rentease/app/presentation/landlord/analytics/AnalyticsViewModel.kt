package com.rentease.app.presentation.landlord.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.LandlordAnalytics
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val analytics: LandlordAnalytics? = null,
    val error: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: LandlordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getAnalytics()) {
                is LandlordResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, analytics = result.data)
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }
}
