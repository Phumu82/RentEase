package com.rentease.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OutboxAction { SAVE, UNSAVE }

enum class OutboxStatus { PENDING, SYNCING, SYNCED, FAILED }

/**
 * A single deferred write, per Part 1 §3.9's outbox design:
 *   "Any action performed offline that RentEase can defer ... is queued in a
 *   local outbox table with a pending flag. When connectivity returns, a
 *   WorkManager background job replays the outbox against the API in order,
 *   resolves conflicts by 'last write wins' on a per-record basis, and
 *   clears successfully-synced entries."
 *
 * "Last write wins" here means: if two outbox rows exist for the same
 * propertyId (e.g. save then unsave while still offline), only the LATEST
 * one is replayed — see FavouriteOutboxDao.getLatestPendingPerProperty().
 */
@Entity(tableName = "favourite_outbox")
data class FavouriteOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: String,
    val action: OutboxAction,
    val status: OutboxStatus,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)
