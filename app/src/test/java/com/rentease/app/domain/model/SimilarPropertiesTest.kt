package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarPropertiesTest {
    private fun p(id: String, price: Double, rooms: Int = 1) = PropertySummary(
        id = id, slug = id, title = id, city = "Johannesburg", province = "Gauteng", distanceKm = 1.0,
        pricePerMonth = price, bedrooms = 1, bathrooms = 1, roomsAvailable = rooms, furnished = false,
        verified = true, rating = 4.0, reviewCount = 1, coverImageUrl = null
    )

    @Test fun `excludes the current property and orders by closest rent`() {
        val cur = p("cur", 4000.0)
        val out = SimilarProperties.rank(cur, listOf(p("cur", 4000.0), p("far", 9000.0), p("near", 4200.0)))
        assertEquals(listOf("near", "far"), out.map { it.id })
    }

    @Test fun `places available listings before full ones`() {
        val cur = p("cur", 4000.0)
        val out = SimilarProperties.rank(cur, listOf(p("full", 4000.0, rooms = 0), p("open", 8000.0)))
        assertEquals(listOf("open", "full"), out.map { it.id })
    }

    @Test fun `caps the result at the limit`() {
        val out = SimilarProperties.rank(p("cur", 1.0), (1..10).map { p("p$it", it * 100.0) })
        assertEquals(SimilarProperties.LIMIT, out.size)
    }
}
