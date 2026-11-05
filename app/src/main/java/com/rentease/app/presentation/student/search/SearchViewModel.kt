package com.rentease.app.presentation.student.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.PropertySortOption
import com.rentease.app.domain.model.PropertySummary
import com.rentease.app.domain.model.SearchFilters
import com.rentease.app.domain.model.University
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val filters: SearchFilters = SearchFilters(),
    val sort: PropertySortOption = PropertySortOption.NEWEST,
    val universities: List<University> = emptyList(),
    val results: List<PropertySummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val page: Int = 0,
    val canLoadMore: Boolean = true
)

/** Client-side sort, applied over whatever has been fetched so far — same
 *  approach as the web app's `listings.index.tsx`. */
private fun List<PropertySummary>.sortedByOption(option: PropertySortOption): List<PropertySummary> = when (option) {
    PropertySortOption.NEWEST -> this // API already returns created_at.desc; no reorder needed
    PropertySortOption.PRICE_ASC -> sortedBy { it.pricePerMonth }
    PropertySortOption.PRICE_DESC -> sortedByDescending { it.pricePerMonth }
    PropertySortOption.RATING -> sortedByDescending { it.rating }
    PropertySortOption.DISTANCE -> sortedBy { it.distanceKm }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val listingsRepository: ListingsRepository,
    tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** True when browsing without an account (public listings only; no saving or booking). */
    val isGuest: Boolean = tokenManager.getUserId() == null

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Optional nav argument: arriving from a university page pre-selects that campus.
        val universityArg = savedStateHandle.get<String>("university")?.takeIf { it.isNotBlank() }
        val keywordArg = savedStateHandle.get<String>("keyword")?.takeIf { it.isNotBlank() }
        if (universityArg != null || keywordArg != null) {
            _uiState.value = _uiState.value.copy(
                filters = SearchFilters(universityId = universityArg, keyword = keywordArg.orEmpty())
            )
        }
        loadUniversities()
        search(resetPage = true)
    }

    private fun loadUniversities() {
        viewModelScope.launch {
            when (val result = listingsRepository.getUniversities()) {
                is ListingsResult.Success -> _uiState.value = _uiState.value.copy(universities = result.data)
                is ListingsResult.Failure -> { /* non-fatal — search still works without the picker populated */ }
            }
        }
    }

    fun updateFilters(filters: SearchFilters) {
        _uiState.value = _uiState.value.copy(filters = filters)
        search(resetPage = true)
    }

    fun updateSort(sort: PropertySortOption) {
        _uiState.value = _uiState.value.copy(sort = sort, results = _uiState.value.results.sortedByOption(sort))
    }

    fun loadNextPage() {
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return
        search(resetPage = false)
    }

    fun toggleFavourite(property: PropertySummary) {
        viewModelScope.launch {
            when (val result = listingsRepository.toggleFavourite(property.id, property.isFavourited)) {
                is ListingsResult.Success -> {
                    val updated = _uiState.value.results.map {
                        if (it.id == property.id) it.copy(isFavourited = result.data) else it
                    }
                    _uiState.value = _uiState.value.copy(results = updated)
                }
                is ListingsResult.Failure -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    private fun search(resetPage: Boolean) {
        val state = _uiState.value
        val nextPage = if (resetPage) 0 else state.page + 1
        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = listingsRepository.search(state.filters, page = nextPage)) {
                is ListingsResult.Success -> {
                    val combined = if (resetPage) result.data else _uiState.value.results + result.data
                    _uiState.value = _uiState.value.copy(
                        results = combined.sortedByOption(_uiState.value.sort),
                        page = nextPage,
                        isLoading = false,
                        canLoadMore = result.data.isNotEmpty(),
                        error = null
                    )
                }
                is ListingsResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
