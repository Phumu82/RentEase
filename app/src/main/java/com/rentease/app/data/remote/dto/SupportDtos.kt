package com.rentease.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComplaintDto(
    val id: String,
    val subject: String = "",
    val category: String = "Other",
    val description: String = "",
    val status: String = "open",
    @SerialName("admin_response") val adminResponse: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("reporter_id") val reporterId: String? = null
)

/** POST body for raising a complaint. `reporter_id` is sent explicitly
 *  because the RLS policy "Users insert own complaints" checks
 *  `reporter_id = auth.uid()` against the submitted row. */
@Serializable
data class ComplaintCreateDto(
    @SerialName("reporter_id") val reporterId: String,
    val subject: String,
    val category: String,
    val description: String
)

/**
 * PATCH body for an admin decision. `resolved_at` mirrors the web app's
 * server function exactly: set the moment the status becomes resolved or
 * closed, null otherwise (e.g. moving something back to "in_progress").
 */
@Serializable
data class ComplaintDecisionDto(
    val status: String,
    @SerialName("admin_response") val adminResponse: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null
)
