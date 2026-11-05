package com.rentease.app.domain.repository

import com.rentease.app.domain.model.AppNotification
import com.rentease.app.domain.model.ChatMessage
import com.rentease.app.domain.model.ConversationSummary
import com.rentease.app.domain.model.LandlordAnalytics
import com.rentease.app.domain.model.LandlordProfile
import com.rentease.app.domain.model.LandlordProperty
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.model.MaintenanceTicket
import com.rentease.app.domain.model.PropertyInput
import com.rentease.app.domain.model.RentalRoom
import com.rentease.app.domain.model.RoomInput
import com.rentease.app.domain.model.TenantBooking

sealed interface LandlordResult<out T> {
    data class Success<T>(val data: T) : LandlordResult<T>
    data class Failure(val message: String) : LandlordResult<Nothing>
}

interface LandlordRepository {

    suspend fun getVerificationStatus(): LandlordResult<LandlordProfile>

    // Properties
    suspend fun getMyProperties(): LandlordResult<List<LandlordProperty>>
    suspend fun getProperty(propertyId: String): LandlordResult<LandlordProperty>
    suspend fun createProperty(input: PropertyInput): LandlordResult<LandlordProperty>
    suspend fun updateProperty(propertyId: String, input: PropertyInput): LandlordResult<LandlordProperty>
    suspend fun deleteProperty(propertyId: String): LandlordResult<Unit>
    suspend fun setPropertyPublished(propertyId: String, published: Boolean): LandlordResult<Unit>
    suspend fun addPropertyImage(propertyId: String, imageUrl: String, position: Int): LandlordResult<Unit>

    /** Replaces the property's gallery with [urls] (in order; index = position). */
    suspend fun setPropertyImages(propertyId: String, urls: List<String>): LandlordResult<Unit>

    // Rooms
    suspend fun getRooms(propertyId: String): LandlordResult<List<RentalRoom>>
    suspend fun createRoom(propertyId: String, input: RoomInput): LandlordResult<RentalRoom>
    suspend fun updateRoom(roomId: String, input: RoomInput): LandlordResult<RentalRoom>
    suspend fun deleteRoom(roomId: String): LandlordResult<Unit>

    // Bookings / tenants
    suspend fun getBookings(): LandlordResult<List<TenantBooking>>
    suspend fun approveBooking(bookingId: String, roomId: String?): LandlordResult<Unit>
    suspend fun rejectBooking(bookingId: String): LandlordResult<Unit>
    suspend fun assignRoom(bookingId: String, roomId: String): LandlordResult<Unit>

    // Maintenance
    suspend fun getMaintenanceTickets(): LandlordResult<List<MaintenanceTicket>>
    suspend fun updateMaintenanceStatus(ticketId: String, status: MaintenanceStatus): LandlordResult<Unit>

    // Messaging
    suspend fun getConversations(): LandlordResult<List<ConversationSummary>>
    suspend fun getMessages(conversationId: String): LandlordResult<List<ChatMessage>>
    suspend fun sendMessage(conversationId: String, body: String): LandlordResult<Unit>
    suspend fun markConversationRead(conversationId: String): LandlordResult<Unit>

    // Notifications
    suspend fun getNotifications(): LandlordResult<List<AppNotification>>
    suspend fun markNotificationRead(notificationId: String): LandlordResult<Unit>

    // Analytics
    suspend fun getAnalytics(): LandlordResult<LandlordAnalytics>
}
