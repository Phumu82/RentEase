package com.rentease.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of properties the student has saved, so the Saved Listings
 * screen has something real to show when there is no connectivity (Part 1
 * §3.9: "the Saved Listings ... screens read from this local cache").
 * Written whenever a favourite toggle succeeds (online) or is queued
 * (offline, optimistically) — see SavedListingsRepositoryImpl.
 */
@Entity(tableName = "cached_properties")
data class CachedPropertyEntity(
    @PrimaryKey val id: String,
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
    /** True while the row is favourited (still shown even if unsynced — see
     *  OutboxEntity for the pending-sync flag). Rows with isFavourited=false
     *  are pruned during the next successful sync/refresh, not kept forever. */
    val isFavourited: Boolean,
    val cachedAt: Long
)
