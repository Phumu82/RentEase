package com.rentease.app.presentation.landlord.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.LandlordProperty
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PropertyListUiState(
    val isLoading: Boolean = true,
    val properties: List<LandlordProperty> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PropertyListViewModel @Inject constructor(
    private val repository: LandlordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertyListUiState())
    val uiState: StateFlow<PropertyListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getMyProperties()) {
                is LandlordResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, properties = result.data)
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun togglePublished(property: LandlordProperty) {
        viewModelScope.launch {
            val nowPublished = property.status != com.rentease.app.domain.model.PropertyStatus.PUBLISHED
            val result = repository.setPropertyPublished(property.id, nowPublished)
            if (result is LandlordResult.Failure) {
                _uiState.value = _uiState.value.copy(error = result.message)
            } else {
                load()
            }
        }
    }

    fun deleteProperty(property: LandlordProperty) {
        viewModelScope.launch {
            val result = repository.deleteProperty(property.id)
            if (result is LandlordResult.Failure) {
                _uiState.value = _uiState.value.copy(error = result.message)
            } else {
                load()
            }
        }
    }
}
