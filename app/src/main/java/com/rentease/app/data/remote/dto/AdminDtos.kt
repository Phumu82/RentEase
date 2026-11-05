package com.rentease.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuditLogDto(
    val id: String,
    @SerialName("actor_id") val actorId: String? = null,
    val action: String,
    val entity: String,
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

/** Body for suspending/reinstating a user account. */
@Serializable
data class SuspendUserDto(@SerialName("is_suspended") val isSuspended: Boolean)

/** Body for approving/rejecting a landlord's verification. */
@Serializable
data class VerificationDecisionDto(
    @SerialName("verification_status") val status: String,
    /** Omitted from the request when null (encodeDefaults=false), leaving existing notes untouched. */
    @SerialName("verification_notes") val notes: String? = null
)

/** Body for approving/rejecting a property listing. */
@Serializable
data class PropertyModerationDecisionDto(val status: String)

@Serializable
data class AuditLogInsertDto(
    @SerialName("actor_id") val actorId: String,
    val action: String,
    val entity: String,
    @SerialName("entity_id") val entityId: String? = null
)

@Serializable
data class AdminAccessRequestDto(val code: String)

@Serializable
data class AdminAccessResponseDto(val success: Boolean = false, val error: String? = null)
