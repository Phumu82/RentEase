package com.rentease.app.presentation.navigation

/** Central route definitions. Role-gated destinations (student/..., landlord/..., admin/...)
 *  are only reachable once RentEaseNavGraph has resolved AuthRepository.currentUser. */
object Routes {
    const val SPLASH = "splash"
    const val ROLE_SELECT = "auth/role_select"
    const val LOGIN = "auth/login"
    const val FORGOT_PASSWORD = "auth/forgot-password"
    const val RESET_PASSWORD_PATTERN = "auth/reset-password?token={token}"

    // Signed-out browsing of published listings (detail reuses student/listing/{slug}).
    const val GUEST_BROWSE = "guest/browse"
    const val GUEST_BROWSE_PATTERN = "guest/browse?university={university}&keyword={keyword}"

    // Public info pages (no sign-in needed), mirroring the web's marketing routes.
    const val INFO_HUB = "info"
    const val INFO_UNIVERSITIES = "info/universities"
    const val INFO_UNIVERSITY = "info/universities/{universityId}"
    const val INFO_ABOUT = "info/about"
    const val INFO_HOW_IT_WORKS = "info/how-it-works"
    const val INFO_SERVICES = "info/services"
    const val INFO_PRICING = "info/pricing"
    const val INFO_FAQ = "info/faq"
    const val INFO_CONTACT = "info/contact"
    const val INFO_PRIVACY = "info/privacy"
    const val INFO_TERMS = "info/terms"
    const val REGISTER_STUDENT = "auth/register/student"
    const val REGISTER_LANDLORD = "auth/register/landlord"

    // Hidden route — never linked from any visible UI, matching web's /admin-access.
    const val ADMIN_ACCESS = "auth/admin_access"

    const val STUDENT_HOME = "student/home"
    const val STUDENT_SEARCH = "student/search"
    const val STUDENT_SAVED = "student/saved"
    const val STUDENT_BOOKINGS = "student/bookings"
    const val STUDENT_NOTIFICATIONS = "student/notifications"
    const val STUDENT_MESSAGES = "student/messages"
    const val STUDENT_THREAD = "student/messages/{conversationId}"
    const val STUDENT_COMPARE = "student/compare/{slugs}"
    const val STUDENT_MAINTENANCE = "student/maintenance"
    const val STUDENT_PAYMENTS = "student/payments"
    const val STUDENT_ONBOARDING = "student/onboarding"
    const val LANDLORD_ONBOARDING = "landlord/onboarding"
    const val SETTINGS = "settings"

    // Shared (role-agnostic) — reachable from both student and landlord home.
    const val SUPPORT = "support"
    const val LANDLORD_HOME = "landlord/home"
    const val ADMIN_HOME = "admin/home"

    // Landlord sub-routes
    const val LANDLORD_PROPERTIES = "landlord/properties"
    const val LANDLORD_PROPERTY_ADD = "landlord/properties/add"
    const val LANDLORD_PROPERTY_EDIT = "landlord/properties/{propertyId}/edit"
    const val LANDLORD_ROOMS = "landlord/properties/{propertyId}/rooms"
    const val LANDLORD_BOOKINGS = "landlord/bookings"
    const val LANDLORD_TENANTS = "landlord/tenants"
    const val LANDLORD_MAINTENANCE = "landlord/maintenance"
    const val LANDLORD_MESSAGES = "landlord/messages"
    const val LANDLORD_THREAD = "landlord/messages/{conversationId}"
    const val LANDLORD_NOTIFICATIONS = "landlord/notifications"
    const val LANDLORD_ANALYTICS = "landlord/analytics"

    // Admin sub-routes
    const val ADMIN_VERIFICATIONS = "admin/verifications"
    const val ADMIN_MODERATION = "admin/moderation"
    const val ADMIN_USERS = "admin/users"
    const val ADMIN_BOOKINGS = "admin/bookings"
    const val ADMIN_AUDIT_LOG = "admin/audit-log"
    const val ADMIN_COMPLAINTS = "admin/complaints"
}
