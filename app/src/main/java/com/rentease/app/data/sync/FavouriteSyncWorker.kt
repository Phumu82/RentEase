package com.rentease.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rentease.app.data.local.dao.CachedPropertyDao
import com.rentease.app.data.local.dao.FavouriteOutboxDao
import com.rentease.app.data.local.entity.OutboxAction
import com.rentease.app.data.local.entity.OutboxStatus
import com.rentease.app.data.remote.api.SupabaseListingsApi
import com.rentease.app.data.remote.dto.FavoriteDto
import com.rentease.app.data.auth.TokenManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The WorkManager half of the outbox mechanism described in Part 1 §3.9:
 * "When connectivity returns, a WorkManager background job replays the
 * outbox against the API in order, resolves conflicts by 'last write wins'
 * on a per-record basis, and clears successfully-synced entries."
 *
 * Enqueued by SavedListingsRepositoryImpl with a NetworkType.CONNECTED
 * constraint (see WorkManagerModule / SyncScheduler) so it only actually
 * runs once the device has connectivity again — WorkManager itself handles
 * waiting for that constraint, retrying on failure with backoff, and
 * surviving process death/reboot (this Worker is enqueued as
 * ExistingWorkPolicy.APPEND_OR_REPLACE via a unique work name so duplicate
 * triggers don't pile up redundant runs).
 */
@HiltWorker
class FavouriteSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outboxDao: FavouriteOutboxDao,
    private val cachedPropertyDao: CachedPropertyDao,
    private val listingsApi: SupabaseListingsApi,
    private val tokenManager: TokenManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = tokenManager.getUserId() ?: return Result.success() // nothing to sync if signed out

        // "Last write wins" per property — supersede stale duplicates first.
        val latestPerProperty = outboxDao.getLatestPendingPerProperty()
        for (entry in latestPerProperty) {
            outboxDao.supersedeOlderEntriesFor(entry.propertyId, entry.id)
        }

        var anyFailure = false

        for (entry in latestPerProperty) {
            outboxDao.update(entry.copy(status = OutboxStatus.SYNCING))

            val response = try {
                when (entry.action) {
                    OutboxAction.SAVE -> listingsApi.addFavorite(FavoriteDto(studentId = userId, propertyId = entry.propertyId))
                    OutboxAction.UNSAVE -> listingsApi.removeFavorite(
                        studentIdFilter = "eq.$userId",
                        propertyIdFilter = "eq.${entry.propertyId}"
                    )
                }
            } catch (e: Exception) {
                null // treated as failure below — network error mid-sync
            }

            if (response != null && response.isSuccessful) {
                outboxDao.update(entry.copy(status = OutboxStatus.SYNCED))
                cachedPropertyDao.setFavourited(entry.propertyId, entry.action == OutboxAction.SAVE)
            } else {
                anyFailure = true
                val newRetryCount = entry.retryCount + 1
                outboxDao.update(
                    entry.copy(
                        status = OutboxStatus.FAILED,
                        retryCount = newRetryCount,
                        lastError = response?.let { "HTTP ${it.code()}" } ?: "Network error"
                    )
                )
            }
        }

        outboxDao.deleteSynced()
        cachedPropertyDao.pruneUnfavourited()

        // WorkManager's own exponential backoff (configured at enqueue time)
        // handles retry timing — Result.retry() here just tells it "not done".
        return if (anyFailure) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "favourite_outbox_sync"
    }
}
