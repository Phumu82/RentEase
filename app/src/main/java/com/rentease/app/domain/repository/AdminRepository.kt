package com.rentease.app.domain.repository

import com.rentease.app.domain.model.AdminBookingItem
import com.rentease.app.domain.model.AdminDashboardStats
import com.rentease.app.domain.model.AdminUserItem
import com.rentease.app.domain.model.AuditLogEntry
import com.rentease.app.domain.model.Complaint
import com.rentease.app.domain.model.LandlordVerificationItem
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.model.PropertyModerationItem
import com.rentease.app.domain.model.VerificationDecision

sealed interface AdminResult<out T> {
    data class Success<T>(val data: T) : AdminResult<T>
    data class Failure(val message: String) : AdminResult<Nothing>
}

interface AdminRepository {

    /** Submits the admin-access code to the server-side Edge Function. On
     *  success the caller's session now holds the admin role. */
    suspend fun requestAdminAccess(code: String): AdminResult<Unit>

    suspend fun getDashboardStats(): AdminResult<AdminDashboardStats>

    /** Every landlord account with their verification state and uploaded documents (web parity). */
    suspend fun getLandlordVerifications(): AdminResult<List<LandlordVerificationItem>>

    /** Records a decision (with optional notes the landlord sees on their onboarding page). */
    suspend fun decideLandlord(userId: String, decision: VerificationDecision, notes: String? = null): AdminResult<Unit>
    suspend fun approveLandlord(userId: String): AdminResult<Unit>
    suspend fun rejectLandlord(userId: String): AdminResult<Unit>

    suspend fun getPendingProperties(): AdminResult<List<PropertyModerationItem>>
    suspend fun approveProperty(propertyId: String): AdminResult<Unit>
    suspend fun rejectProperty(propertyId: String): AdminResult<Unit>

    suspend fun getAllUsers(): AdminResult<List<AdminUserItem>>
    suspend fun setUserSuspended(userId: String, suspended: Boolean): AdminResult<Unit>

    suspend fun getAllBookings(): AdminResult<List<AdminBookingItem>>

    suspend fun getAuditLog(): AdminResult<List<AuditLogEntry>>

    /** Every complaint raised by any student or landlord, newest first, with
     *  reporter name/email joined in for display. */
    suspend fun getAllComplaints(): AdminResult<List<Complaint>>

    /** Moves a complaint through its lifecycle and (optionally) replies to the
     *  reporter. NOTE: unlike the web app's equivalent server function, this
     *  does not also insert a notification for the reporter — RLS's "Own
     *  notifications" policy only allows a user to insert notifications for
     *  themselves (`user_id = auth.uid()`), so notifying a *different* user
     *  from the client requires the service_role key, which this app
     *  deliberately never holds. The status change and audit log entry are
     *  fully real; the reporter simply won't get a push/in-app notification
     *  about it from here. */
    suspend fun decideComplaint(complaintId: String, status: MaintenanceStatus, response: String?): AdminResult<Unit>
}
