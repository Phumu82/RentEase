package com.rentease.app.presentation.navigation

import androidx.compose.runtime.Composable
import com.rentease.app.presentation.auth.reset.ResetPasswordScreen
import com.rentease.app.presentation.auth.reset.ForgotPasswordScreen
import com.rentease.app.domain.model.PasswordReset
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rentease.app.domain.model.AuthUser
import com.rentease.app.domain.model.UserRole
import com.rentease.app.presentation.info.AboutScreen
import com.rentease.app.presentation.info.ContactScreen
import com.rentease.app.presentation.info.FaqScreen
import com.rentease.app.presentation.info.HowItWorksScreen
import com.rentease.app.presentation.info.InfoHubScreen
import com.rentease.app.presentation.info.InfoPage
import com.rentease.app.presentation.info.PricingScreen
import com.rentease.app.presentation.info.PrivacyScreen
import com.rentease.app.presentation.info.ServicesScreen
import com.rentease.app.presentation.info.TermsScreen
import com.rentease.app.presentation.info.UniversitiesScreen
import com.rentease.app.presentation.info.UniversityDetailScreen
import com.rentease.app.presentation.admin.audit.AuditLogScreen
import com.rentease.app.presentation.admin.access.AdminAccessScreen
import com.rentease.app.presentation.admin.bookings.AdminBookingsScreen
import com.rentease.app.presentation.admin.dashboard.AdminDashboardScreen
import com.rentease.app.presentation.admin.moderation.PropertyModerationScreen
import com.rentease.app.presentation.admin.users.UserManagementScreen
import com.rentease.app.presentation.admin.verification.LandlordVerificationScreen
import com.rentease.app.presentation.auth.login.LoginScreen
import com.rentease.app.presentation.auth.register.LandlordRegisterScreen
import com.rentease.app.presentation.auth.register.RoleSelectScreen
import com.rentease.app.presentation.auth.register.StudentRegisterScreen
import com.rentease.app.presentation.landlord.analytics.AnalyticsScreen
import com.rentease.app.presentation.landlord.bookings.BookingRequestsScreen
import com.rentease.app.presentation.landlord.dashboard.LandlordDashboardScreen
import com.rentease.app.presentation.landlord.maintenance.MaintenanceScreen
import com.rentease.app.presentation.landlord.messages.ConversationListScreen
import com.rentease.app.presentation.landlord.messages.ThreadScreen
import com.rentease.app.presentation.landlord.notifications.NotificationsScreen
import com.rentease.app.presentation.landlord.properties.PropertyEditScreen
import com.rentease.app.presentation.landlord.properties.PropertyListScreen
import com.rentease.app.presentation.landlord.rooms.RoomManagementScreen
import com.rentease.app.presentation.landlord.tenants.TenantsScreen
import com.rentease.app.presentation.session.SessionViewModel
import com.rentease.app.presentation.session.SplashScreen
import com.rentease.app.presentation.settings.SettingsScreen
import com.rentease.app.presentation.student.bookings.MyBookingsScreen
import com.rentease.app.presentation.student.compare.ComparisonScreen
import com.rentease.app.presentation.admin.complaints.AdminComplaintsScreen
import com.rentease.app.presentation.student.dashboard.StudentDashboardScreen
import com.rentease.app.presentation.student.onboarding.StudentOnboardingScreen
import com.rentease.app.presentation.landlord.onboarding.LandlordOnboardingScreen
import com.rentease.app.presentation.support.SupportScreen
import com.rentease.app.presentation.student.detail.ListingDetailScreen
import com.rentease.app.presentation.student.maintenance.StudentMaintenanceScreen
import com.rentease.app.presentation.student.payments.PaymentsScreen
import com.rentease.app.presentation.student.messages.StudentConversationListScreen
import com.rentease.app.presentation.student.messages.StudentThreadScreen
import com.rentease.app.presentation.student.notifications.StudentNotificationsScreen
import com.rentease.app.presentation.student.saved.SavedListingsScreen
import com.rentease.app.presentation.student.search.ListingsScreen

