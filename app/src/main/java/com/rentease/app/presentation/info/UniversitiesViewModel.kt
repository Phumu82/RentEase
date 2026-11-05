package com.rentease.app.presentation.info

import androidx.lifecycle.SavedStateHandle
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

data class UniversitiesUiState(
    val isLoading: Boolean = true,
    val universities: List<University> = emptyList(),
    val keyword: String = "",
    val error: String? = null
)

/** Public universities directory, read straight from the `universities` table (anon-readable). */
@HiltViewModel
class UniversitiesViewModel @Inject constructor(
    private val listingsRepository: ListingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UniversitiesUiState())
    val uiState: StateFlow<UniversitiesUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = listingsRepository.getUniversities()) {
                is ListingsResult.Success ->
                    _uiState.value = _uiState.value.copy(isLoading = false, universities = result.data)
                is ListingsResult.Failure ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun setKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(keyword = keyword)
    }
}

data class UniversityDetailUiState(
    val isLoading: Boolean = true,
    val university: University? = null,
    val nearby: List<PropertySummary> = emptyList(),
    val notFound: Boolean = false,
    val error: String? = null
)

/** One campus plus up to six published properties linked to it. */
@HiltViewModel
class UniversityDetailViewModel @Inject constructor(
    private val listingsRepository: ListingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val universityId: String = checkNotNull(savedStateHandle["universityId"])
    private val _uiState = MutableStateFlow(UniversityDetailUiState())
    val uiState: StateFlow<UniversityDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.value = UniversityDetailUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = listingsRepository.getUniversities()) {
                is ListingsResult.Failure ->
                    _uiState.value = UniversityDetailUiState(isLoading = false, error = result.message)
                is ListingsResult.Success -> {
                    val uni = result.data.firstOrNull { it.id == universityId }
                    if (uni == null) {
                        _uiState.value = UniversityDetailUiState(isLoading = false, notFound = true)
                        return@launch
                    }
                    // Nearby listings are a best-effort extra; the campus info still shows without them.
                    val nearby = (listingsRepository.search(SearchFilters(universityId = uni.id), page = 0, pageSize = 6)
                        as? ListingsResult.Success)?.data.orEmpty()
                    _uiState.value = UniversityDetailUiState(isLoading = false, university = uni, nearby = nearby)
                }
            }
        }
    }
}
