package com.rentease.app.presentation.session

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentease.app.domain.model.AuthOutcome
import com.rentease.app.domain.model.AuthUser
import com.rentease.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-wide session actions: restoring a stored session at launch and a REAL
 * logout. Previously every "Log out" button only navigated back to the
 * role-select screen and never cleared the stored tokens.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /** Restores the session from encrypted storage; [onResult] gets null when there is none. */
    fun restore(onResult: (AuthUser?) -> Unit) {
        viewModelScope.launch {
            val user = (authRepository.restoreSession() as? AuthOutcome.Success)?.user
            if (user != null) applyStoredLanguage(user)
            onResult(user)
        }
    }

    /** Signs out server-side (best effort), wipes the encrypted tokens, then calls [onDone]. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    private fun applyStoredLanguage(user: AuthUser) {
        val wanted = user.preferredLanguage.dbCode
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (current != wanted && !(current.isEmpty() && wanted == "en")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(wanted))
        }
    }
}
