package com.rentease.app.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.local.RentEaseDatabase
import com.rentease.app.data.local.entity.FavouriteOutboxEntity
import com.rentease.app.data.local.entity.OutboxAction
import com.rentease.app.data.local.entity.OutboxStatus
import com.rentease.app.data.remote.api.SupabaseListingsApi
import com.rentease.app.data.remote.dto.FavoriteDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

/**
 * Tests FavouriteSyncWorker's actual doWork() logic — real Room reads/writes
 * against an in-memory database, a mocked API layer (network is what
 * genuinely can't be tested here, not the sync logic itself), and
 * TestListenableWorkerBuilder (androidx.work.testing) to construct the
 * worker the same way WorkManager itself would. Covers the PoE-mandatory
 * "WorkManager synchronisation" test requirement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavouriteSyncWorkerTest {

    private lateinit var db: RentEaseDatabase
    private lateinit var api: SupabaseListingsApi
    private lateinit var tokenManager: TokenManager
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, RentEaseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = mockk()
        tokenManager = mockk()
        every { tokenManager.getUserId() } returns "user-1"
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun buildWorker(): FavouriteSyncWorker =
        TestListenableWorkerBuilder<FavouriteSyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context, workerClassName: String, workerParameters: WorkerParameters
                ): ListenableWorker = FavouriteSyncWorker(
                    appContext, workerParameters,
                    db.favouriteOutboxDao(), db.cachedPropertyDao(), api, tokenManager
                )
            })
            .build() as FavouriteSyncWorker

    @Test
    fun `doWork succeeds and marks the outbox entry SYNCED when the API call succeeds`() = runTest {
        db.favouriteOutboxDao().insert(
            FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.SAVE, status = OutboxStatus.PENDING, createdAt = 1L)
        )
        coEvery { api.addFavorite(FavoriteDto(studentId = "user-1", propertyId = "prop-1")) } returns Response.success(Unit)

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertTrue(db.favouriteOutboxDao().getPendingAndFailed().isEmpty()) // deleteSynced() already ran
    }

    @Test
    fun `doWork retries when the API call fails rather than silently dropping the entry`() = runTest {
        db.favouriteOutboxDao().insert(
            FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.SAVE, status = OutboxStatus.PENDING, createdAt = 1L)
        )
        coEvery { api.addFavorite(any()) } returns Response.error(500, "".toResponseBody("application/json".toMediaType()))

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        val remaining = db.favouriteOutboxDao().getPendingAndFailed()
        assertEquals(1, remaining.size)
        assertEquals(OutboxStatus.FAILED, remaining.first().status)
        assertEquals(1, remaining.first().retryCount)
    }

    @Test
    fun `doWork applies last-write-wins, only replaying the newest entry for a property`() = runTest {
        val dao = db.favouriteOutboxDao()
        dao.insert(FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.SAVE, status = OutboxStatus.PENDING, createdAt = 100L))
        dao.insert(FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.UNSAVE, status = OutboxStatus.PENDING, createdAt = 200L))
        coEvery { api.removeFavorite(studentIdFilter = "eq.user-1", propertyIdFilter = "eq.prop-1") } returns Response.success(Unit)

        buildWorker().doWork()

        // Only the UNSAVE (the later action) should have actually called the API —
        // verified indirectly: SAVE's addFavorite was never stubbed, so if it had
        // been called this test would throw a MockK "no answer found" exception.
        assertTrue(dao.getPendingAndFailed().isEmpty())
    }

    @Test
    fun `doWork succeeds immediately when there is nothing pending`() = runTest {
        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun `doWork succeeds without calling the API when the user is signed out`() = runTest {
        every { tokenManager.getUserId() } returns null
        db.favouriteOutboxDao().insert(
            FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.SAVE, status = OutboxStatus.PENDING, createdAt = 1L)
        )

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }
}
