package com.rentease.app.domain.model

// ---------- Landlord verification queue ----------

/** One uploaded verification document: which slot it is and its private storage path. */
data class VerificationDocument(val kind: LandlordDocumentKind, val path: String)

data class LandlordVerificationItem(
    val userId: String,
    val fullName: String,
    val email: String?,
    val businessName: String?,
    val idDocumentUrl: String?,
    val proofOfOwnershipUrl: String?,
    val status: VerificationStatus,
    val legalName: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val registrationNumber: String? = null,
    val notes: String? = null,
    /** All uploaded documents (required and optional), in slot order. */
    val documents: List<VerificationDocument> = emptyList()
)

// ---------- Property moderation queue ----------

data class PropertyModerationItem(
    val id: String,
    val title: String,
    val landlordName: String,
    val city: String,
    val province: String,
    val pricePerMonth: Double,
    val coverImageUrl: String?,
    val status: PropertyStatus
)

// ---------- User management ----------

data class AdminUserItem(
    val id: String,
    val fullName: String,
    val email: String?,
    val role: UserRole,
    val isSuspended: Boolean
)

// ---------- Booking oversight (platform-wide, read-only for admin) ----------

data class AdminBookingItem(
    val id: String,
    val reference: String,
    val studentName: String,
    val propertyTitle: String,
    val status: BookingStatus,
    val monthlyRent: Double
)

// ---------- Audit log ----------

data class AuditLogEntry(
    val id: String,
    val actorName: String?,
    val action: String,
    val entity: String,
    val entityId: String?,
    val createdAt: String
)

// ---------- Dashboard summary ----------

data class AdminDashboardStats(
    val pendingLandlordVerifications: Int,
    val pendingPropertyModerations: Int,
    val totalStudents: Int,
    val totalLandlords: Int,
    val suspendedUsers: Int,
    val openMaintenanceTickets: Int
)
