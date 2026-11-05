package com.rentease.app.data.repository

import com.rentease.app.data.remote.httpHint
import android.content.Context
import com.rentease.app.R
import com.rentease.app.domain.model.SimilarProperties
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseListingsApi
import com.rentease.app.data.remote.dto.BookingDto
import com.rentease.app.data.remote.dto.BookingUpdateDto
import com.rentease.app.data.remote.dto.FavoriteDto
import com.rentease.app.data.remote.dto.MaintenanceRequestCreateDto
import com.rentease.app.data.remote.dto.PaymentCreateDto
import com.rentease.app.data.remote.dto.PaymentMarkPaidDto
import com.rentease.app.data.remote.dto.PropertyDto
import com.rentease.app.data.remote.dto.ReviewCreateDto
import com.rentease.app.domain.model.BookableRoom
import com.rentease.app.domain.model.BookingRequestInput
import com.rentease.app.domain.model.BookingStatus
import com.rentease.app.domain.model.MaintenancePriority
import com.rentease.app.domain.model.MaintenanceRequestInput
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.model.MaintenanceTicket
import com.rentease.app.domain.model.Payment
import com.rentease.app.domain.model.PaymentKind
import com.rentease.app.domain.model.PaymentStatus
import com.rentease.app.domain.model.PropertyDetail
import com.rentease.app.domain.model.PropertySummary
import com.rentease.app.domain.model.Review
import com.rentease.app.domain.model.ReviewInput
import com.rentease.app.domain.model.SearchFilters
import com.rentease.app.domain.model.StudentBookingSummary
import com.rentease.app.domain.model.University
import com.rentease.app.domain.model.isOpenBookingStatus
import com.rentease.app.domain.repository.ListingsRepository
import com.rentease.app.domain.repository.ListingsResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListingsRepositoryImpl @Inject constructor(
    private val api: SupabaseListingsApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : ListingsRepository {

    override suspend fun getUniversities(): ListingsResult<List<University>> {
        val response = api.getUniversities()
        return if (response.isSuccessful) {
            ListingsResult.Success(
                response.body().orEmpty().map {
                    University(
                        id = it.id, name = it.name, shortName = it.shortName, city = it.city,
                        province = it.province, students = it.students, averageRent = it.averageRent
                    )
                }
            )
        } else {
            ListingsResult.Failure("${context.getString(R.string.error_could_not_load_universities)}${context.httpHint(response.code())}")
        }
    }

    override suspend fun getAvailableRooms(propertyId: String): ListingsResult<List<BookableRoom>> {
        val response = api.getAvailableRooms(propertyIdFilter = "eq.$propertyId")
        if (!response.isSuccessful) {
            return ListingsResult.Failure("${context.getString(R.string.error_could_not_load_rooms)}${context.httpHint(response.code())}")
        }
        return ListingsResult.Success(
            response.body().orEmpty().map {
                BookableRoom(id = it.id, name = it.name, roomType = it.roomType, pricePerMonth = it.pricePerMonth)
            }
        )
    }

    override suspend fun search(filters: SearchFilters, page: Int, pageSize: Int): ListingsResult<List<PropertySummary>> {
        val queryParams = buildQueryParams(filters)
        val from = page * pageSize
        val to = from + pageSize - 1

        val response = api.searchProperties(filters = queryParams, range = "$from-$to")
        if (!response.isSuccessful) {
            return ListingsResult.Failure("${context.getString(R.string.error_search_failed)}${context.httpHint(response.code())}")
        }
        val properties = response.body().orEmpty()

        // Resolve which of these are already favourited by the current student,
        // so the heart icon renders correctly without a second screen-level call.
        val favouriteIds = tokenManager.getUserId()?.let { userId ->
            val favResponse = api.getFavoriteIds(studentIdFilter = "eq.$userId")
            if (favResponse.isSuccessful) favResponse.body().orEmpty().map { it.propertyId }.toSet() else emptySet()
        } ?: emptySet()

        return ListingsResult.Success(properties.map { it.toSummary(isFavourited = it.id in favouriteIds) })
    }

    override suspend fun getSimilar(property: PropertySummary): ListingsResult<List<PropertySummary>> {
        val params = mapOf(
            "status" to "eq.published",
            "city" to "eq.${property.city}",
            "id" to "neq.${property.id}"
        )
        val response = api.searchProperties(filters = params, range = "0-${SimilarProperties.LIMIT * 3 - 1}")
        if (!response.isSuccessful) {
            return ListingsResult.Failure("${context.getString(R.string.error_search_failed)}${context.httpHint(response.code())}")
        }
        val candidates = response.body().orEmpty().map { it.toSummary(isFavourited = false) }
        return ListingsResult.Success(SimilarProperties.rank(property, candidates))
    }

    override suspend fun getPropertyDetail(slug: String): ListingsResult<PropertyDetail> {
        val propertyResponse = api.getPropertyBySlug(slugFilter = "eq.$slug")
        val property = propertyResponse.takeIf { it.isSuccessful }?.body()?.firstOrNull()
            ?: return ListingsResult.Failure(context.getString(R.string.error_listing_not_found))

        val imagesResponse = api.getPropertyImages(propertyIdFilter = "eq.${property.id}")
        val images = if (imagesResponse.isSuccessful) imagesResponse.body().orEmpty().map { it.url } else emptyList()

        val reviewsResponse = api.getReviews(propertyIdFilter = "eq.${property.id}")
        val reviews = if (reviewsResponse.isSuccessful) {
            reviewsResponse.body().orEmpty().map { Review(id = it.id, authorName = it.authorName, rating = it.rating, title = it.title, body = it.body) }
        } else emptyList()

        val favouriteIds = tokenManager.getUserId()?.let { userId ->
            val favResponse = api.getFavoriteIds(studentIdFilter = "eq.$userId")
            if (favResponse.isSuccessful) favResponse.body().orEmpty().map { it.propertyId }.toSet() else emptySet()
        } ?: emptySet()

        return ListingsResult.Success(
            PropertyDetail(
                summary = property.toSummary(isFavourited = property.id in favouriteIds),
                description = property.description,
                address = property.address,
                deposit = property.deposit,
                amenities = property.amenities,
                latitude = property.latitude,
                longitude = property.longitude,
                landlordName = property.landlordName,
                images = images.ifEmpty { listOfNotNull(property.coverImage) },
                reviews = reviews,
                landlordId = property.landlordId
            )
        )
    }

    override suspend fun toggleFavourite(propertyId: String, isCurrentlyFavourited: Boolean): ListingsResult<Boolean> {
        val userId = tokenManager.getUserId()
            ?: return ListingsResult.Failure(context.getString(R.string.error_signin_required_save_listings))

        val response = if (isCurrentlyFavourited) {
            api.removeFavorite(studentIdFilter = "eq.$userId", propertyIdFilter = "eq.$propertyId")
        } else {
            api.addFavorite(FavoriteDto(studentId = userId, propertyId = propertyId))
        }

        return if (response.isSuccessful) {
            ListingsResult.Success(!isCurrentlyFavourited)
        } else {
            ListingsResult.Failure("${context.getString(R.string.error_could_not_update_favourites)}${context.httpHint(response.code())}")
        }
    }

    override suspend fun createBookingRequest(input: BookingRequestInput): ListingsResult<String> {
        val userId = tokenManager.getUserId()
            ?: return ListingsResult.Failure(context.getString(R.string.error_signin_required_booking))

        // Duplicate guard: a student may hold only one open (pending/approved/active) request
        // per property. If one exists but its deposit row is missing — i.e. an earlier attempt
        // created the booking and then failed on the payment — repair it instead of creating a
        // second booking, so retries never produce duplicate booking or payment records.
        val existingBookings = api.getBookingsForStudent(studentIdFilter = "eq.$userId")
        if (existingBookings.isSuccessful) {
            val open = existingBookings.body().orEmpty()
                .firstOrNull { it.propertyId == input.propertyId && isOpenBookingStatus(it.status) }
            if (open != null) {
                val openId = open.id
                val openRef = open.reference ?: openId
                if (openId != null && openRef != null && !hasDepositPayment(userId, openId)) {
                    val repaired = createDepositPayment(openId, userId, open.deposit, open.moveInDate)
                    if (!repaired.isSuccessful) {
                        return ListingsResult.Failure("${context.getString(R.string.error_booking_deposit_failed)}${context.httpHint(repaired.code())}")
                    }
                    return ListingsResult.Success(openRef)
                }
                return ListingsResult.Failure(context.getString(R.string.error_booking_duplicate))
            }
        }

        val response = api.createBooking(
            BookingDto(
                studentId = userId,
                propertyId = input.propertyId,
                roomId = input.roomId,
                landlordId = input.landlordId,
                moveInDate = input.moveInDate,
                leaseMonths = input.leaseMonths,
                monthlyRent = input.monthlyRent,
                deposit = input.deposit,
                message = input.message,
                idDocumentUrl = input.idDocumentUrl,
                proofOfRegistrationUrl = input.proofOfRegistrationUrl
            )
        )

        val bookingResult = response.takeIf { it.isSuccessful }?.body()?.firstOrNull()
        val displayReference = bookingResult?.reference ?: bookingResult?.id
        if (displayReference == null) {
            return ListingsResult.Failure("${context.getString(R.string.error_booking_request_failed)}${context.httpHint(response.code())}")
        }

        // Auto-create the deposit payment record, same two-insert flow as `booking-dialog.tsx`.
        // The web app treats a failed second insert as an overall failure (`if (paymentError)
        // throw paymentError`); mirrored here. A retry is safe thanks to the guard above.
        val bookingId = bookingResult?.id
        if (bookingId != null) {
            val paymentResponse = createDepositPayment(bookingId, userId, input.deposit, input.moveInDate)
            if (!paymentResponse.isSuccessful) {
                return ListingsResult.Failure("${context.getString(R.string.error_booking_deposit_failed)}${context.httpHint(paymentResponse.code())}")
            }
        }

        return ListingsResult.Success(displayReference)
    }

    private suspend fun hasDepositPayment(userId: String, bookingId: String): Boolean {
        val payments = api.getPaymentsForStudent(studentIdFilter = "eq.$userId")
        if (!payments.isSuccessful) return true // can't tell — never risk a duplicate payment row
        return payments.body().orEmpty().any { it.bookingId == bookingId && it.kind == "deposit" }
    }

    private suspend fun createDepositPayment(bookingId: String, userId: String, amount: Double, dueDate: String) =
        api.createPayment(PaymentCreateDto(bookingId = bookingId, studentId = userId, amount = amount, dueDate = dueDate))

    override suspend fun getMyBookings(): ListingsResult<List<StudentBookingSummary>> {
        val userId = tokenManager.getUserId() ?: return ListingsResult.Failure(context.getString(R.string.error_signin_required_view_bookings))

        val response = api.getBookingsForStudent(studentIdFilter = "eq.$userId")
        if (!response.isSuccessful) return ListingsResult.Failure("${context.getString(R.string.error_could_not_load_my_bookings)}${context.httpHint(response.code())}")

        val bookings = response.body().orEmpty()
        if (bookings.isEmpty()) return ListingsResult.Success(emptyList())

        val propertyIds = bookings.map { it.propertyId }.distinct()
        val titlesResponse = api.getPropertyTitlesById(idInFilter = "in.(${propertyIds.joinToString(",")})")
        val titleById = titlesResponse.takeIf { it.isSuccessful }?.body().orEmpty().associate { it.id to (it.title ?: context.getString(R.string.fallback_property_name)) }

        return ListingsResult.Success(
            bookings.map { dto ->
                StudentBookingSummary(
                    id = dto.id.orEmpty(),
                    reference = dto.reference ?: dto.id.orEmpty().take(8).uppercase(),
                    propertyId = dto.propertyId,
                    propertyTitle = titleById[dto.propertyId] ?: context.getString(R.string.fallback_property_name),
                    moveInDate = dto.moveInDate,
                    leaseMonths = dto.leaseMonths,
                    monthlyRent = dto.monthlyRent,
                    deposit = dto.deposit,
                    status = BookingStatus.fromDb(dto.status)
                )
            }
        )
    }

    override suspend fun cancelBooking(bookingId: String): ListingsResult<Unit> {
        tokenManager.getUserId() ?: return ListingsResult.Failure(context.getString(R.string.error_signin_required_view_bookings))

        val response = api.cancelBooking(idFilter = "eq.$bookingId", body = BookingUpdateDto(status = "cancelled"))
        return if (response.isSuccessful) {
            ListingsResult.Success(Unit)
        } else {
            ListingsResult.Failure("${context.getString(R.string.error_cancel_booking_failed)}${context.httpHint(response.code())}")
        }
    }

    override suspend fun createReview(input: ReviewInput): ListingsResult<Unit> {
        val userId = tokenManager.getUserId()
            ?: return ListingsResult.Failure(context.getString(R.string.error_not_signed_in))

        val response = api.createReview(
            ReviewCreateDto(
                propertyId = input.propertyId,
                bookingId = input.bookingId,
                studentId = userId,
                authorName = input.authorName,
                rating = input.rating,
                title = input.title,
                body = input.body
            )
        )

        return when {
            response.isSuccessful -> ListingsResult.Success(Unit)
            // Unique index on reviews.booking_id -> Postgres unique violation -> HTTP 409.
            response.code() == 409 -> ListingsResult.Failure(context.getString(R.string.error_review_already_submitted))
            else -> ListingsResult.Failure("${context.getString(R.string.error_review_submit_failed)}${context.httpHint(response.code())}")
        }
    }

    override suspend fun getMyReviewedBookingIds(): ListingsResult<Set<String>> {
        val userId = tokenManager.getUserId()
            ?: return ListingsResult.Failure(context.getString(R.string.error_not_signed_in))

        val response = api.getMyReviewedBookings(studentIdFilter = "eq.$userId")
        if (!response.isSuccessful) {
            return ListingsResult.Failure("${context.getString(R.string.error_could_not_load_my_bookings)}${context.httpHint(response.code())}")
        }
        return ListingsResult.Success(response.body().orEmpty().mapNotNull { it.bookingId }.toSet())
    }

    /**
     * Student's own maintenance requests (Part 1 §3.9). Reuses the landlord-side
     * [MaintenanceTicket] domain model for consistency — [MaintenanceTicket.studentId]/
     * [MaintenanceTicket.studentName] are left blank here since a student viewing
     * their own requests never needs to display their own name back to themselves.
     */
    override suspend fun getMyMaintenanceRequests(): ListingsResult<List<MaintenanceTicket>> {
        val userId = tokenManager.getUserId()
            ?: return ListingsResult.Failure(context.getString(R.string.error_signin_required_view_bookings))

        val response = api.getMaintenanceForStudent(studentIdFilter = "eq.$userId")
        if (!response.isSuccessful) {
            return ListingsResult.Failure("${context.getString(R.string.error_could_not_load_maintenance_tickets)}${context.httpHint(response.code())}")
        }

        val tickets = response.body().orEmpty()
        if (tickets.isEmpty()) return ListingsResult.Success(emptyList())

        val propertyIds = tickets.map { it.propertyId }.distinct()
        val titlesResponse = api.getPropertyTitlesById(idInFilter = "in.(${propertyIds.joinToString(",")})")
        val titleById = titlesResponse.takeIf { it.isSuccessful }?.body().orEmpty()
            .associate { it.id to (it.title ?: context.getString(R.string.fallback_property_name)) }

        return ListingsResult.Success(
            tickets.map { dto ->
                MaintenanceTicket(
                    id = dto.id,
                    propertyId = dto.propertyId,
                    propertyTitle = titleById[dto.propertyId] ?: context.getString(R.string.fallback_property_name),
                    studentId = dto.studentId,
                    studentName = "",
                    title = dto.title,
                    description = dto.description,
                    category = dto.category,
                    priority = MaintenancePriority.fromDb(dto.priority),
                    status = MaintenanceStatus.fromDb(dto.status),
                    photoUrl = dto.photoUrl,
                    createdAt = dto.createdAt
                )
            }
        )
    }

    override suspend fun createMaintenanceRequest(input: MaintenanceRequestInput): ListingsResult<Unit> {
        val userId = tokenManager.getUserId()
            ?: return ListingsResult.Failure(context.getString(R.string.error_signin_required_booking))

        val response = api.createMaintenanceRequest(
            MaintenanceRequestCreateDto(
                studentId = userId,
                propertyId = input.propertyId,
                bookingId = input.bookingId,
                title = input.title,
                description = input.description,
                category = input.category,
                priority = input.priority.toDb(),
                photoUrl = input.photoUrl
            )
        )

        return if (response.isSuccessful) {
            ListingsResult.Success(Unit)
        } else {
            ListingsResult.Failure("${context.getString(R.string.error_maintenance_submit_failed)}${context.httpHint(response.code())}")
        }
    }

    override suspend fun getMyPayments(): ListingsResult<List<Payment>> {
        val userId = tokenManager.getUserId()
            ?: return ListingsResult.Failure(context.getString(R.string.error_not_signed_in))

        val response = api.getPaymentsForStudent(studentIdFilter = "eq.$userId")
        if (!response.isSuccessful) {
            return ListingsResult.Failure("${context.getString(R.string.error_could_not_load_my_payments)}${context.httpHint(response.code())}")
        }

        val payments = response.body().orEmpty()
        if (payments.isEmpty()) return ListingsResult.Success(emptyList())

        // Payments only carry booking_id, not property_id — reuse getMyBookings()'s
        // already-resolved property titles instead of a third round-trip.
        val bookingTitles = (getMyBookings() as? ListingsResult.Success)?.data
            ?.associate { it.id to it.propertyTitle }
            .orEmpty()

        return ListingsResult.Success(
            payments.map { dto ->
                Payment(
                    id = dto.id,
                    reference = dto.reference,
                    propertyTitle = bookingTitles[dto.bookingId] ?: context.getString(R.string.fallback_property_name),
                    kind = PaymentKind.fromDb(dto.kind),
                    status = PaymentStatus.fromDb(dto.status),
                    amount = dto.amount,
                    dueDate = dto.dueDate,
                    paidAt = dto.paidAt,
                    createdAt = dto.createdAt
                )
            }
        )
    }

    override suspend fun markPaymentPaid(paymentId: String): ListingsResult<Unit> {
        tokenManager.getUserId() ?: return ListingsResult.Failure(context.getString(R.string.error_not_signed_in))

        val response = api.markPaymentPaid(
            idFilter = "eq.$paymentId",
            body = PaymentMarkPaidDto(paidAt = Instant.now().toString())
        )
        return if (response.isSuccessful) {
            ListingsResult.Success(Unit)
        } else {
            ListingsResult.Failure("${context.getString(R.string.error_mark_payment_paid_failed)}${context.httpHint(response.code())}")
        }
    }

    /**
     * Translates [SearchFilters] into PostgREST filter query params.
     * `status=eq.published` is always applied — students must never see
     * pending/rejected properties (admin-moderation workflow, §3.10).
     */
    private fun buildQueryParams(filters: SearchFilters): Map<String, String> {
        val params = mutableMapOf("status" to "eq.published")

        if (filters.keyword.isNotBlank()) {
            params["title"] = "ilike.*${filters.keyword.trim()}*"
        }
        filters.universityId?.let { params["university_id"] = "eq.$it" }
        filters.province?.let { params["province"] = "eq.$it" }
        filters.maxRent?.let { params["price_per_month"] = "lte.$it" }
        if (filters.furnishedOnly) params["furnished"] = "eq.true"
        if (filters.availableOnly) params["rooms_available"] = "gt.0"
        if (filters.amenities.isNotEmpty()) {
            // PostgREST array "contains" filter: amenities=cs.{WiFi,Parking}
            params["amenities"] = "cs.{${filters.amenities.joinToString(",")}}"
        }
        return params
    }

    private fun PropertyDto.toSummary(isFavourited: Boolean) = PropertySummary(
        id = id,
        slug = slug,
        title = title,
        city = city,
        province = province,
        distanceKm = distanceKm,
        pricePerMonth = pricePerMonth,
        bedrooms = bedrooms,
        bathrooms = bathrooms,
        roomsAvailable = roomsAvailable,
        furnished = furnished,
        verified = verified,
        rating = rating,
        reviewCount = reviewCount,
        coverImageUrl = coverImage,
        isFavourited = isFavourited
    )
}
