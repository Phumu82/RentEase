package com.rentease.app.domain.repository

import com.rentease.app.domain.model.PropertySummary
import kotlinx.coroutines.flow.Flow

sealed interface SyncState {
    data object Idle : SyncState
    data class Pending(val count: Int) : SyncState
    data object Syncing : SyncState
    data class Error(val message: String) : SyncState
}

interface SavedListingsRepository {
    /** Local-first read — always backed by Room, refreshed opportunistically
     *  from the network when online. Never throws on offline. */
    fun observeSavedListings(): Flow<List<PropertySummary>>

    /** Live count of unsynced outbox entries, for UI badges/spinners. */
    fun observeSyncState(): Flow<SyncState>

    /**
     * Toggles a listing's saved state. If online, writes straight through to
     * the API and updates the cache. If offline, updates the cache
     * optimistically and queues the write in the outbox for later sync —
     * this NEVER throws or blocks on connectivity.
     */
    suspend fun toggleSaved(property: PropertySummary)

    /** Triggers an immediate one-off sync attempt (in addition to the
     *  automatic WorkManager constraint-based trigger). */
    fun requestImmediateSync()

    /** Best-effort refresh of the local cache from the server, when online.
     *  Silently does nothing if offline — the cache simply stays as-is. */
    suspend fun refreshFromServer()
}
