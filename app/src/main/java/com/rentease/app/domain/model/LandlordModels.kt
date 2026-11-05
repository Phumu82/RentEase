package com.rentease.app.domain.model

// ---------- Verification ----------

enum class VerificationStatus {
    PENDING, SUBMITTED, VERIFIED, REJECTED, DOCUMENTS_REQUIRED;
    companion object {
        fun fromDb(value: String): VerificationStatus = valueOf(value.uppercase())
    }
}

data class LandlordProfile(
    val userId: String,
    val legalName: String?,
    val businessName: String?,
    val verificationStatus: VerificationStatus,
    val onboardingComplete: Boolean
)

// ---------- Property management (landlord's own view of a property) ----------

enum class PropertyStatus {
    DRAFT, PENDING, PUBLISHED, REJECTED, ARCHIVED;
    companion object {
        fun fromDb(value: String): PropertyStatus = valueOf(value.uppercase())
    }
    fun toDb(): String = name.lowercase()
}

data class LandlordProperty(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val address: String,
    val city: String,
    val province: String,
    val universityId: String?,
    val distanceKm: Double,
    val pricePerMonth: Double,
    val deposit: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val roomsAvailable: Int,
    val furnished: Boolean,
    val amenities: List<String>,
    val coverImageUrl: String?,
    val images: List<String>,
    val status: PropertyStatus,
    val verified: Boolean,
    val rating: Double,
    val reviewCount: Int
)

/** Create/update payload — a subset of LandlordProperty the landlord actually edits. */
data class PropertyInput(
    val title: String,
    val description: String,
    val address: String,
    val city: String,
    val province: String,
    val universityId: String?,
    /** Walking/driving distance to the campus in km (shown as "x km from campus"). */
    val distanceKm: Double,
    val pricePerMonth: Double,
    val deposit: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val furnished: Boolean,
    val amenities: List<String>,
    val coverImageUrl: String?,
    /** Rooms still open for booking; the repository keeps it in step with the property's rooms. */
    val roomsAvailable: Int = 1
)

// ---------- Rooms ----------

enum class RoomStatus {
    AVAILABLE, RESERVED, OCCUPIED, MAINTENANCE;
    companion object {
        fun fromDb(value: String): RoomStatus = valueOf(value.uppercase())
    }
    fun toDb(): String = name.lowercase()
}

data class RentalRoom(
    val id: String,
    val propertyId: String,
    val name: String,
    val roomType: String,
    val pricePerMonth: Double,
    val capacity: Int,
    val status: RoomStatus
)

data class RoomInput(
    val name: String,
    val roomType: String,
    val pricePerMonth: Double,
    val capacity: Int,
    val status: RoomStatus
)

// ---------- Bookings, from the landlord's side ----------

data class TenantBooking(
    val id: String,
    val reference: String,
    val studentId: String,
    val studentName: String,
    val propertyId: String,
    val propertyTitle: String,
    val roomId: String?,
    val roomName: String?,
    val moveInDate: String,
    val leaseMonths: Int,
    val monthlyRent: Double,
    val deposit: Double,
    val status: BookingStatus,
    val message: String?,
    val idDocumentUrl: String?,
    val proofOfRegistrationUrl: String?
)

// ---------- Maintenance ----------

enum class MaintenancePriority {
    LOW, MEDIUM, HIGH, URGENT;
    companion object {
        fun fromDb(value: String): MaintenancePriority = valueOf(value.uppercase())
    }
    fun toDb(): String = name.lowercase()
}

enum class MaintenanceStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED;
    companion object {
        fun fromDb(value: String): MaintenanceStatus = when (value.lowercase()) {
            "open" -> OPEN
            "in_progress" -> IN_PROGRESS
            "resolved" -> RESOLVED
            "closed" -> CLOSED
            else -> throw IllegalArgumentException("Unknown maintenance status: $value")
        }
    }
    fun toDb(): String = name.lowercase()
}

data class MaintenanceTicket(
    val id: String,
    val propertyId: String,
    val propertyTitle: String,
    val studentId: String,
    val studentName: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: MaintenancePriority,
    val status: MaintenanceStatus,
    val photoUrl: String?,
    val createdAt: String
)

/** Input for a student logging a new maintenance issue (Part 1 §3.9 /
 *  student/maintenance). [bookingId] is optional — the DB column allows NULL —
 *  but is included whenever the student picked one of their own bookings, to
 *  match the web app's flow and give the landlord booking context for free. */
data class MaintenanceRequestInput(
    val propertyId: String,
    val bookingId: String?,
    val title: String,
    val description: String,
    val category: String,
    val priority: MaintenancePriority,
    val photoUrl: String?
)

// ---------- Messaging ----------

data class ConversationSummary(
    val id: String,
    val studentId: String,
    val landlordId: String,
    val propertyId: String?,
    val counterpartName: String,
    val subject: String,
    val lastMessageAt: String,
    val unreadCount: Int
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val createdAt: String,
    val readAt: String?
)

// ---------- Notifications ----------

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val link: String?,
    val readAt: String?,
    val createdAt: String
)

// ---------- Analytics ----------

data class LandlordAnalytics(
    val totalProperties: Int,
    val totalRoomsAvailable: Int,
    val occupiedRoomsEstimate: Int,
    val occupancyRatePercent: Double,
    /** Monthly rent of approved/active bookings (the website's "Contracted monthly income"). */
    val contractedMonthlyIncome: Double,
    /** Sum of `payments` rows with `status = 'paid'` for this landlord's bookings (the website's "collected"). */
    val collectedToDate: Double,
    val incomeByProperty: List<PropertyIncome> = emptyList(),
    val pendingBookingRequests: Int,
    val openMaintenanceTickets: Int
)
