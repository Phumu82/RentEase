package com.rentease.app.di

import android.content.Context
import androidx.room.Room
import com.rentease.app.data.local.RentEaseDatabase
import com.rentease.app.data.local.dao.CachedPropertyDao
import com.rentease.app.data.local.dao.FavouriteOutboxDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RentEaseDatabase =
        Room.databaseBuilder(context, RentEaseDatabase::class.java, RentEaseDatabase.DATABASE_NAME)
            // Offline cache/outbox data is disposable and rebuilds itself from
            // the server — destructive migration is an acceptable trade-off
            // here (unlike a hypothetical primary datastore) since Room is
            // explicitly a CACHE + OUTBOX in this app, not the source of truth.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCachedPropertyDao(db: RentEaseDatabase): CachedPropertyDao = db.cachedPropertyDao()

    @Provides
    fun provideFavouriteOutboxDao(db: RentEaseDatabase): FavouriteOutboxDao = db.favouriteOutboxDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): androidx.work.WorkManager =
        androidx.work.WorkManager.getInstance(context)
}
