package com.rentease.app.domain.model

data class University(
    val id: String,
    val name: String,
    val shortName: String,
    val city: String,
    val province: String,
    val students: Int = 0,
    val averageRent: Double = 0.0
)

data class PropertySummary(
    val id: String,
    val slug: String,
    val title: String,
    val city: String,
    val province: String,
    val distanceKm: Double,
    val pricePerMonth: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val roomsAvailable: Int,
    val furnished: Boolean,
    val verified: Boolean,
    val rating: Double,
    val reviewCount: Int,
    val coverImageUrl: String?,
    val isFavourited: Boolean = false
)

data class PropertyDetail(
    val summary: PropertySummary,
    val description: String,
    val address: String,
    val deposit: Double,
    val amenities: List<String>,
    val latitude: Double?,
    val longitude: Double?,
    val landlordName: String,
    val images: List<String>,
    val reviews: List<Review>,
    /** Owner's profile id; copied onto the booking row like the web does. */
    val landlordId: String? = null
)

data class Review(
    val id: String,
    val authorName: String,
    val rating: Int,
    val title: String,
    val body: String
)

/** A student's review of a property, tied to one of their own bookings. The
 *  `reviews` table has a unique index on `booking_id` (added in a later
 *  migration), so each booking can be reviewed at most once. */
data class ReviewInput(
    val propertyId: String,
    val bookingId: String,
    val authorName: String,
    val rating: Int,
    val title: String,
    val body: String
)

/** Mirrors the Postgres `booking_status` enum exactly (see audit note: it's
 *  `approved`, not `accepted`, unlike the prose in Part 1 §3.5). */
enum class BookingStatus {
    PENDING, APPROVED, REJECTED, CANCELLED, ACTIVE, COMPLETED;

    companion object {
        fun fromDb(value: String): BookingStatus = valueOf(value.uppercase())
    }
    fun toDb(): String = name.lowercase()
}

/** Mirrors the web app's exact sort choices (`listings.index.tsx`'s `sort`
 *  search param: newest/price-asc/price-desc/rating/distance). Sorting is
 *  applied client-side over whatever page(s) have already been fetched —
 *  same approach the web app uses — so it needs no new backend endpoint;
 *  "newest" simply keeps the API's default `created_at.desc` order. */
enum class PropertySortOption {
    NEWEST, PRICE_ASC, PRICE_DESC, RATING, DISTANCE
}

data class SearchFilters(
    val keyword: String = "",
    val universityId: String? = null,
    val province: String? = null,
    val maxRent: Double? = null,
    val furnishedOnly: Boolean = false,
    /** Only listings with at least one room still available (web "Available now"). */
    val availableOnly: Boolean = false,
    val amenities: Set<String> = emptySet()
)

/** The 5-province SEARCH subset (web `src/data/marketing.ts` PROVINCES), used for
 *  the listings province filter. Profile/onboarding forms use [LocationCatalog],
 *  which carries all nine provinces like the web's `src/data/locations.ts`. */
object ProvinceCatalog {
    val ALL = listOf("Gauteng", "Western Cape", "KwaZulu-Natal", "Eastern Cape", "Free State")
}

/** Amenity set mirrors the filter list from Part 1 §3.4 exactly. */
object AmenityCatalog {
    val ALL = listOf(
        "WiFi", "Parking", "Laundry", "Kitchen", "24/7 Security", "Furnished",
        "Study Room", "Backup Power", "Water Included", "Cleaning Service",
        "Gym", "Shuttle Service"
    )
}

data class BookingRequestInput(
    val propertyId: String,
    val moveInDate: String, // ISO yyyy-MM-dd
    val leaseMonths: Int,
    val monthlyRent: Double,
    val deposit: Double,
    val message: String?,
    /** Private-bucket object PATHS (bucket `booking-documents`), despite the legacy "Url" names. */
    val idDocumentUrl: String,
    val proofOfRegistrationUrl: String,
    val roomId: String? = null,
    val landlordId: String? = null
)

/** A room the student can choose while booking. */
data class BookableRoom(
    val id: String,
    val name: String,
    val roomType: String,
    val pricePerMonth: Double
)

/** Lease lengths offered by the web booking dialog. */
object LeaseOptions {
    val MONTHS = listOf(6, 9, 12, 24)
    const val DEFAULT_MONTHS = 12
}

