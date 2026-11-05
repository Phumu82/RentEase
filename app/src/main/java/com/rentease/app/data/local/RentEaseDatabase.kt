package com.rentease.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rentease.app.data.local.dao.CachedPropertyDao
import com.rentease.app.data.local.dao.FavouriteOutboxDao
import com.rentease.app.data.local.entity.CachedPropertyEntity
import com.rentease.app.data.local.entity.FavouriteOutboxEntity

@Database(
    entities = [CachedPropertyEntity::class, FavouriteOutboxEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(RentEaseTypeConverters::class)
abstract class RentEaseDatabase : RoomDatabase() {
    abstract fun cachedPropertyDao(): CachedPropertyDao
    abstract fun favouriteOutboxDao(): FavouriteOutboxDao

    companion object {
        const val DATABASE_NAME = "rentease.db"
    }
}
