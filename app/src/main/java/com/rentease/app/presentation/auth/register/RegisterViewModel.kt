package com.rentease.app.presentation.auth.register

import kotlin.coroutines.cancellation.CancellationException
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.R
import com.rentease.app.domain.model.RegistrationOutcome
import com.rentease.app.domain.model.RegistrationValidation
import com.rentease.app.domain.model.LandlordRegistrationInput
import com.rentease.app.domain.model.StudentRegistrationInput
import com.rentease.app.domain.model.University
import com.rentease.app.domain.repository.AuthRepository
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import com.rentease.app.notifications.DeviceTokenRegistrar
import com.rentease.app.presentation.auth.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val listingsRepository: ListingsRepository,
    private val deviceTokenRegistrar: DeviceTokenRegistrar,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _confirmEmail = MutableStateFlow<String?>(null)
    /** Set when the account was created but needs its email confirmed before the first sign-in. */
    val confirmEmail: StateFlow<String?> = _confirmEmail.asStateFlow()

    private val _universities = MutableStateFlow<List<University>>(emptyList())
    /** Real universities from the backend, for the institution dropdowns. */
    val universities: StateFlow<List<University>> = _universities.asStateFlow()

    init {
        viewModelScope.launch {
            val result = listingsRepository.getUniversities()
            if (result is ListingsResult.Success) _universities.value = result.data
        }
    }

    /** Short student sign-up: contact details, university and student number. Preferences come later. */
    fun registerStudent(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        studentNumber: String,
        universityId: String
    ) {
        val problem = RegistrationValidation.validate(
            fullName, email, phone, password,
            // The university is only demanded when the list actually loaded; otherwise a failed load would
            // make sign-up impossible.
            listOfNotNull(studentNumber, universityId.takeIf { _universities.value.isNotEmpty() })
        )
        if (problem != null) {
            _uiState.value = AuthUiState.Error(appContext.getString(problem.messageRes()))
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val outcome = runGuarded { authRepository.registerStudent(
                StudentRegistrationInput(
                    fullName = fullName.trim(),
                    email = email.trim(),
                    password = password,
                    phone = phone.trim(),
                    studentNumber = studentNumber.trim(),
                    universityId = universityId
                )
            ) }
            _uiState.value = outcome.toUiState()
        }
    }

    /** Short landlord sign-up (name, email, phone, password) like the website; business details and documents follow in onboarding. */
    fun registerLandlord(fullName: String, email: String, password: String, phone: String) {
        val problem = RegistrationValidation.validate(fullName, email, phone, password)
        if (problem != null) {
            _uiState.value = AuthUiState.Error(appContext.getString(problem.messageRes()))
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val outcome = runGuarded { authRepository.registerLandlord(
                LandlordRegistrationInput(
                    fullName = fullName.trim(),
                    email = email.trim(),
                    password = password,
                    phone = phone.trim()
                )
            ) }
            _uiState.value = outcome.toUiState()
        }
    }

    /** A malformed or unexpected server answer must show a message, never crash the app. */
    private suspend fun runGuarded(block: suspend () -> RegistrationOutcome): RegistrationOutcome =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RegistrationOutcome.Failure(appContext.getString(R.string.error_request_failed))
        }

    private fun RegistrationValidation.Problem.messageRes(): Int = when (this) {
        RegistrationValidation.Problem.MISSING_FIELDS -> R.string.error_fill_required_fields
        RegistrationValidation.Problem.EMAIL_INVALID -> R.string.error_email_invalid
        RegistrationValidation.Problem.PHONE_INVALID -> R.string.error_phone_invalid
        RegistrationValidation.Problem.PASSWORD_RULES -> R.string.error_password_rules
    }

    private fun RegistrationOutcome.toUiState(): AuthUiState = when (this) {
        is RegistrationOutcome.Success -> {
            viewModelScope.launch { deviceTokenRegistrar.registerCurrentToken() }
            AuthUiState.Success(user)
        }
        is RegistrationOutcome.ConfirmEmail -> {
            _confirmEmail.value = email
            AuthUiState.Idle
        }
        is RegistrationOutcome.Failure -> AuthUiState.Error(message)
    }
}
