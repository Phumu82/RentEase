package com.rentease.app.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.local.dao.CachedPropertyDao
import com.rentease.app.data.local.dao.FavouriteOutboxDao
import com.rentease.app.data.local.entity.CachedPropertyEntity
import com.rentease.app.data.local.entity.FavouriteOutboxEntity
import com.rentease.app.data.local.entity.OutboxAction
import com.rentease.app.data.local.entity.OutboxStatus
import com.rentease.app.data.remote.api.SupabaseListingsApi
import com.rentease.app.data.remote.dto.FavoriteDto
import com.rentease.app.data.sync.FavouriteSyncWorker
import com.rentease.app.domain.model.PropertySummary
import com.rentease.app.domain.repository.SavedListingsRepository
import com.rentease.app.domain.repository.SyncState
import com.rentease.app.util.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedListingsRepositoryImpl @Inject constructor(
    private val cachedPropertyDao: CachedPropertyDao,
    private val outboxDao: FavouriteOutboxDao,
    private val listingsApi: SupabaseListingsApi,
    private val tokenManager: TokenManager,
    private val connectivityObserver: ConnectivityObserver,
    private val workManager: WorkManager
) : SavedListingsRepository {

    override fun observeSavedListings(): Flow<List<PropertySummary>> =
        cachedPropertyDao.observeFavourited().map { entities -> entities.map { it.toDomain() } }

    override fun observeSyncState(): Flow<SyncState> =
        outboxDao.observePendingCount().combine(connectivityObserver.observe()) { pending, online ->
            when {
                pending == 0 -> SyncState.Idle
                online -> SyncState.Syncing
                else -> SyncState.Pending(pending)
            }
        }

    override suspend fun toggleSaved(property: PropertySummary) {
        val newFavouritedState = !property.isFavourited

        // 1. Optimistic local update — the UI reflects the change instantly,
        //    regardless of connectivity, per §3.9's offline-first design.
        cachedPropertyDao.upsert(property.toEntity(isFavourited = newFavouritedState))

        val userId = tokenManager.getUserId() ?: return

        if (connectivityObserver.isCurrentlyOnline()) {
            // 2a. Online: try a direct write-through first. If it genuinely
            //     fails (not just "no network", we already checked that), fall
            //     through to the outbox so the attempt isn't lost.
            val response = runCatching {
                if (newFavouritedState) {
                    listingsApi.addFavorite(FavoriteDto(studentId = userId, propertyId = property.id))
                } else {
                    listingsApi.removeFavorite(studentIdFilter = "eq.$userId", propertyIdFilter = "eq.${property.id}")
                }
            }.getOrNull()

            if (response?.isSuccessful == true) {
                return // done — no outbox entry needed for a successful direct write
            }
        }

        // 2b. Offline (or the direct write failed): queue for later sync.
        outboxDao.insert(
            FavouriteOutboxEntity(
                propertyId = property.id,
                action = if (newFavouritedState) OutboxAction.SAVE else OutboxAction.UNSAVE,
                status = OutboxStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
        )
        enqueueSyncWork()
    }

    override fun requestImmediateSync() = enqueueSyncWork()

    override suspend fun refreshFromServer() {
        if (!connectivityObserver.isCurrentlyOnline()) return
        val userId = tokenManager.getUserId() ?: return

        val favouriteIdsResponse = runCatching { listingsApi.getFavoriteIds(studentIdFilter = "eq.$userId") }.getOrNull()
        val favouriteIds = favouriteIdsResponse?.takeIf { it.isSuccessful }?.body()?.map { it.propertyId } ?: return

        for (propertyId in favouriteIds) {
            val existing = cachedPropertyDao.getById(propertyId)
            if (existing != null) {
                cachedPropertyDao.upsert(existing.copy(isFavourited = true, cachedAt = System.currentTimeMillis()))
            }
            // Properties not already cached (never viewed on this device) are
            // intentionally NOT fetched here — that would need a per-id
            // GET /properties?id=eq.X round trip per favourite on every
            // refresh. They'll populate next time the student views/saves
            // them from the Search or Detail screen, which already upserts
            // the cache as a side effect of toggleSaved().
        }
    }

    /**
     * Enqueued with a NetworkType.CONNECTED constraint — this is what makes
     * "WorkManager network constraint" real rather than simulated: the OS
     * itself won't run this work until connectivity is actually back,
     * including after app process death or device reboot. Exponential
     * backoff handles retry timing on repeated failure (Result.retry() in
     * the Worker). ExistingWorkPolicy.APPEND_OR_REPLACE with a unique name
     * means repeated triggers (e.g. toggling several listings while offline)
     * don't queue up redundant duplicate sync runs.
     */
    private fun enqueueSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<FavouriteSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            FavouriteSyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    private fun CachedPropertyEntity.toDomain() = PropertySummary(
        id = id, slug = slug, title = title, city = city, province = province, distanceKm = distanceKm,
        pricePerMonth = pricePerMonth, bedrooms = bedrooms, bathrooms = bathrooms, roomsAvailable = roomsAvailable,
        furnished = furnished, verified = verified, rating = rating, reviewCount = reviewCount,
        coverImageUrl = coverImageUrl, isFavourited = isFavourited
    )

    private fun PropertySummary.toEntity(isFavourited: Boolean) = CachedPropertyEntity(
        id = id, slug = slug, title = title, city = city, province = province, distanceKm = distanceKm,
        pricePerMonth = pricePerMonth, bedrooms = bedrooms, bathrooms = bathrooms, roomsAvailable = roomsAvailable,
        furnished = furnished, verified = verified, rating = rating, reviewCount = reviewCount,
        coverImageUrl = coverImageUrl, isFavourited = isFavourited, cachedAt = System.currentTimeMillis()
    )
}
