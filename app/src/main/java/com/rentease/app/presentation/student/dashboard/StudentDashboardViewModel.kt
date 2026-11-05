package com.rentease.app.presentation.student.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.totalPaid
import com.rentease.app.domain.repository.AuthRepository
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import com.rentease.app.domain.repository.NotificationsRepository
import com.rentease.app.domain.repository.NotificationsResult
import com.rentease.app.domain.repository.OnboardingRepository
import com.rentease.app.domain.repository.OnboardingResult
import com.rentease.app.domain.repository.SavedListingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentDashboardUiState(
    val isLoading: Boolean = true,
    val studentName: String? = null,
    val bookingsCount: Int = 0,
    val savedCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val totalPaid: Double = 0.0,
    /** True when the student's `onboarding_complete` flag is still false — shows the resume banner. */
    val onboardingIncomplete: Boolean = false,
    val error: String? = null
)

/**
 * Mirrors the web app's student dashboard (`student.index.tsx`): a quick-glance
 * overview (booking/saved/unread counts) plus links into the rest of the student
 * area, shown right after login instead of dropping the student straight into
 * search. All three counts come from the same repositories already used
 * elsewhere (no new backend endpoints) — this is aggregation only, over real
 * Supabase-backed data.
 */
@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val listingsRepository: ListingsRepository,
    private val savedListingsRepository: SavedListingsRepository,
    private val notificationsRepository: NotificationsRepository,
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentDashboardUiState())
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val name = authRepository.currentUser.value?.fullName
            val bookingsResult = listingsRepository.getMyBookings()
            val notificationsResult = notificationsRepository.getNotifications()
            val paymentsResult = listingsRepository.getMyPayments()
            val onboardingResult = onboardingRepository.getStudentOnboarding()
            val savedCount = runCatching { savedListingsRepository.observeSavedListings().first().size }
                .getOrDefault(0)

            val bookingsCount = (bookingsResult as? ListingsResult.Success)?.data?.size ?: 0
            val unreadCount = (notificationsResult as? NotificationsResult.Success)?.data
                ?.count { it.readAt == null } ?: 0

            val totalPaid = (paymentsResult as? ListingsResult.Success)?.data?.totalPaid() ?: 0.0
            // Only flag as incomplete on a successful read; a failed read must not nag.
            val onboardingIncomplete = (onboardingResult as? OnboardingResult.Success)?.data?.onboardingComplete == false

            val error = (bookingsResult as? ListingsResult.Failure)?.message
                ?: (notificationsResult as? NotificationsResult.Failure)?.message

            _uiState.value = StudentDashboardUiState(
                isLoading = false,
                studentName = name,
                bookingsCount = bookingsCount,
                savedCount = savedCount,
                unreadNotificationsCount = unreadCount,
                totalPaid = totalPaid,
                onboardingIncomplete = onboardingIncomplete,
                error = error
            )
        }
    }
}