@Composable
fun RentEaseNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SPLASH,
    /** Raw deep-link URI from an FCM notification tap (rentease://...), if
     *  the activity was launched via one — see MainActivity/NotificationPoster.
     *  Handled best-effort: if the relevant screen needs an authenticated
     *  session and the user isn't signed in, the destination's own
     *  repository calls fail gracefully to an error state rather than
     *  crashing (every repository in this app returns a Failure result for
     *  "not signed in" instead of throwing). */
    startDeepLink: String? = null
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    // Links that arrive while the app is already running (singleTop re-delivery). The first link is
    // applied by the splash screen; this remembers what was handled so rotation does not repeat it.
    var handledDeepLink by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(startDeepLink) {
        val link = startDeepLink ?: return@LaunchedEffect
        val current = navController.currentDestination?.route
        if (link != handledDeepLink && current != null && current != Routes.SPLASH) {
            handledDeepLink = link
            navController.navigateToDeepLink(link)
        }
    }
    // Real logout: clears the encrypted session (server sign-out + local tokens),
    // THEN returns to role selection and wipes the back stack.
    val logout: () -> Unit = {
        sessionViewModel.logout {
            navController.navigate(Routes.ROLE_SELECT) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // Restores a stored session (if any) and routes by role; otherwise role select.
        // The FCM deep link (if the app was opened from a notification) is applied
        // once the destination is known.
        composable(Routes.SPLASH) {
            SplashScreen(
                onResolved = { user ->
                    if (user != null) {
                        navController.navigate(homeRouteFor(user)) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.ROLE_SELECT) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                    startDeepLink?.let { uri ->
                        handledDeepLink = uri
                        navController.navigateToDeepLink(uri)
                    }
                }
            )
        }

        composable(Routes.ROLE_SELECT) {
            RoleSelectScreen(
                onStudentSelected = { navController.navigate(Routes.REGISTER_STUDENT) },
                onLandlordSelected = { navController.navigate(Routes.REGISTER_LANDLORD) },
                onSignInInstead = { navController.navigate(Routes.LOGIN) },
                onLearnMore = { navController.navigate(Routes.INFO_HUB) },
                onBrowseListings = { navController.navigate(Routes.GUEST_BROWSE) },
                onOpenListing = { slug -> navController.navigate("student/listing/$slug") },
                onSearch = { keyword ->
                    navController.navigate(
                        if (keyword.isBlank()) Routes.GUEST_BROWSE
                        else "guest/browse?keyword=${android.net.Uri.encode(keyword)}"
                    )
                },
                onOpenUniversity = { uni -> navController.navigate("info/universities/${uni.id}") },
                onOpenUniversities = { navController.navigate(Routes.INFO_UNIVERSITIES) }
            )
        }

        // Public (signed-out) browsing of published listings. Saving and booking ask to sign in.
        composable(
            route = Routes.GUEST_BROWSE_PATTERN,
            arguments = listOf(
                navArgument("university") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("keyword") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            // Signed-out visitors get public browsing only (the screen hides account shortcuts and asks
            // to sign in for saving/booking); a signed-in student arriving from a university page
            // gets the normal controls, so the real destinations are wired here as well.
            ListingsScreen(
                onPropertyClick = { slug -> navController.navigate("student/listing/$slug") },
                onBack = { navController.popBackStack() },
                onCompare = { slugs -> navController.navigate("student/compare/${slugs.joinToString(",")}") },
                onLogout = logout,
                onNavigateToSaved = { navController.navigate(Routes.STUDENT_SAVED) },
                onNavigateToBookings = { navController.navigate(Routes.STUDENT_BOOKINGS) },
                onNavigateToMaintenance = { navController.navigate(Routes.STUDENT_MAINTENANCE) },
                onNavigateToMessages = { navController.navigate(Routes.STUDENT_MESSAGES) },
                onNavigateToNotifications = { navController.navigate(Routes.STUDENT_NOTIFICATIONS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onSignInRequired = { navController.navigate(Routes.LOGIN) }
            )
        }

        // ---------------- Public info pages (no sign-in) ----------------
        composable(Routes.INFO_HUB) {
            InfoHubScreen(
                onBack = { navController.popBackStack() },
                onOpen = { page ->
                    navController.navigate(
                        when (page) {
                            InfoPage.UNIVERSITIES -> Routes.INFO_UNIVERSITIES
                            InfoPage.ABOUT -> Routes.INFO_ABOUT
                            InfoPage.HOW_IT_WORKS -> Routes.INFO_HOW_IT_WORKS
                            InfoPage.SERVICES -> Routes.INFO_SERVICES
                            InfoPage.PRICING -> Routes.INFO_PRICING
                            InfoPage.FAQ -> Routes.INFO_FAQ
                            InfoPage.CONTACT -> Routes.INFO_CONTACT
                            InfoPage.PRIVACY -> Routes.INFO_PRIVACY
                            InfoPage.TERMS -> Routes.INFO_TERMS
                        }
                    )
                }
            )
        }
        composable(Routes.INFO_UNIVERSITIES) {
            UniversitiesScreen(
                onBack = { navController.popBackStack() },
                onOpen = { uni -> navController.navigate("info/universities/${uni.id}") }
            )
        }
        composable(
            route = Routes.INFO_UNIVERSITY,
            arguments = listOf(navArgument("universityId") { type = NavType.StringType })
        ) {
            UniversityDetailScreen(
                onBack = { navController.popBackStack() },
                onBrowseListings = { uni -> navController.navigate("guest/browse?university=${uni.id}") },
                onOpenProperty = { slug -> navController.navigate("student/listing/$slug") }
            )
        }
        composable(Routes.INFO_ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.INFO_HOW_IT_WORKS) {
            HowItWorksScreen(
                onBack = { navController.popBackStack() },
                onCreateStudent = { navController.navigate(Routes.REGISTER_STUDENT) },
                onCreateLandlord = { navController.navigate(Routes.REGISTER_LANDLORD) }
            )
        }
        composable(Routes.INFO_SERVICES) {
            ServicesScreen(
                onBack = { navController.popBackStack() },
                onContact = { navController.navigate(Routes.INFO_CONTACT) },
                onPricing = { navController.navigate(Routes.INFO_PRICING) }
            )
        }
        composable(Routes.INFO_PRICING) {
            PricingScreen(
                onBack = { navController.popBackStack() },
                onCreateStudent = { navController.navigate(Routes.REGISTER_STUDENT) },
                onCreateLandlord = { navController.navigate(Routes.REGISTER_LANDLORD) },
                onContact = { navController.navigate(Routes.INFO_CONTACT) }
            )
        }
        composable(Routes.INFO_FAQ) {
            FaqScreen(
                onBack = { navController.popBackStack() },
                onContact = { navController.navigate(Routes.INFO_CONTACT) }
            )
        }
        composable(Routes.INFO_CONTACT) { ContactScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.INFO_PRIVACY) { PrivacyScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.INFO_TERMS) { TermsScreen(onBack = { navController.popBackStack() }) }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { user -> navController.routeToHome(user) },
                onNavigateToRegister = { navController.navigate(Routes.ROLE_SELECT) },
                onNavigateToAdminAccess = { navController.navigate(Routes.ADMIN_ACCESS) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.RESET_PASSWORD_PATTERN,
            arguments = listOf(navArgument("token") { type = NavType.StringType; defaultValue = "" })
        ) {
            ResetPasswordScreen(
                onDone = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Routes.ADMIN_ACCESS) {
            AdminAccessScreen(
                onAccessGranted = {
                    navController.navigate(Routes.ADMIN_HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER_STUDENT) {
            // Straight to the dashboard: its "finish your profile" banner leads to onboarding when the
            // student is ready, so sign-up is not followed by a second long form.
            StudentRegisterScreen(
                onRegistered = { user -> navController.routeToHome(user) },
                onGoToLogin = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.LOGIN) { inclusive = true } } }
            )
        }

        composable(Routes.REGISTER_LANDLORD) {
            // The landlord dashboard shows the verification banner that leads to business details and documents.
            LandlordRegisterScreen(
                onRegistered = { user -> navController.routeToHome(user) },
                onGoToLogin = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.LOGIN) { inclusive = true } } }
            )
        }

        // Student core: search/listings + listing detail + booking request (Phase 5).
        composable(Routes.STUDENT_HOME) {
            StudentDashboardScreen(
                onNavigateToSearch = { navController.navigate(Routes.STUDENT_SEARCH) },
                onNavigateToSaved = { navController.navigate(Routes.STUDENT_SAVED) },
                onNavigateToBookings = { navController.navigate(Routes.STUDENT_BOOKINGS) },
                onNavigateToMaintenance = { navController.navigate(Routes.STUDENT_MAINTENANCE) },
                onNavigateToMessages = { navController.navigate(Routes.STUDENT_MESSAGES) },
                onNavigateToNotifications = { navController.navigate(Routes.STUDENT_NOTIFICATIONS) },
                onNavigateToSupport = { navController.navigate(Routes.SUPPORT) },
                onNavigateToPayments = { navController.navigate(Routes.STUDENT_PAYMENTS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToOnboarding = { navController.navigate(Routes.STUDENT_ONBOARDING) },
                onLogout = logout
            )
        }
        composable(Routes.STUDENT_ONBOARDING) {
            StudentOnboardingScreen(
                // Re-enter home so the dashboard (and its banner) reload with fresh data.
                onDone = {
                    navController.navigate(Routes.STUDENT_HOME) {
                        popUpTo(Routes.STUDENT_HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.LANDLORD_ONBOARDING) {
            LandlordOnboardingScreen(
                onDone = {
                    navController.navigate(Routes.LANDLORD_HOME) {
                        popUpTo(Routes.LANDLORD_HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.STUDENT_PAYMENTS) {
            PaymentsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STUDENT_SEARCH) {
            ListingsScreen(
                onPropertyClick = { slug -> navController.navigate("student/listing/$slug") },
                onBack = { navController.popBackStack() },
                onCompare = { slugs -> navController.navigate("student/compare/${slugs.joinToString(",")}") },
                onNavigateToSaved = { navController.navigate(Routes.STUDENT_SAVED) },
                onNavigateToBookings = { navController.navigate(Routes.STUDENT_BOOKINGS) },
                onNavigateToMaintenance = { navController.navigate(Routes.STUDENT_MAINTENANCE) },
                onNavigateToMessages = { navController.navigate(Routes.STUDENT_MESSAGES) },
                onNavigateToNotifications = { navController.navigate(Routes.STUDENT_NOTIFICATIONS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onLogout = logout
            )
        }
        composable(Routes.STUDENT_MAINTENANCE) {
            StudentMaintenanceScreen(onBack = { navController.popBackStack() })
        }
        // Shared (role-agnostic): reachable from both the student and landlord dashboards.
        composable(Routes.SUPPORT) {
            SupportScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STUDENT_SAVED) {
            SavedListingsScreen(
                onPropertyClick = { slug -> navController.navigate("student/listing/$slug") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.STUDENT_BOOKINGS) {
            MyBookingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STUDENT_NOTIFICATIONS) {
            StudentNotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STUDENT_MESSAGES) {
            StudentConversationListScreen(
                onOpenThread = { conversationId -> navController.navigate("student/messages/$conversationId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.STUDENT_THREAD,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) {
            StudentThreadScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.STUDENT_COMPARE,
            arguments = listOf(navArgument("slugs") { type = NavType.StringType })
        ) {
            ComparisonScreen(
                onBack = { navController.popBackStack() },
                onPropertyClick = { slug -> navController.navigate("student/listing/$slug") }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.ROLE_SELECT) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(
            route = "student/listing/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) {
            ListingDetailScreen(
                onBack = { navController.popBackStack() },
                onViewBookings = { navController.navigate(Routes.STUDENT_BOOKINGS) },
                onSignInRequired = { navController.navigate(Routes.LOGIN) },
                onOpenListing = { slug -> navController.navigate("student/listing/$slug") }
            )
        }

        // Placeholder destinations removed — Admin dashboard is now real (below).

        // ---------------- Admin core (Phase 7) ----------------

        composable(Routes.ADMIN_HOME) {
            AdminDashboardScreen(
                onNavigateToVerifications = { navController.navigate(Routes.ADMIN_VERIFICATIONS) },
                onNavigateToModeration = { navController.navigate(Routes.ADMIN_MODERATION) },
                onNavigateToUsers = { navController.navigate(Routes.ADMIN_USERS) },
                onNavigateToBookings = { navController.navigate(Routes.ADMIN_BOOKINGS) },
                onNavigateToAuditLog = { navController.navigate(Routes.ADMIN_AUDIT_LOG) },
                onNavigateToComplaints = { navController.navigate(Routes.ADMIN_COMPLAINTS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onLogout = logout
            )
        }

        composable(Routes.ADMIN_COMPLAINTS) {
            AdminComplaintsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_VERIFICATIONS) {
            LandlordVerificationScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_MODERATION) {
            PropertyModerationScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_USERS) {
            UserManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_BOOKINGS) {
            AdminBookingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_AUDIT_LOG) {
            AuditLogScreen(onBack = { navController.popBackStack() })
        }

        // ---------------- Landlord core (Phase 6) ----------------

        composable(Routes.LANDLORD_HOME) {
            LandlordDashboardScreen(
                onNavigateToProperties = { navController.navigate(Routes.LANDLORD_PROPERTIES) },
                onNavigateToBookings = { navController.navigate(Routes.LANDLORD_BOOKINGS) },
                onNavigateToTenants = { navController.navigate(Routes.LANDLORD_TENANTS) },
                onNavigateToMaintenance = { navController.navigate(Routes.LANDLORD_MAINTENANCE) },
                onNavigateToMessages = { navController.navigate(Routes.LANDLORD_MESSAGES) },
                onNavigateToNotifications = { navController.navigate(Routes.LANDLORD_NOTIFICATIONS) },
                onNavigateToAnalytics = { navController.navigate(Routes.LANDLORD_ANALYTICS) },
                onNavigateToSupport = { navController.navigate(Routes.SUPPORT) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToOnboarding = { navController.navigate(Routes.LANDLORD_ONBOARDING) },
                onLogout = logout
            )
        }

        composable(Routes.LANDLORD_PROPERTIES) {
            PropertyListScreen(
                onAddProperty = { navController.navigate(Routes.LANDLORD_PROPERTY_ADD) },
                onEditProperty = { propertyId -> navController.navigate("landlord/properties/$propertyId/edit") },
                onManageRooms = { propertyId -> navController.navigate("landlord/properties/$propertyId/rooms") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.LANDLORD_PROPERTY_ADD) {
            PropertyEditScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.LANDLORD_PROPERTY_EDIT,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) {
            PropertyEditScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.LANDLORD_ROOMS,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) {
            RoomManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LANDLORD_BOOKINGS) {
            BookingRequestsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LANDLORD_TENANTS) {
            TenantsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LANDLORD_MAINTENANCE) {
            MaintenanceScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LANDLORD_MESSAGES) {
            ConversationListScreen(
                onOpenThread = { conversationId -> navController.navigate("landlord/messages/$conversationId") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.LANDLORD_THREAD,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) {
            ThreadScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LANDLORD_NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LANDLORD_ANALYTICS) {
            AnalyticsScreen(onBack = { navController.popBackStack() })
        }
    }

}

/**
 * Parses a `rentease://...` URI (from an FCM notification tap) and routes to
 * the matching destination. Deliberately a small, explicit mapping rather
 * than Compose Navigation's declarative deepLinks{} DSL — that DSL binds a
 * URI pattern to ONE destination at graph-definition time, which doesn't fit
 * well with the app's role-gated destinations here; this function is called
 * once, after the graph exists, and just calls navController.navigate(...)
 * like any other programmatic navigation.
 *
 * Supported formats (documented so backend/Edge Function notification
 * payloads know what `deep_link` values are meaningful):
 *   rentease://listing/{slug}          -> Student listing detail
 *   rentease://landlord/bookings        -> Landlord booking requests
 *   rentease://landlord/maintenance     -> Landlord maintenance
 *   rentease://landlord/messages/{id}   -> Landlord conversation thread
 *   rentease://admin-access             -> Admin access screen
 * Unrecognized URIs are silently ignored rather than crashing.
 */
private fun NavHostController.navigateToDeepLink(rawUri: String) {
    val uri = runCatching { android.net.Uri.parse(rawUri) }.getOrNull() ?: return
    if (uri.scheme != "rentease") return

    // Password-recovery link from the reset email: rentease://reset-password#access_token=...&type=recovery
    PasswordReset.parseRecoveryToken(rawUri)?.let { token ->
        navigate("auth/reset-password?token=${android.net.Uri.encode(token)}")
        return
    }

    val host = uri.host.orEmpty()
    val segments = uri.pathSegments

    val route = when {
        host == "listing" && segments.isNotEmpty() -> "student/listing/${segments[0]}"
        host == "landlord" && segments.getOrNull(0) == "bookings" -> Routes.LANDLORD_BOOKINGS
        host == "landlord" && segments.getOrNull(0) == "maintenance" -> Routes.LANDLORD_MAINTENANCE
        host == "landlord" && segments.getOrNull(0) == "messages" && segments.size > 1 ->
            "landlord/messages/${segments[1]}"
        host == "admin-access" -> Routes.ADMIN_ACCESS
        else -> null
    }

    route?.let { navigate(it) }
}

private fun homeRouteFor(user: AuthUser): String = when (user.role) {
    UserRole.STUDENT -> Routes.STUDENT_HOME
    UserRole.LANDLORD -> Routes.LANDLORD_HOME
    UserRole.ADMIN -> Routes.ADMIN_HOME
}

private fun NavHostController.routeToHome(user: AuthUser) {
    val destination = homeRouteFor(user)
    navigate(destination) {
        // Clear the whole back stack (not just ROLE_SELECT): after the password-reset flow ROLE_SELECT is
        // gone, and Back from the home screen must never land on the login/guest screens again.
        popUpTo(0) { inclusive = true }
    }
}
