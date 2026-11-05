package com.rentease.app.presentation.landlord.properties

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.R
import com.rentease.app.data.repository.DocumentUploadRepository
import com.rentease.app.data.repository.PrivateUploadResult
import com.rentease.app.domain.model.DocumentRules
import com.rentease.app.domain.model.LandlordProperty
import com.rentease.app.domain.model.PropertyInput
import com.rentease.app.domain.model.PropertyPhotos
import com.rentease.app.domain.model.University
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PropertyEditUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingImage: Boolean = false,
    /** Ordered gallery; the first photo is the cover. */
    val photos: List<String> = emptyList(),
    val existing: LandlordProperty? = null,
    /** Universities the landlord can link the property to (public list). */
    val universities: List<University> = emptyList(),
    val error: String? = null,
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class PropertyEditViewModel @Inject constructor(
    private val repository: LandlordRepository,
    private val listingsRepository: ListingsRepository,
    private val documentUploadRepository: DocumentUploadRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val propertyId: String? = savedStateHandle["propertyId"]

    /** Set once a new property has been created, so a retry after a photo failure updates instead of duplicating. */
    private var savedPropertyId: String? = propertyId

    private val _uiState = MutableStateFlow(PropertyEditUiState(isEditMode = propertyId != null))
    val uiState: StateFlow<PropertyEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val unis = (listingsRepository.getUniversities() as? ListingsResult.Success)?.data.orEmpty()
            _uiState.value = _uiState.value.copy(universities = unis)
        }
        propertyId?.let { id ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                when (val result = repository.getProperty(id)) {
                    is LandlordResult.Success -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        existing = result.data,
                        photos = PropertyPhotos.initial(result.data.coverImageUrl, result.data.images)
                    )
                    is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    /** Uploads the picked photos one by one and appends them to the gallery (max [PropertyPhotos.MAX_PHOTOS]). */
    fun addPhotos(context: Context, uris: List<Uri>) {
        val room = PropertyPhotos.remainingSlots(_uiState.value.photos)
        val picked = uris.take(room)
        if (picked.isEmpty()) {
            if (uris.isNotEmpty()) _uiState.value = _uiState.value.copy(error = context.getString(R.string.photos_limit_reached, PropertyPhotos.MAX_PHOTOS))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingImage = true, error = null)
            var problem: String? = if (uris.size > picked.size) context.getString(R.string.photos_limit_reached, PropertyPhotos.MAX_PHOTOS) else null
            for (uri in picked) {
                // Written under `{userId}/photos/...` (what the property-images storage policy
                // requires). Guests only see the URL if the bucket is marked Public in Supabase.
                when (val result = documentUploadRepository.uploadPrivate(context, uri, "property-images", "photos")) {
                    is PrivateUploadResult.Success -> {
                        val url = documentUploadRepository.publicUrlFor("property-images", result.path)
                        _uiState.value = _uiState.value.copy(photos = PropertyPhotos.add(_uiState.value.photos, url))
                    }
                    is PrivateUploadResult.Rejected -> problem = context.getString(
                        when (result.problem) {
                            DocumentRules.Problem.EMPTY -> R.string.document_error_empty
                            DocumentRules.Problem.UNSUPPORTED_TYPE -> R.string.document_error_type
                            DocumentRules.Problem.TOO_LARGE -> R.string.document_error_too_large
                        }
                    )
                    PrivateUploadResult.Failed -> problem = context.getString(R.string.error_image_upload_failed)
                }
            }
            _uiState.value = _uiState.value.copy(isUploadingImage = false, error = problem)
        }
    }

    fun removePhoto(url: String) {
        _uiState.value = _uiState.value.copy(photos = PropertyPhotos.remove(_uiState.value.photos, url))
    }

    fun makeCover(url: String) {
        _uiState.value = _uiState.value.copy(photos = PropertyPhotos.makeCover(_uiState.value.photos, url))
    }

    fun save(input: PropertyInput, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val photos = _uiState.value.photos
            val withCover = input.copy(coverImageUrl = photos.firstOrNull())
            val existingId = savedPropertyId
            val saved: LandlordResult<String> = if (existingId != null) {
                when (val r = repository.updateProperty(existingId, withCover)) {
                    is LandlordResult.Success -> LandlordResult.Success(existingId)
                    is LandlordResult.Failure -> LandlordResult.Failure(r.message)
                }
            } else {
                when (val r = repository.createProperty(withCover)) {
                    is LandlordResult.Success -> LandlordResult.Success(r.data.id)
                    is LandlordResult.Failure -> LandlordResult.Failure(r.message)
                }
            }
            when (saved) {
                is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(isSaving = false, error = saved.message)
                is LandlordResult.Success -> {
                    savedPropertyId = saved.data
                    when (val g = repository.setPropertyImages(saved.data, photos)) {
                        is LandlordResult.Success -> _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
                        is LandlordResult.Failure -> _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = context.getString(R.string.photos_saved_partial, g.message)
                        )
                    }
                }
            }
        }
    }
}
