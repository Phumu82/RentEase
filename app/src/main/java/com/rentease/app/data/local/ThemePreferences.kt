package com.rentease.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Dark/light mode preference — deliberately a **plain, local-only**
 * SharedPreferences value, not synced to Supabase. This mirrors the web app
 * exactly: `src/lib/theme.tsx` stores the choice in the browser's
 * `localStorage` only (falling back to `prefers-color-scheme` when nothing is
 * stored) and never touches the `profiles` table. Non-sensitive, so no
 * encryption needed here (unlike [com.rentease.app.data.auth.TokenManager]).
 */
@Singleton
class ThemePreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("rentease_theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        when (prefs.getString(KEY_THEME_MODE, null)) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(
            KEY_THEME_MODE,
            when (mode) {
                ThemeMode.DARK -> "dark"
                ThemeMode.LIGHT -> "light"
                ThemeMode.SYSTEM -> null
            }
        ).apply()
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
