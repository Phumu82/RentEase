package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PropertyComparisonTest {

    private fun detail(
        slug: String,
        rent: Double,
        km: Double,
        rating: Double = 0.0,
        reviews: Int = 0
    ) = PropertyDetail(
        summary = PropertySummary(
            id = "id-$slug", slug = slug, title = "Title $slug", city = "Pretoria", province = "Gauteng",
            distanceKm = km, pricePerMonth = rent, bedrooms = 2, bathrooms = 1, roomsAvailable = 1,
            furnished = true, verified = true, rating = rating, reviewCount = reviews, coverImageUrl = null
        ),
        description = "", address = "", deposit = rent, amenities = emptyList(),
        latitude = null, longitude = null, landlordName = "L", images = emptyList(), reviews = emptyList()
    )

    @Test
    fun `toggleSelection adds, removes and caps at three`() {
        var selected = emptyList<String>()
        selected = PropertyComparison.toggleSelection(selected, "a")
        selected = PropertyComparison.toggleSelection(selected, "b")
        selected = PropertyComparison.toggleSelection(selected, "c")
        assertEquals(listOf("a", "b", "c"), selected)
        // A 4th listing is ignored while three are selected.
        assertEquals(listOf("a", "b", "c"), PropertyComparison.toggleSelection(selected, "d"))
        // Tapping a selected one removes it.
        assertEquals(listOf("a", "c"), PropertyComparison.toggleSelection(selected, "b"))
    }

    @Test
    fun `canCompare requires two or three listings`() {
        assertFalse(PropertyComparison.canCompare(emptyList()))
        assertFalse(PropertyComparison.canCompare(listOf("a")))
        assertTrue(PropertyComparison.canCompare(listOf("a", "b")))
        assertTrue(PropertyComparison.canCompare(listOf("a", "b", "c")))
        assertFalse(PropertyComparison.canCompare(listOf("a", "b", "c", "d")))
    }

    @Test
    fun `bestIndices picks the lowest for cost-like rows and the highest for score-like rows`() {
        assertEquals(setOf(1), PropertyComparison.bestIndices(listOf(3000.0, 2500.0, 2800.0), higherIsBetter = false))
        assertEquals(setOf(0), PropertyComparison.bestIndices(listOf(4.8, 4.1, null), higherIsBetter = true))
    }

    @Test
    fun `bestIndices returns every tied winner`() {
        assertEquals(setOf(0, 2), PropertyComparison.bestIndices(listOf(1.0, 5.0, 1.0), higherIsBetter = false))
    }

    @Test
    fun `bestIndices highlights nothing when all values are equal or fewer than two exist`() {
        assertTrue(PropertyComparison.bestIndices(listOf(2.0, 2.0, 2.0), higherIsBetter = false).isEmpty())
        assertTrue(PropertyComparison.bestIndices(listOf(2.0, null, null), higherIsBetter = true).isEmpty())
        assertTrue(PropertyComparison.bestIndices(emptyList(), higherIsBetter = true).isEmpty())
    }

    @Test
    fun `summarize names the cheapest, closest and top-rated listing`() {
        val items = listOf(
            detail("a", rent = 3000.0, km = 0.5, rating = 4.0, reviews = 10),
            detail("b", rent = 2500.0, km = 2.0, rating = 4.9, reviews = 3),
            detail("c", rent = 2800.0, km = 1.0)
        )
        val summary = PropertyComparison.summarize(items)
        assertEquals("b", summary.lowestRent!!.summary.slug)
        assertEquals("a", summary.closest!!.summary.slug)
        assertEquals("b", summary.topRated!!.summary.slug)
    }

    @Test
    fun `unreviewed listings are never called top rated`() {
        val items = listOf(detail("a", 1.0, 1.0, rating = 0.0, reviews = 0), detail("b", 2.0, 2.0, rating = 0.0, reviews = 0))
        assertNull(PropertyComparison.summarize(items).topRated)
        assertNull(PropertyComparison.ratingOrNull(items[0].summary))
    }
}
