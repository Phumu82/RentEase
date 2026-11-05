package com.rentease.app.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.PropertySummary
import com.rentease.app.domain.model.SearchFilters
import com.rentease.app.domain.model.University
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeaturedUiState(
    val isLoading: Boolean = true,
    val properties: List<PropertySummary> = emptyList(),
    val universities: List<University> = emptyList(),
    val failed: Boolean = false
)

/** The first screen's "Featured places": real published properties (public read), best rated first. */
@HiltViewModel
class FeaturedListingsViewModel @Inject constructor(
    private val listingsRepository: ListingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeaturedUiState())
    val uiState: StateFlow<FeaturedUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.value = FeaturedUiState(isLoading = true)
        viewModelScope.launch {
            // Campuses are a best-effort extra; the listings list is what matters most.
            val campuses = (listingsRepository.getUniversities() as? ListingsResult.Success)?.data.orEmpty()
            _uiState.value = when (val result = listingsRepository.search(SearchFilters(), page = 0, pageSize = FEATURED_COUNT)) {
                is ListingsResult.Success -> FeaturedUiState(
                    isLoading = false,
                    properties = result.data.sortedByDescending { it.rating },
                    universities = campuses
                )
                is ListingsResult.Failure -> FeaturedUiState(isLoading = false, failed = true, universities = campuses)
            }
        }
    }

    companion object { const val FEATURED_COUNT = 6 }
}
