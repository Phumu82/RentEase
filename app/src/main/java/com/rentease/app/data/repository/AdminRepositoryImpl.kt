package com.rentease.app.data.repository

import com.rentease.app.data.remote.httpHint
import android.content.Context
import androidx.annotation.StringRes
import com.rentease.app.R
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseAdminAccessApi
import com.rentease.app.data.remote.api.SupabaseAdminApi
import com.rentease.app.data.remote.dto.AdminAccessRequestDto
import com.rentease.app.data.remote.dto.AuditLogInsertDto
import com.rentease.app.data.remote.dto.ComplaintDecisionDto
import com.rentease.app.data.remote.dto.PropertyModerationDecisionDto
import com.rentease.app.data.remote.dto.SuspendUserDto
import com.rentease.app.data.remote.dto.VerificationDecisionDto
import com.rentease.app.domain.model.AdminBookingItem
import com.rentease.app.domain.model.AdminDashboardStats
import com.rentease.app.domain.model.AdminUserItem
import com.rentease.app.domain.model.AuditLogEntry
import com.rentease.app.domain.model.BookingStatus
import com.rentease.app.domain.model.Complaint
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.model.LandlordVerificationItem
import com.rentease.app.domain.model.PropertyModerationItem
import com.rentease.app.domain.model.PropertyStatus
import com.rentease.app.domain.model.UserRole
import com.rentease.app.domain.model.VerificationDecision
import com.rentease.app.domain.model.VerificationDocument
import com.rentease.app.domain.model.VerificationStatus
import com.rentease.app.domain.model.LandlordDocumentKind
import com.rentease.app.domain.repository.AdminRepository
import com.rentease.app.domain.repository.AdminResult
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Response
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val adminApi: SupabaseAdminApi,
    private val adminAccessApi: SupabaseAdminAccessApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : AdminRepository {

    override suspend fun requestAdminAccess(code: String): AdminResult<Unit> {
        val response = try {
            adminAccessApi.requestAdminAccess(AdminAccessRequestDto(code))
        } catch (e: Exception) {
            return AdminResult.Failure(context.getString(R.string.error_could_not_reach_server))
        }
        val body = response.body()
        return if (response.isSuccessful && body?.success == true) {
            // The Edge Function has now granted the admin role server-side, but
            // this device's cached role (in TokenManager) is still whatever it
            // was at login — the caller must re-resolve/re-login to pick up the
            // new role. We deliberately do NOT locally fake role="admin" here;
            // that would be a client trusting itself, exactly what §7's
            // security rules forbid.
            AdminResult.Success(Unit)
        } else {
            AdminResult.Failure(body?.error ?: response.errorMessage(R.string.error_access_denied))
        }
    }

    override suspend fun getDashboardStats(): AdminResult<AdminDashboardStats> {
        val verifications = adminApi.getLandlordProfiles(statusFilter = "eq.submitted").bodyOrNull().orEmpty()
        val pendingProperties = adminApi.getPropertiesByStatus(statusFilter = "eq.pending").bodyOrNull().orEmpty()
        val profiles = adminApi.getAllProfiles().bodyOrNull().orEmpty()
        val roles = adminApi.getAllRoles().bodyOrNull().orEmpty()
        val roleByUser = roles.associate { it.userId to it.role }

        val studentCount = profiles.count { roleByUser[it.id] == "student" }
        val landlordCount = profiles.count { roleByUser[it.id] == "landlord" }
        val suspendedCount = profiles.count { it.isSuspended }

        return AdminResult.Success(
            AdminDashboardStats(
                pendingLandlordVerifications = verifications.size,
                pendingPropertyModerations = pendingProperties.size,
                totalStudents = studentCount,
                totalLandlords = landlordCount,
                suspendedUsers = suspendedCount,
                // There is no platform-wide "all maintenance tickets" admin
                // endpoint defined yet (Phase 7 scope was verification/
                // moderation/users/bookings/audit log) — reporting 0 here
                // rather than a wrong/fabricated number.
                openMaintenanceTickets = 0
            )
        )
    }

    // ---------------- Landlord verification ----------------

    override suspend fun getLandlordVerifications(): AdminResult<List<LandlordVerificationItem>> {
        val response = adminApi.getLandlordProfiles()
        if (!response.isSuccessful) return AdminResult.Failure(response.errorMessage(R.string.error_could_not_load_verification_queue))

        val landlords = response.body().orEmpty()
        if (landlords.isEmpty()) return AdminResult.Success(emptyList())

        val profiles = adminApi.getAllProfiles().bodyOrNull().orEmpty().associateBy { it.id }

        return AdminResult.Success(
            landlords.map { dto ->
                val profile = profiles[dto.userId]
                val documents = listOfNotNull(
                    dto.idDocumentUrl?.takeIf { it.isNotBlank() }?.let { VerificationDocument(LandlordDocumentKind.ID_DOCUMENT, it) },
                    dto.proofOfOwnershipUrl?.takeIf { it.isNotBlank() }?.let { VerificationDocument(LandlordDocumentKind.PROOF_OF_OWNERSHIP, it) },
                    dto.proofOfAddressUrl?.takeIf { it.isNotBlank() }?.let { VerificationDocument(LandlordDocumentKind.PROOF_OF_ADDRESS, it) },
                    dto.businessRegistrationDocUrl?.takeIf { it.isNotBlank() }?.let { VerificationDocument(LandlordDocumentKind.BUSINESS_REGISTRATION, it) }
                )
                LandlordVerificationItem(
                    userId = dto.userId,
                    fullName = profile?.fullName?.takeIf { it.isNotBlank() } ?: dto.legalName ?: context.getString(R.string.fallback_landlord_name),
                    email = profile?.email,
                    businessName = dto.businessName ?: profile?.companyName,
                    idDocumentUrl = dto.idDocumentUrl,
                    proofOfOwnershipUrl = dto.proofOfOwnershipUrl,
                    status = VerificationStatus.fromDb(dto.verificationStatus),
                    legalName = dto.legalName,
                    phone = dto.contactPhone ?: profile?.phone,
                    address = listOfNotNull(dto.businessAddress, dto.city, dto.province, dto.postalCode)
                        .filter { it.isNotBlank() }.joinToString(", ").ifBlank { null },
                    registrationNumber = dto.businessRegistrationNumber,
                    notes = dto.verificationNotes,
                    documents = documents
                )
            }
        )
    }

    override suspend fun approveLandlord(userId: String): AdminResult<Unit> =
        decideLandlord(userId, VerificationDecision.VERIFIED)

    override suspend fun rejectLandlord(userId: String): AdminResult<Unit> =
        decideLandlord(userId, VerificationDecision.REJECTED)

    override suspend fun decideLandlord(userId: String, decision: VerificationDecision, notes: String?): AdminResult<Unit> {
        val response = adminApi.setVerificationStatus(
            userIdFilter = "eq.$userId",
            body = VerificationDecisionDto(decision.dbValue, notes?.trim()?.takeIf { it.isNotEmpty() })
        )
        if (!response.isSuccessful) return AdminResult.Failure(response.errorMessage(R.string.error_could_not_update_verification_status))
        // The web also keeps profiles.is_verified (the badge students see) in step with the decision.
        val badge = adminApi.setProfileVerified(
            idFilter = "eq.$userId",
            body = mapOf("is_verified" to (decision == VerificationDecision.VERIFIED))
        )
        if (!badge.isSuccessful) return AdminResult.Failure(badge.errorMessage(R.string.error_could_not_update_verification_status))
        logAuditEvent(action = "admin.landlord_${decision.dbValue}", entity = "landlord_profiles", entityId = userId)
        return AdminResult.Success(Unit)
    }

    // ---------------- Property moderation ----------------

    override suspend fun getPendingProperties(): AdminResult<List<PropertyModerationItem>> {
        val response = adminApi.getPropertiesByStatus(statusFilter = "eq.pending")
        if (!response.isSuccessful) return AdminResult.Failure(response.errorMessage(R.string.error_could_not_load_moderation_queue))

        val properties = response.body().orEmpty()
        return AdminResult.Success(
            properties.map { dto ->
                PropertyModerationItem(
                    id = dto.id,
                    title = dto.title,
                    landlordName = dto.landlordName.ifBlank { context.getString(R.string.fallback_landlord_name) },
                    city = dto.city,
                    province = dto.province,
                    pricePerMonth = dto.pricePerMonth,
                    coverImageUrl = dto.coverImage,
                    status = PropertyStatus.fromDb(dto.status)
                )
            }
        )
    }

    override suspend fun approveProperty(propertyId: String): AdminResult<Unit> =
        setPropertyStatus(propertyId, "published", auditAction = "admin.property_approved")

    override suspend fun rejectProperty(propertyId: String): AdminResult<Unit> =
        setPropertyStatus(propertyId, "rejected", auditAction = "admin.property_rejected")

    private suspend fun setPropertyStatus(propertyId: String, status: String, auditAction: String): AdminResult<Unit> {
        val response = adminApi.setPropertyModerationStatus(idFilter = "eq.$propertyId", body = PropertyModerationDecisionDto(status))
        if (!response.isSuccessful) return AdminResult.Failure(response.errorMessage(R.string.error_could_not_update_property_status))
        logAuditEvent(action = auditAction, entity = "properties", entityId = propertyId)
        return AdminResult.Success(Unit)
    }

    // ---------------- User management ----------------

    override suspend fun getAllUsers(): AdminResult<List<AdminUserItem>> {
        val profilesResponse = adminApi.getAllProfiles()
        if (!profilesResponse.isSuccessful) return AdminResult.Failure(profilesResponse.errorMessage(R.string.error_could_not_load_users))

        val roles = adminApi.getAllRoles().bodyOrNull().orEmpty().associate { it.userId to it.role }

        return AdminResult.Success(
            profilesResponse.body().orEmpty().mapNotNull { profile ->
                val roleStr = roles[profile.id] ?: return@mapNotNull null
                AdminUserItem(
                    id = profile.id,
                    fullName = profile.fullName,
                    email = profile.email,
                    role = UserRole.fromDb(roleStr),
                    isSuspended = profile.isSuspended
                )
            }
        )
    }

    override suspend fun setUserSuspended(userId: String, suspended: Boolean): AdminResult<Unit> {
        val response = adminApi.setSuspended(idFilter = "eq.$userId", body = SuspendUserDto(suspended))
        if (!response.isSuccessful) return AdminResult.Failure(response.errorMessage(R.string.error_could_not_update_user_status))
        logAuditEvent(
            action = if (suspended) "admin.user_suspended" else "admin.user_reinstated",
            entity = "profiles", entityId = userId
        )
        return AdminResult.Success(Unit)
    }

    // ---------------- Booking oversight ----------------

    override suspend fun getAllBookings(): AdminResult<List<AdminBookingItem>> {
        val response = adminApi.getAllBookings()
        if (!response.isSuccessful) return AdminResult.Failure(response.errorMessage(R.string.error_could_not_load_bookings))

        val bookings = response.body().orEmpty()
        if (bookings.isEmpty()) return AdminResult.Success(emptyList())

        val profiles = adminApi.getAllProfiles().bodyOrNull().orEmpty().associateBy { it.id }

        // There's no "get all properties regardless of status" admin endpoint,
        // so titles are resolved by scanning each known status bucket once.
        // Not cheap, but correct — and this screen is not a hot path.
        val propertyTitles = mutableMapOf<String, String>()
        for (status in listOf("draft", "pending", "published", "rejected", "archived")) {
            adminApi.getPropertiesByStatus(statusFilter = "eq.$status").bodyOrNull().orEmpty().forEach {
                propertyTitles[it.id] = it.title
            }
        }

        return AdminResult.Success(
            bookings.map { dto ->
                AdminBookingItem(
                    id = dto.id,
                    reference = dto.reference,
                    studentName = profiles[dto.studentId]?.fullName ?: context.getString(R.string.fallback_student_name),
                    propertyTitle = propertyTitles[dto.propertyId] ?: context.getString(R.string.fallback_property_name),
                    status = BookingStatus.fromDb(dto.status),
                    monthlyRent = dto.monthlyRent
                )
            }
        )
    }

    // ---------------- Audit log ----------------

    override suspend fun getAuditLog(): AdminResult<List<AuditLogEntry>> {
        val response = adminApi.getAuditLogs()
        if (!response.isSuccessful) return AdminResult.Failure(response.errorMessage(R.string.error_could_not_load_audit_log))

        val entries = response.body().orEmpty()
        val actorIds = entries.mapNotNull { it.actorId }.distinct()
        val names = if (actorIds.isNotEmpty()) {
            adminApi.getAllProfiles().bodyOrNull().orEmpty().associate { it.id to it.fullName }
        } else emptyMap()

        return AdminResult.Success(
            entries.map { dto ->
                AuditLogEntry(
                    id = dto.id,
                    actorName = dto.actorId?.let { names[it] },
                    action = dto.action,
                    entity = dto.entity,
                    entityId = dto.entityId,
                    createdAt = dto.createdAt
                )
            }
        )
    }

    override suspend fun getAllComplaints(): AdminResult<List<Complaint>> {
        val response = adminApi.getAllComplaints()
        if (!response.isSuccessful) return AdminResult.Failure(response.errorMessage(R.string.error_could_not_load_complaints))

        val complaints = response.body().orEmpty()
        val reporterIds = complaints.mapNotNull { it.reporterId }.distinct()
        val profiles = if (reporterIds.isNotEmpty()) {
            adminApi.getAllProfiles().bodyOrNull().orEmpty().associateBy { it.id }
        } else emptyMap()

        return AdminResult.Success(
            complaints.map { dto ->
                val profile = dto.reporterId?.let { profiles[it] }
                Complaint(
                    id = dto.id,
                    subject = dto.subject,
                    category = dto.category,
                    description = dto.description,
                    status = MaintenanceStatus.fromDb(dto.status),
                    adminResponse = dto.adminResponse,
                    createdAt = dto.createdAt,
                    reporterName = profile?.fullName,
                    reporterEmail = profile?.email
                )
            }
        )
    }

    override suspend fun decideComplaint(complaintId: String, status: MaintenanceStatus, response: String?): AdminResult<Unit> {
        val resolvedAt = if (status == MaintenanceStatus.RESOLVED || status == MaintenanceStatus.CLOSED) {
            Instant.now().toString()
        } else null

        val apiResponse = adminApi.decideComplaint(
            idFilter = "eq.$complaintId",
            body = ComplaintDecisionDto(status = status.toDb(), adminResponse = response?.ifBlank { null }, resolvedAt = resolvedAt)
        )
        if (!apiResponse.isSuccessful) {
            return AdminResult.Failure(apiResponse.errorMessage(R.string.error_could_not_update_complaint))
        }
        logAuditEvent(action = "admin.complaint_${status.toDb()}", entity = "complaints", entityId = complaintId)
        return AdminResult.Success(Unit)
    }

    // ---------------- Helpers ----------------

    /**
     * Every admin write action logs an audit_logs row, per Phase 7's explicit
     * requirement ("Every important administrative action must be recorded").
     * Best-effort: a failed audit insert does not roll back or fail the admin
     * action itself, since the action already succeeded server-side.
     */
    private suspend fun logAuditEvent(action: String, entity: String, entityId: String?) {
        val actorId = tokenManager.getUserId() ?: return
        runCatching {
            adminApi.insertAuditLog(AuditLogInsertDto(actorId = actorId, action = action, entity = entity, entityId = entityId))
        }
    }

    private fun <T> Response<T>.bodyOrNull(): T? = if (isSuccessful) body() else null
    private fun <T> Response<T>.errorMessage(@StringRes fallbackRes: Int): String =
        errorBody()?.string()
            ?.takeIf { it.isNotBlank() && !(code() == 503 && "network_unreachable" in it) }
            ?: "${context.getString(fallbackRes)}${context.httpHint(code())}"
}