/** Rent and deposit shown in the booking sheet; a chosen room's price overrides the property's. */
data class BookingQuote(val monthlyRent: Double, val deposit: Double) {
    companion object {
        fun of(propertyRent: Double, deposit: Double, room: BookableRoom?): BookingQuote =
            BookingQuote(room?.pricePerMonth ?: propertyRent, deposit)
    }
}

/** Booking states in which a student must not file another request for the same property. */
private val OPEN_BOOKING_STATUSES = setOf("pending", "approved", "active")
fun isOpenBookingStatus(status: String): Boolean = status.lowercase() in OPEN_BOOKING_STATUSES

/** Student's own view of one of their bookings — deliberately a distinct,
 *  simpler shape from TenantBooking (the landlord's view of the same
 *  underlying row), since a student never needs the landlord-side fields
 *  (assignable rooms list, etc.). */
data class StudentBookingSummary(
    val id: String,
    val reference: String,
    val propertyId: String,
    val propertyTitle: String,
    val moveInDate: String,
    val leaseMonths: Int,
    val monthlyRent: Double,
    val deposit: Double,
    val status: BookingStatus
)

/** Province → cities lookup mirroring the web's `src/data/locations.ts` exactly
 *  (all nine provinces), used by student/landlord onboarding forms. */
object LocationCatalog {
    val CITIES_BY_PROVINCE: Map<String, List<String>> = linkedMapOf(
        "Gauteng" to listOf("Johannesburg", "Braamfontein", "Pretoria", "Soweto", "Vanderbijlpark", "Midrand", "Benoni"),
        "Western Cape" to listOf("Cape Town", "Stellenbosch", "Bellville", "Paarl", "George"),
        "KwaZulu-Natal" to listOf("Durban", "Pietermaritzburg", "Westville", "Empangeni"),
        "Eastern Cape" to listOf("Port Elizabeth", "East London", "Mthatha", "Alice"),
        "Free State" to listOf("Bloemfontein", "Welkom", "Qwaqwa"),
        "Limpopo" to listOf("Polokwane", "Thohoyandou", "Tzaneen", "Makhado"),
        "Mpumalanga" to listOf("Mbombela", "Emalahleni", "Secunda"),
        "North West" to listOf("Potchefstroom", "Mahikeng", "Rustenburg"),
        "Northern Cape" to listOf("Kimberley", "Upington")
    )

    val PROVINCES: List<String> = CITIES_BY_PROVINCE.keys.toList()

    fun citiesFor(province: String?): List<String> = CITIES_BY_PROVINCE[province].orEmpty()
}

/** Room-type choices for the student preference field (web `roomTypes`). */
object RoomTypeCatalog {
    val ALL = listOf("Single room", "Shared room", "En-suite", "Bachelor flat", "Any")
}

/**
 * Upload rules shared with the web (`validateDocument` in `src/data/locations.ts`):
 * PDF/JPG/PNG/WEBP, at most 5 MB. Pure so it is unit-testable without Android.
 */
object DocumentRules {
    const val MAX_BYTES: Long = 5L * 1024 * 1024
    val ACCEPTED_MIME_TYPES = listOf("application/pdf", "image/jpeg", "image/png", "image/webp")

    enum class Problem { EMPTY, UNSUPPORTED_TYPE, TOO_LARGE }

    /** Returns null when the file is acceptable, otherwise why not. */
    fun validate(mimeType: String?, sizeBytes: Long): Problem? = when {
        sizeBytes <= 0L -> Problem.EMPTY
        mimeType == null || mimeType !in ACCEPTED_MIME_TYPES -> Problem.UNSUPPORTED_TYPE
        sizeBytes > MAX_BYTES -> Problem.TOO_LARGE
        else -> null
    }
}

/**
 * Object-path builder for the web's private buckets, mirroring `uploadPrivateFile`
 * in `src/lib/storage.ts`: `{userId}/{folder}/{timestamp}-{safeName}`. The leading
 * user-id segment is what the `Own booking documents` storage policy checks.
 */
object StoragePaths {
    private val UNSAFE = Regex("[^a-zA-Z0-9._-]")

    fun safeName(name: String): String = name.replace(UNSAFE, "-").takeLast(80)

    fun privateObjectPath(userId: String, folder: String, fileName: String, nowMillis: Long): String =
        "$userId/$folder/$nowMillis-${safeName(fileName)}"
}
