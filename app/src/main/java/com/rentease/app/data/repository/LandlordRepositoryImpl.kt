package com.rentease.app.data.repository

import com.rentease.app.data.remote.httpHint
import android.content.Context
import androidx.annotation.StringRes
import com.rentease.app.R
import com.rentease.app.domain.model.IncomeReport
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseLandlordApi
import com.rentease.app.data.remote.dto.BookingUpdateDto
import com.rentease.app.data.remote.dto.ConversationDto
import com.rentease.app.data.remote.dto.LandlordBookingDto
import com.rentease.app.data.remote.dto.MaintenanceUpdateDto
import com.rentease.app.data.remote.dto.MessageDto
import com.rentease.app.data.remote.dto.PropertyDto
import com.rentease.app.data.remote.dto.RoomDto
import com.rentease.app.domain.model.AppNotification
import com.rentease.app.domain.model.BookingStatus
import com.rentease.app.domain.model.ChatMessage
import com.rentease.app.domain.model.ConversationSummary
import com.rentease.app.domain.model.LandlordAnalytics
import com.rentease.app.domain.model.LandlordProfile
import com.rentease.app.domain.model.LandlordProperty
import com.rentease.app.domain.model.MaintenancePriority
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.model.MaintenanceTicket
import com.rentease.app.domain.model.PropertyInput
import com.rentease.app.domain.model.PropertyStatus
import com.rentease.app.domain.model.RentalRoom
import com.rentease.app.domain.model.RoomInput
import com.rentease.app.domain.model.RoomStatus
import com.rentease.app.domain.model.TenantBooking
import com.rentease.app.domain.model.VerificationStatus
import com.rentease.app.domain.repository.LandlordRepository
import com.rentease.app.domain.repository.LandlordResult
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Response
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LandlordRepositoryImpl @Inject constructor(
    private val api: SupabaseLandlordApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : LandlordRepository {

    private fun requireUserId(): String? = tokenManager.getUserId()

    override suspend fun getVerificationStatus(): LandlordResult<LandlordProfile> {
        val userId = requireUserId() ?: return LandlordResult.Failure(context.getString(R.string.error_not_signed_in))
        val response = api.getLandlordProfile(userIdFilter = "eq.$userId")
        val dto = response.bodyOrNull()?.firstOrNull()
            ?: return LandlordResult.Failure("${context.getString(R.string.error_landlord_profile_not_found)}${context.httpHint(response.code())}")

        return LandlordResult.Success(
            LandlordProfile(
                userId = dto.userId,
                legalName = dto.legalName,
                businessName = dto.businessName,
                verificationStatus = VerificationStatus.fromDb(dto.verificationStatus),
                onboardingComplete = dto.onboardingComplete
            )
        )
    }

    // ---------------- Properties ----------------

    override suspend fun getMyProperties(): LandlordResult<List<LandlordProperty>> {
        val userId = requireUserId() ?: return LandlordResult.Failure(context.getString(R.string.error_not_signed_in))
        val response = api.getMyProperties(landlordIdFilter = "eq.$userId")
        if (!response.isSuccessful) return LandlordResult.Failure("${context.getString(R.string.error_could_not_load_properties)}${context.httpHint(response.code())}")

        val properties = response.body().orEmpty().map { dto ->
            val images = api.getPropertyImages(propertyIdFilter = "eq.${dto.id}").bodyOrNull().orEmpty().map { it.url }
            dto.toLandlordProperty(images)
        }
        return LandlordResult.Success(properties)
    }

    override suspend fun getProperty(propertyId: String): LandlordResult<LandlordProperty> {
        val response = api.getPropertyById(idFilter = "eq.$propertyId")
        val dto = response.bodyOrNull()?.firstOrNull() ?: return LandlordResult.Failure(context.getString(R.string.error_property_not_found))
        val images = api.getPropertyImages(propertyIdFilter = "eq.$propertyId").bodyOrNull().orEmpty().map { it.url }
        return LandlordResult.Success(dto.toLandlordProperty(images))
    }

    override suspend fun createProperty(input: PropertyInput): LandlordResult<LandlordProperty> {
        val userId = requireUserId() ?: return LandlordResult.Failure(context.getString(R.string.error_not_signed_in))
        val slug = generateSlug(input.title)
        val landlordName = resolveLandlordName(userId)

        val body = mapOf(
            "landlord_id" to userId,
            "landlord_name" to landlordName,
            "university_id" to input.universityId,
            "distance_km" to input.distanceKm,
            "rooms_available" to input.roomsAvailable,
            "slug" to slug,
            "title" to input.title,
            "description" to input.description,
            "address" to input.address,
            "city" to input.city,
            "province" to input.province,
            "price_per_month" to input.pricePerMonth,
            "deposit" to input.deposit,
            "bedrooms" to input.bedrooms,
            "bathrooms" to input.bathrooms,
            "furnished" to input.furnished,
            "amenities" to input.amenities,
            "cover_image" to input.coverImageUrl,
            // New properties start as 'pending' — an admin must approve before
            // students can see them (§3.10 property moderation). This is a
            // deliberate deviation from silently defaulting to 'published'.
            "status" to "pending"
        )

        val response = api.createProperty(body)
        val dto = response.bodyOrNull()?.firstOrNull()
            ?: return LandlordResult.Failure(response.errorMessage(R.string.error_could_not_create_property))
        return LandlordResult.Success(dto.toLandlordProperty(emptyList()))
    }

    override suspend fun updateProperty(propertyId: String, input: PropertyInput): LandlordResult<LandlordProperty> {
        val body = mapOf(
            "university_id" to input.universityId,
            "distance_km" to input.distanceKm,
            "rooms_available" to input.roomsAvailable,
            "title" to input.title,
            "description" to input.description,
            "address" to input.address,
            "city" to input.city,
            "province" to input.province,
            "price_per_month" to input.pricePerMonth,
            "deposit" to input.deposit,
            "bedrooms" to input.bedrooms,
            "bathrooms" to input.bathrooms,
            "furnished" to input.furnished,
            "amenities" to input.amenities,
            "cover_image" to input.coverImageUrl
        )
        val response = api.updateProperty(idFilter = "eq.$propertyId", body = body)
        val dto = response.bodyOrNull()?.firstOrNull()
            ?: return LandlordResult.Failure(response.errorMessage(R.string.error_could_not_update_property))
        val images = api.getPropertyImages(propertyIdFilter = "eq.$propertyId").bodyOrNull().orEmpty().map { it.url }
        return LandlordResult.Success(dto.toLandlordProperty(images))
    }

    override suspend fun deleteProperty(propertyId: String): LandlordResult<Unit> {
        val response = api.deleteProperty(idFilter = "eq.$propertyId")
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_delete_property))
    }

    override suspend fun setPropertyPublished(propertyId: String, published: Boolean): LandlordResult<Unit> {
        // Landlords can only toggle between their own draft/published states —
        // moving OUT of 'pending'/'rejected' back into the review queue is an
        // admin concern (§3.10), so this only flips published <-> draft.
        val newStatus = if (published) "published" else "draft"
        val response = api.updateProperty(idFilter = "eq.$propertyId", body = mapOf("status" to newStatus))
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_update_availability))
    }

    override suspend fun addPropertyImage(propertyId: String, imageUrl: String, position: Int): LandlordResult<Unit> {
        val response = api.addPropertyImage(
            com.rentease.app.data.remote.dto.PropertyImageDto(
                id = UUID.randomUUID().toString(), propertyId = propertyId, url = imageUrl, position = position
            )
        )
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_attach_image))
    }

    override suspend fun setPropertyImages(propertyId: String, urls: List<String>): LandlordResult<Unit> {
        val cleared = api.deleteAllPropertyImages(propertyIdFilter = "eq.$propertyId")
        if (!cleared.isSuccessful) return LandlordResult.Failure(cleared.errorMessage(R.string.error_could_not_attach_image))
        urls.forEachIndexed { index, url ->
            val result = addPropertyImage(propertyId, url, index)
            if (result is LandlordResult.Failure) return result
        }
        return LandlordResult.Success(Unit)
    }

    // ---------------- Rooms ----------------

    override suspend fun getRooms(propertyId: String): LandlordResult<List<RentalRoom>> {
        val response = api.getRooms(propertyIdFilter = "eq.$propertyId")
        return if (response.isSuccessful) {
            LandlordResult.Success(response.body().orEmpty().map { it.toRoom() })
        } else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_load_rooms))
    }

    override suspend fun createRoom(propertyId: String, input: RoomInput): LandlordResult<RentalRoom> {
        val response = api.createRoom(
            RoomDto(
                propertyId = propertyId,
                name = input.name,
                roomType = input.roomType,
                pricePerMonth = input.pricePerMonth,
                capacity = input.capacity,
                status = input.status.toDb()
            )
        )
        val dto = response.bodyOrNull()?.firstOrNull() ?: return LandlordResult.Failure(response.errorMessage(R.string.error_could_not_create_room))
        syncRoomsAvailable(propertyId)
        return LandlordResult.Success(dto.toRoom())
    }

    override suspend fun updateRoom(roomId: String, input: RoomInput): LandlordResult<RentalRoom> {
        val body = mapOf(
            "name" to input.name,
            "room_type" to input.roomType,
            "price_per_month" to input.pricePerMonth,
            "capacity" to input.capacity,
            "status" to input.status.toDb()
        )
        val response = api.updateRoom(idFilter = "eq.$roomId", body = body)
        val dto = response.bodyOrNull()?.firstOrNull() ?: return LandlordResult.Failure(response.errorMessage(R.string.error_could_not_update_room))
        syncRoomsAvailable(dto.propertyId)
        return LandlordResult.Success(dto.toRoom())
    }

    override suspend fun deleteRoom(roomId: String): LandlordResult<Unit> {
        // Look the room up first: once it is gone we can no longer tell which property to re-count.
        val propertyId = api.getRoomById(idFilter = "eq.$roomId").bodyOrNull()?.firstOrNull()?.propertyId
        val response = api.deleteRoom(idFilter = "eq.$roomId")
        if (!response.isSuccessful) return LandlordResult.Failure(response.errorMessage(R.string.error_could_not_delete_room))
        propertyId?.let { syncRoomsAvailable(it) }
        return LandlordResult.Success(Unit)
    }

    /**
     * Keeps `properties.rooms_available` equal to the number of rooms with status `available`, so the
     * "Available now" filter and listing cards are right without the landlord editing a number by hand.
     * Best effort: a failure here never fails the room change itself. A property with no rooms keeps the
     * number typed on the property form.
     */
    private suspend fun syncRoomsAvailable(propertyId: String) {
        val rooms = api.getRooms(propertyIdFilter = "eq.$propertyId").bodyOrNull().orEmpty()
        if (rooms.isEmpty()) return
        api.updateProperty(idFilter = "eq.$propertyId", body = mapOf("rooms_available" to rooms.count { it.status == "available" }))
    }

    // ---------------- Bookings ----------------

    override suspend fun getBookings(): LandlordResult<List<TenantBooking>> {
        val userId = requireUserId() ?: return LandlordResult.Failure(context.getString(R.string.error_not_signed_in))
        val response = api.getBookingsForLandlord(landlordIdFilter = "eq.$userId")
        if (!response.isSuccessful) return LandlordResult.Failure(response.errorMessage(R.string.error_could_not_load_bookings))

        val bookings = response.body().orEmpty()
        if (bookings.isEmpty()) return LandlordResult.Success(emptyList())

        val studentNames = resolveNames(bookings.map { it.studentId }.distinct()) { ids -> api.getProfileNames(ids) }
            .associate { it.id to (it.fullName ?: context.getString(R.string.fallback_student_name)) }
        val propertyTitles = resolveNames(bookings.map { it.propertyId }.distinct()) { ids -> api.getPropertyTitles(ids) }
            .associate { it.id to (it.title ?: context.getString(R.string.fallback_property_name)) }
        val roomIds = bookings.mapNotNull { it.roomId }.distinct()
        val roomNames = if (roomIds.isNotEmpty()) {
            resolveNames(roomIds) { ids -> api.getRoomNames(ids) }.associate { it.id to it.name }
        } else emptyMap()

        return LandlordResult.Success(
            bookings.map { dto ->
                TenantBooking(
                    id = dto.id,
                    reference = dto.reference,
                    studentId = dto.studentId,
                    studentName = studentNames[dto.studentId] ?: context.getString(R.string.fallback_student_name),
                    propertyId = dto.propertyId,
                    propertyTitle = propertyTitles[dto.propertyId] ?: context.getString(R.string.fallback_property_name),
                    roomId = dto.roomId,
                    roomName = dto.roomId?.let { roomNames[it] },
                    moveInDate = dto.moveInDate,
                    leaseMonths = dto.leaseMonths,
                    monthlyRent = dto.monthlyRent,
                    deposit = dto.deposit,
                    status = BookingStatus.fromDb(dto.status),
                    message = dto.message,
                    idDocumentUrl = dto.idDocumentUrl,
                    proofOfRegistrationUrl = dto.proofOfRegistrationUrl
                )
            }
        )
    }

    override suspend fun approveBooking(bookingId: String, roomId: String?): LandlordResult<Unit> {
        val response = api.updateBooking(
            idFilter = "eq.$bookingId",
            body = BookingUpdateDto(status = "approved", roomId = roomId, decidedAt = Instant.now().toString())
        )
        // Approving a booking occupies the assigned room, if one was given —
        // mirrors the web app's implicit behaviour of a booking driving room status.
        if (response.isSuccessful && roomId != null) {
            api.updateRoom(idFilter = "eq.$roomId", body = mapOf("status" to "occupied"))
        }
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_approve_booking))
    }

    override suspend fun rejectBooking(bookingId: String): LandlordResult<Unit> {
        val response = api.updateBooking(
            idFilter = "eq.$bookingId",
            body = BookingUpdateDto(status = "rejected", decidedAt = Instant.now().toString())
        )
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_reject_booking))
    }

    override suspend fun assignRoom(bookingId: String, roomId: String): LandlordResult<Unit> {
        val response = api.updateBooking(idFilter = "eq.$bookingId", body = BookingUpdateDto(roomId = roomId))
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_assign_room))
    }

    // ---------------- Maintenance ----------------

    override suspend fun getMaintenanceTickets(): LandlordResult<List<MaintenanceTicket>> {
        val propertiesResult = getMyProperties()
        val properties = (propertiesResult as? LandlordResult.Success)?.data
            ?: return LandlordResult.Failure(context.getString(R.string.error_could_not_resolve_properties_maintenance))
        if (properties.isEmpty()) return LandlordResult.Success(emptyList())

        val idFilter = "in.(${properties.joinToString(",") { it.id }})"
        val response = api.getMaintenanceForProperties(propertyIdInFilter = idFilter)
        if (!response.isSuccessful) return LandlordResult.Failure(response.errorMessage(R.string.error_could_not_load_maintenance_tickets))

        val tickets = response.body().orEmpty()
        val propertyTitleById = properties.associate { it.id to it.title }
        val studentNames = resolveNames(tickets.map { it.studentId }.distinct()) { ids -> api.getProfileNames(ids) }
            .associate { it.id to (it.fullName ?: context.getString(R.string.fallback_student_name)) }

        return LandlordResult.Success(
            tickets.map { dto ->
                MaintenanceTicket(
                    id = dto.id,
                    propertyId = dto.propertyId,
                    propertyTitle = propertyTitleById[dto.propertyId] ?: context.getString(R.string.fallback_property_name),
                    studentId = dto.studentId,
                    studentName = studentNames[dto.studentId] ?: context.getString(R.string.fallback_student_name),
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

    override suspend fun updateMaintenanceStatus(ticketId: String, status: MaintenanceStatus): LandlordResult<Unit> {
        val response = api.updateMaintenanceStatus(idFilter = "eq.$ticketId", body = MaintenanceUpdateDto(status = status.toDb()))
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_update_ticket_status))
    }

    // ---------------- Messaging ----------------

    override suspend fun getConversations(): LandlordResult<List<ConversationSummary>> {
        val userId = requireUserId() ?: return LandlordResult.Failure(context.getString(R.string.error_not_signed_in))
        val response = api.getConversations(landlordIdFilter = "eq.$userId")
        if (!response.isSuccessful) return LandlordResult.Failure(response.errorMessage(R.string.error_could_not_load_conversations))

        val conversations = response.body().orEmpty()
        if (conversations.isEmpty()) return LandlordResult.Success(emptyList())

        val studentIds = conversations.map { it.studentId }.distinct()
        val studentNames = resolveNames(studentIds) { ids -> api.getProfileNames(ids) }.associate { it.id to (it.fullName ?: context.getString(R.string.fallback_student_name)) }

        return LandlordResult.Success(
            conversations.map { dto ->
                val unread = api.getMessages(conversationIdFilter = "eq.${dto.id}").bodyOrNull().orEmpty()
                    .count { it.senderId != userId && it.readAt == null }
                ConversationSummary(
                    id = dto.id.orEmpty(),
                    studentId = dto.studentId,
                    landlordId = dto.landlordId,
                    propertyId = dto.propertyId,
                    counterpartName = studentNames[dto.studentId] ?: context.getString(R.string.fallback_student_name),
                    subject = dto.subject,
                    lastMessageAt = dto.lastMessageAt,
                    unreadCount = unread
                )
            }
        )
    }

    override suspend fun getMessages(conversationId: String): LandlordResult<List<ChatMessage>> {
        val response = api.getMessages(conversationIdFilter = "eq.$conversationId")
        return if (response.isSuccessful) {
            LandlordResult.Success(response.body().orEmpty().map {
                ChatMessage(
                    id = it.id.orEmpty(), conversationId = it.conversationId, senderId = it.senderId,
                    body = it.body, createdAt = it.createdAt, readAt = it.readAt
                )
            })
        } else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_load_messages))
    }

    override suspend fun sendMessage(conversationId: String, body: String): LandlordResult<Unit> {
        val userId = requireUserId() ?: return LandlordResult.Failure(context.getString(R.string.error_not_signed_in))
        val response = api.sendMessage(MessageDto(conversationId = conversationId, senderId = userId, body = body))
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_send_message))
    }

    override suspend fun markConversationRead(conversationId: String): LandlordResult<Unit> {
        val userId = requireUserId() ?: return LandlordResult.Failure(context.getString(R.string.error_not_signed_in))
        val response = api.markMessagesRead(
            conversationIdFilter = "eq.$conversationId",
            notSenderFilter = "neq.$userId",
            body = mapOf("read_at" to Instant.now().toString())
        )
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_mark_conversation_read))
    }

    // ---------------- Notifications ----------------

    override suspend fun getNotifications(): LandlordResult<List<AppNotification>> {
        val userId = requireUserId() ?: return LandlordResult.Failure(context.getString(R.string.error_not_signed_in))
        val response = api.getNotifications(userIdFilter = "eq.$userId")
        return if (response.isSuccessful) {
            LandlordResult.Success(response.body().orEmpty().map {
                AppNotification(
                    id = it.id, title = it.title, body = it.body, category = it.category,
                    link = it.link, readAt = it.readAt, createdAt = it.createdAt
                )
            })
        } else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_load_notifications))
    }

    override suspend fun markNotificationRead(notificationId: String): LandlordResult<Unit> {
        val response = api.markNotificationRead(idFilter = "eq.$notificationId", body = mapOf("read_at" to Instant.now().toString()))
        return if (response.isSuccessful) LandlordResult.Success(Unit)
        else LandlordResult.Failure(response.errorMessage(R.string.error_could_not_update_notification))
    }

    // ---------------- Analytics ----------------

    /**
     * Computed client-side from properties/rooms/bookings — there is no
     * dedicated analytics table or endpoint in the schema. Occupancy is
     * estimated as (rooms currently 'occupied') / (total rooms across all
     * properties); revenue is estimated as the sum of monthly_rent across
     * ACTIVE bookings, not a real payment ledger (no payments integration
     * exists yet — see Phase 1 audit gap #2).
     */
    override suspend fun getAnalytics(): LandlordResult<LandlordAnalytics> {
        val propertiesResult = getMyProperties()
        val properties = (propertiesResult as? LandlordResult.Success)?.data
            ?: return LandlordResult.Failure(context.getString(R.string.error_could_not_compute_analytics))

        var totalRooms = 0
        var occupiedRooms = 0
        for (property in properties) {
            val roomsResponse = api.getRooms(propertyIdFilter = "eq.${property.id}")
            val rooms = roomsResponse.bodyOrNull().orEmpty()
            totalRooms += rooms.size
            occupiedRooms += rooms.count { it.status == "occupied" }
        }

        val bookingsResult = getBookings()
        val bookings = (bookingsResult as? LandlordResult.Success)?.data.orEmpty()
        val pendingRequests = bookings.count { it.status == BookingStatus.PENDING }

        // Real revenue: sum of actually-paid `payments` rows for this landlord's own
        // bookings (RLS's "Payment parties read" allows this via owns_property), not a
        // rent-based estimate. Matches `landlord.index.tsx`'s calculation exactly.
        val bookingIds = bookings.map { it.id }
        val paidRevenue = if (bookingIds.isNotEmpty()) {
            api.getPaymentsForBookings(bookingIdInFilter = "in.(${bookingIds.joinToString(",")})")
                .bodyOrNull().orEmpty().sumOf { it.amount }
        } else 0.0

        val maintenanceResult = getMaintenanceTickets()
        val openTickets = (maintenanceResult as? LandlordResult.Success)?.data
            ?.count { it.status == MaintenanceStatus.OPEN || it.status == MaintenanceStatus.IN_PROGRESS } ?: 0

        return LandlordResult.Success(
            LandlordAnalytics(
                totalProperties = properties.size,
                totalRoomsAvailable = totalRooms,
                occupiedRoomsEstimate = occupiedRooms,
                occupancyRatePercent = if (totalRooms > 0) (occupiedRooms.toDouble() / totalRooms) * 100.0 else 0.0,
                contractedMonthlyIncome = IncomeReport.contractedMonthly(bookings),
                collectedToDate = paidRevenue,
                incomeByProperty = IncomeReport.byProperty(bookings),
                pendingBookingRequests = pendingRequests,
                openMaintenanceTickets = openTickets
            )
        )
    }

    // ---------------- Helpers ----------------

    private suspend fun resolveNames(
        ids: List<String>,
        fetch: suspend (String) -> Response<List<com.rentease.app.data.remote.dto.NameLookupDto>>
    ): List<com.rentease.app.data.remote.dto.NameLookupDto> {
        if (ids.isEmpty()) return emptyList()
        val response = fetch("in.(${ids.joinToString(",")})")
        return response.bodyOrNull().orEmpty()
    }

    /** Name shown on the listing: business name, else legal name, else the profile name (never blank when any exists). */
    private suspend fun resolveLandlordName(userId: String): String {
        val lp = api.getLandlordProfile(userIdFilter = "eq.$userId").bodyOrNull()?.firstOrNull()
        lp?.businessName?.takeIf { it.isNotBlank() }?.let { return it }
        lp?.legalName?.takeIf { it.isNotBlank() }?.let { return it }
        return api.getProfileNames(idInFilter = "in.($userId)").bodyOrNull()?.firstOrNull()?.fullName.orEmpty()
    }

    private fun generateSlug(title: String): String {
        val base = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return "$base-${UUID.randomUUID().toString().take(6)}"
    }

    private fun PropertyDto.toLandlordProperty(images: List<String>) = LandlordProperty(
        id = id, slug = slug, title = title, description = description, address = address,
        city = city, province = province, universityId = universityId, distanceKm = distanceKm, pricePerMonth = pricePerMonth,
        deposit = deposit, bedrooms = bedrooms, bathrooms = bathrooms, roomsAvailable = roomsAvailable,
        furnished = furnished, amenities = amenities, coverImageUrl = coverImage, images = images,
        status = PropertyStatus.fromDb(status),
        verified = verified, rating = rating, reviewCount = reviewCount
    )

    private fun RoomDto.toRoom() = RentalRoom(
        id = id.orEmpty(), propertyId = propertyId, name = name, roomType = roomType,
        pricePerMonth = pricePerMonth, capacity = capacity, status = RoomStatus.fromDb(status)
    )

    private fun <T> Response<T>.bodyOrNull(): T? = if (isSuccessful) body() else null
    private fun <T> Response<T>.errorMessage(@StringRes fallbackRes: Int): String =
        errorBody()?.string()
            ?.takeIf { it.isNotBlank() && !(code() == 503 && "network_unreachable" in it) }
            ?: "${context.getString(fallbackRes)}${context.httpHint(code())}"
}
