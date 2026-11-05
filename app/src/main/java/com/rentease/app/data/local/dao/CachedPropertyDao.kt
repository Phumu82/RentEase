package com.rentease.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rentease.app.data.local.entity.CachedPropertyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedPropertyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedPropertyEntity)

    @Query("SELECT * FROM cached_properties WHERE isFavourited = 1 ORDER BY cachedAt DESC")
    fun observeFavourited(): Flow<List<CachedPropertyEntity>>

    @Query("SELECT * FROM cached_properties WHERE id = :propertyId LIMIT 1")
    suspend fun getById(propertyId: String): CachedPropertyEntity?

    @Query("UPDATE cached_properties SET isFavourited = :favourited WHERE id = :propertyId")
    suspend fun setFavourited(propertyId: String, favourited: Boolean)

    /** Removes cache rows that are no longer favourited AND have no pending
     *  outbox entry — called after a successful sync so stale unfavourited
     *  rows don't accumulate forever. */
    @Query(
        """
        DELETE FROM cached_properties
        WHERE isFavourited = 0
        AND id NOT IN (SELECT propertyId FROM favourite_outbox WHERE status = 'PENDING' OR status = 'SYNCING')
        """
    )
    suspend fun pruneUnfavourited()
}
