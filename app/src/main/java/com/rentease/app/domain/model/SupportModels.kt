package com.rentease.app.domain.model

/**
 * A complaint raised by a student or landlord (Part 1 §3.10, `/support` — a
 * single role-agnostic route in the web app, not split by role: `reporter_id`
 * has no role constraint, RLS just checks `reporter_id = auth.uid()`).
 *
 * Reuses [MaintenanceStatus] rather than a new enum: the web app's
 * `complaints.status` lifecycle (open/in_progress/resolved/closed) is
 * byte-for-byte the same 4 values as `maintenance_requests.status`.
 *
 * [reporterName]/[reporterEmail] are only ever populated on the admin queue
 * (joined client-side from `adminApi.getAllProfiles()`, the same batch-lookup
 * pattern [com.rentease.app.data.repository.AdminRepositoryImpl.getAllUsers]
 * already uses); a student/landlord viewing their own complaints has no need
 * to see their own name echoed back, so those stay null there.
 */
data class Complaint(
    val id: String,
    val subject: String,
    val category: String,
    val description: String,
    val status: MaintenanceStatus,
    val adminResponse: String?,
    val createdAt: String,
    val reporterName: String? = null,
    val reporterEmail: String? = null
)

/** Input for a student or landlord raising a new complaint. */
data class ComplaintInput(
    val subject: String,
    val category: String,
    val description: String
)

/** Mirrors the web app's fixed category list (`support.tsx`'s `categories`) exactly. */
object ComplaintCategoryCatalog {
    val ALL = listOf(
        "Booking", "Payment", "Property", "Landlord conduct", "Student conduct", "Technical", "Other"
    )
}
