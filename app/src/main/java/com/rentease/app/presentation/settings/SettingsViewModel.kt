package com.rentease.app.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.data.local.ThemeMode
import com.rentease.app.data.local.ThemePreferences
import com.rentease.app.domain.model.AppLanguage
import com.rentease.app.domain.model.AuthOutcome
import com.rentease.app.domain.model.NotificationPreferences
import com.rentease.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSaving: Boolean = false,
    val error: String? = null,
    val profileSaved: Boolean = false,
    val fullName: String = "",
    val email: String = "",
    val phone: String = ""
)

/**
 * Settings = profile (editable name + phone), language, notification
 * preferences, logout. Everything is persisted through [AuthRepository]:
 *  - language        -> profiles.preferred_language (+ AppCompatDelegate locale, applied immediately)
 *  - name / phone    -> profiles.full_name / profiles.phone
 *  - notify_* flags  -> profiles.notify_* (also honoured client-side by the FCM
 *                       service, see RentEaseMessagingService)
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _profileSaved = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.currentUser, _isSaving, _error, _profileSaved, themePreferences.themeMode
    ) { user, isSaving, error, profileSaved, themeMode ->
        SettingsUiState(
            currentLanguage = user?.preferredLanguage ?: AppLanguage.ENGLISH,
            notificationPreferences = user?.notificationPreferences ?: NotificationPreferences(),
            themeMode = themeMode,
            isSaving = isSaving,
            error = error,
            profileSaved = profileSaved,
            fullName = user?.fullName.orEmpty(),
            email = user?.email.orEmpty(),
            phone = user?.phone.orEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = themePreferences.setThemeMode(mode)

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            when (val outcome = authRepository.updateLanguage(language)) {
                is AuthOutcome.Success -> {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(language.dbCode)
                    )
                }
                is AuthOutcome.Failure -> _error.value = outcome.message
            }
            _isSaving.value = false
        }
    }

    fun saveProfile(fullName: String, phone: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            _profileSaved.value = false
            when (val outcome = authRepository.updateProfile(fullName, phone)) {
                is AuthOutcome.Success -> _profileSaved.value = true
                is AuthOutcome.Failure -> _error.value = outcome.message
            }
            _isSaving.value = false
        }
    }

    fun setBookingUpdatesEnabled(enabled: Boolean) =
        updatePreferences(uiState.value.notificationPreferences.copy(bookingUpdates = enabled))

    fun setNewMessagesEnabled(enabled: Boolean) =
        updatePreferences(uiState.value.notificationPreferences.copy(newMessages = enabled))

    fun setMaintenanceUpdatesEnabled(enabled: Boolean) =
        updatePreferences(uiState.value.notificationPreferences.copy(maintenanceUpdates = enabled))

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    private fun updatePreferences(preferences: NotificationPreferences) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            when (val outcome = authRepository.updateNotificationPreferences(preferences)) {
                is AuthOutcome.Success -> Unit
                is AuthOutcome.Failure -> _error.value = outcome.message
            }
            _isSaving.value = false
        }
    }
}
