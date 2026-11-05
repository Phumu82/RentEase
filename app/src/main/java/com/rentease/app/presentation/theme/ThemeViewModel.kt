package com.rentease.app.presentation.theme

import androidx.lifecycle.ViewModel
import com.rentease.app.data.local.ThemeMode
import com.rentease.app.data.local.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Thin wrapper so the Compose root (MainActivity) can read the local
 *  dark/light preference without constructing [ThemePreferences] itself. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
}
