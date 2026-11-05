package com.rentease.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rentease.app.data.local.entity.CachedPropertyEntity
import com.rentease.app.data.local.entity.FavouriteOutboxEntity
import com.rentease.app.data.local.entity.OutboxAction
import com.rentease.app.data.local.entity.OutboxStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real Room DAO tests against an in-memory (but genuine SQLite) database —
 * not mocked DAOs. Covers the PoE-mandatory "Room DAOs" and "offline
 * operations" test requirements.
 *
 * @Config(sdk = [34]) pins Robolectric to API 34 explicitly — Robolectric
 * 4.13 doesn't yet ship shadows for API 35 (this project's compileSdk),
 * and without pinning, Robolectric would try to use the manifest's
 * targetSdkVersion and fail before any test body runs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RentEaseDatabaseTest {

    private lateinit var db: RentEaseDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RentEaseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleProperty(id: String = "prop-1", favourited: Boolean = true) = CachedPropertyEntity(
        id = id, slug = "slug-$id", title = "Test Property", city = "Pretoria", province = "Gauteng",
        distanceKm = 1.2, pricePerMonth = 3500.0, bedrooms = 1, bathrooms = 1, roomsAvailable = 1,
        furnished = true, verified = true, rating = 4.5, reviewCount = 3, coverImageUrl = null,
        isFavourited = favourited, cachedAt = System.currentTimeMillis()
    )

    // ---------------- CachedPropertyDao ----------------

    @Test
    fun `upsert then observeFavourited returns the row when favourited`() = runTest {
        db.cachedPropertyDao().upsert(sampleProperty(favourited = true))

        val results = db.cachedPropertyDao().observeFavourited().first()

        assertEquals(1, results.size)
        assertEquals("prop-1", results.first().id)
    }

    @Test
    fun `observeFavourited excludes rows that are not favourited`() = runTest {
        db.cachedPropertyDao().upsert(sampleProperty(id = "prop-1", favourited = false))

        val results = db.cachedPropertyDao().observeFavourited().first()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `upsert with the same id replaces the existing row rather than duplicating`() = runTest {
        db.cachedPropertyDao().upsert(sampleProperty(id = "prop-1").copy(title = "Old Title"))
        db.cachedPropertyDao().upsert(sampleProperty(id = "prop-1").copy(title = "New Title"))

        val results = db.cachedPropertyDao().observeFavourited().first()

        assertEquals(1, results.size)
        assertEquals("New Title", results.first().title)
    }

    @Test
    fun `setFavourited toggles the flag without needing a full upsert`() = runTest {
        db.cachedPropertyDao().upsert(sampleProperty(favourited = true))

        db.cachedPropertyDao().setFavourited("prop-1", false)

        assertEquals(false, db.cachedPropertyDao().getById("prop-1")?.isFavourited)
    }

    @Test
    fun `pruneUnfavourited removes unfavourited rows with no pending outbox entry`() = runTest {
        db.cachedPropertyDao().upsert(sampleProperty(id = "prop-1", favourited = false))

        db.cachedPropertyDao().pruneUnfavourited()

        assertNull(db.cachedPropertyDao().getById("prop-1"))
    }

    @Test
    fun `pruneUnfavourited keeps an unfavourited row that still has a pending outbox entry`() = runTest {
        db.cachedPropertyDao().upsert(sampleProperty(id = "prop-1", favourited = false))
        db.favouriteOutboxDao().insert(
            FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.UNSAVE, status = OutboxStatus.PENDING, createdAt = 1L)
        )

        db.cachedPropertyDao().pruneUnfavourited()

        assertEquals("prop-1", db.cachedPropertyDao().getById("prop-1")?.id)
    }

    // ---------------- FavouriteOutboxDao ----------------

    @Test
    fun `insert then getPendingAndFailed returns the queued entry`() = runTest {
        db.favouriteOutboxDao().insert(
            FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.SAVE, status = OutboxStatus.PENDING, createdAt = 1L)
        )

        val pending = db.favouriteOutboxDao().getPendingAndFailed()

        assertEquals(1, pending.size)
        assertEquals(OutboxAction.SAVE, pending.first().action)
    }

    @Test
    fun `getLatestPendingPerProperty returns only the newest entry when there are duplicates`() = runTest {
        val dao = db.favouriteOutboxDao()
        dao.insert(FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.SAVE, status = OutboxStatus.PENDING, createdAt = 100L))
        dao.insert(FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.UNSAVE, status = OutboxStatus.PENDING, createdAt = 200L))

        val latest = dao.getLatestPendingPerProperty()

        assertEquals(1, latest.size)
        assertEquals(OutboxAction.UNSAVE, latest.first().action)
        assertEquals(200L, latest.first().createdAt)
    }

    @Test
    fun `supersedeOlderEntriesFor marks the older duplicate as SYNCED without touching the kept row`() = runTest {
        val dao = db.favouriteOutboxDao()
        val olderId = dao.insert(FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.SAVE, status = OutboxStatus.PENDING, createdAt = 100L))
        val newerId = dao.insert(FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.UNSAVE, status = OutboxStatus.PENDING, createdAt = 200L))

        dao.supersedeOlderEntriesFor("prop-1", keepId = newerId)

        val remaining = dao.getPendingAndFailed()
        assertEquals(1, remaining.size)
        assertEquals(newerId, remaining.first().id)
    }

    @Test
    fun `deleteSynced removes only SYNCED rows`() = runTest {
        val dao = db.favouriteOutboxDao()
        dao.insert(FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.SAVE, status = OutboxStatus.SYNCED, createdAt = 1L))
        dao.insert(FavouriteOutboxEntity(propertyId = "prop-2", action = OutboxAction.SAVE, status = OutboxStatus.PENDING, createdAt = 2L))

        dao.deleteSynced()

        val remaining = dao.getPendingAndFailed()
        assertEquals(1, remaining.size)
        assertEquals("prop-2", remaining.first().propertyId)
    }

    @Test
    fun `observePendingCount reflects PENDING and SYNCING rows only`() = runTest {
        val dao = db.favouriteOutboxDao()
        dao.insert(FavouriteOutboxEntity(propertyId = "prop-1", action = OutboxAction.SAVE, status = OutboxStatus.PENDING, createdAt = 1L))
        dao.insert(FavouriteOutboxEntity(propertyId = "prop-2", action = OutboxAction.SAVE, status = OutboxStatus.SYNCING, createdAt = 2L))
        dao.insert(FavouriteOutboxEntity(propertyId = "prop-3", action = OutboxAction.SAVE, status = OutboxStatus.SYNCED, createdAt = 3L))

        val count = dao.observePendingCount().first()

        assertEquals(2, count)
    }
}
