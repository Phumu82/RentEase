package com.rentease.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rentease.app.data.local.entity.FavouriteOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteOutboxDao {

    @Insert
    suspend fun insert(entity: FavouriteOutboxEntity): Long

    @Update
    suspend fun update(entity: FavouriteOutboxEntity)

    @Query("SELECT * FROM favourite_outbox WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getPendingAndFailed(): List<FavouriteOutboxEntity>

    @Query("SELECT COUNT(*) FROM favourite_outbox WHERE status = 'PENDING' OR status = 'SYNCING'")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM favourite_outbox WHERE status = 'SYNCED'")
    suspend fun deleteSynced()

    /**
     * "Last write wins" per §3.9: if a property has multiple pending outbox
     * rows (e.g. saved then unsaved while still offline), only the most
     * recent one should actually be replayed — the earlier ones are
     * superseded, not separately synced. Returns one row per propertyId,
     * the latest by createdAt, among PENDING/FAILED rows only.
     */
    @Query(
        """
        SELECT * FROM favourite_outbox o1
        WHERE (status = 'PENDING' OR status = 'FAILED')
        AND createdAt = (
            SELECT MAX(createdAt) FROM favourite_outbox o2
            WHERE o2.propertyId = o1.propertyId AND (o2.status = 'PENDING' OR o2.status = 'FAILED')
        )
        ORDER BY createdAt ASC
        """
    )
    suspend fun getLatestPendingPerProperty(): List<FavouriteOutboxEntity>

    /** Superseded rows (older pending entries for a property that now has a
     *  newer one) are marked SYNCED directly rather than replayed — they
     *  represent intent that's already obsolete. */
    @Query(
        """
        UPDATE favourite_outbox SET status = 'SYNCED'
        WHERE propertyId = :propertyId AND id != :keepId AND (status = 'PENDING' OR status = 'FAILED')
        """
    )
    suspend fun supersedeOlderEntriesFor(propertyId: String, keepId: Long)
}
