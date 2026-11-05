package com.rentease.app

import android.Manifest
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentease.app.data.local.ThemeMode
import com.rentease.app.presentation.navigation.RentEaseNavGraph
import com.rentease.app.presentation.theme.RentEaseTheme
import com.rentease.app.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity Compose host. Extends AppCompatActivity (not plain
 * ComponentActivity) so AppCompatDelegate.setApplicationLocales() - used by
 * Settings > Language - re-renders the UI on API 26-32 as well as API 33+.
 * All screens (auth, student, landlord, admin) are
 * Composable destinations inside RentEaseNavGraph — there is no WebView anywhere
 * in this app, per the explicit "do not use WebView as the final implementation"
 * requirement.
 *
 * Deep links from FCM notifications (rentease://booking/{id}, etc. — see
 * NotificationPoster) arrive here via the ACTION_VIEW intent-filter declared
 * in the manifest; RentEaseNavGraph reads `intent.data` at the relevant
 * destinations to route directly to the right screen rather than always
 * landing on the role's home tab.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    /** Latest rentease:// link; updated by [onNewIntent] when a link arrives while the app is open. */
    private var deepLinkUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkUri = intent?.data?.toString()
        setContent {
            RentEaseApp(deepLinkUri = deepLinkUri)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri = intent.data?.toString()
    }
}

@Composable
private fun RentEaseApp(deepLinkUri: String?) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op either way — notifications simply won't show in-tray if denied */ }

    LaunchedEffect(Unit) {
        // POST_NOTIFICATIONS is a runtime permission from API 33+; below that,
        // notification channels created at install time are enough.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    // The user can force light/dark independently of the phone setting, so the status/navigation bar
    // icons must follow the chosen theme rather than the system one (otherwise: dark icons on a dark page).
    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(activity, useDarkTheme) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (useDarkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (useDarkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        onDispose { }
    }

    RentEaseTheme(darkTheme = useDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            RentEaseNavGraph(startDeepLink = deepLinkUri)
        }
    }
}
