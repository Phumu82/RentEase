package com.rentease.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rentease.app.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * RentEase application entry point.
 * Hilt generates the DI container graph from here; all @Inject constructors
 * and modules under [com.rentease.app.di] hang off this.
 *
 * Also configures WorkManager to use Hilt's worker factory so that
 * FavouriteSyncWorker (and any future @HiltWorker) can have its
 * dependencies (DAOs, Retrofit APIs, TokenManager) injected normally,
 * rather than manually constructed. Default WorkManager auto-initialization
 * is disabled in the manifest so this on-demand config is the one that's
 * actually used.
 */
@HiltAndroidApp
class RentEaseApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
