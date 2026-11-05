package com.rentease.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of public.device_tokens (see migration 20260828000000). */
@Serializable
data class DeviceTokenDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("fcm_token") val fcmToken: String,
    val platform: String = "android"
)
