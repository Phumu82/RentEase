package com.rentease.app.presentation.student.compare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.PropertyComparison
import com.rentease.app.domain.model.PropertyDetail
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComparisonUiState(
    val isLoading: Boolean = true,
    val items: List<PropertyDetail> = emptyList(),
    val error: String? = null
)

/** Loads the full detail of each selected listing (comma-separated `slugs` nav argument). */
@HiltViewModel
class ComparisonViewModel @Inject constructor(
    private val listingsRepository: ListingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val slugs: List<String> = (savedStateHandle.get<String>("slugs") ?: "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(PropertyComparison.MAX_ITEMS)

    private val _uiState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ComparisonUiState(isLoading = true)
            val loaded = mutableListOf<PropertyDetail>()
            var firstError: String? = null
            for (slug in slugs) {
                when (val result = listingsRepository.getPropertyDetail(slug)) {
                    is ListingsResult.Success -> loaded.add(result.data)
                    is ListingsResult.Failure -> if (firstError == null) firstError = result.message
                }
            }
            _uiState.value = ComparisonUiState(
                isLoading = false,
                items = loaded,
                error = if (loaded.size < PropertyComparison.MIN_ITEMS) firstError else null
            )
        }
    }
}
