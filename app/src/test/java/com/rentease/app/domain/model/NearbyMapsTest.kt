package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyMapsTest {
    @Test fun `uses coordinates when the property has them`() {
        val uri = NearbyMaps.searchUri(NearbyCategory.FOOD, -26.2, 28.03, "1 Jorissen St", "Johannesburg")
        assertEquals("geo:-26.2,28.03?q=restaurants%20cafes", uri)
    }

    @Test fun `falls back to an address search without coordinates`() {
        val uri = NearbyMaps.searchUri(NearbyCategory.HEALTH, null, null, "12 Burnett St", "Pretoria")
        assertTrue(uri.startsWith("geo:0,0?q=hospital%20clinic%20pharmacy%20near%2012%20Burnett%20St%2C%20Pretoria"))
    }

    @Test fun `location pin labels the property`() {
        assertEquals("geo:-26.2,28.03?q=-26.2,28.03(Hatfield%20Rooms)", NearbyMaps.locationUri("Hatfield Rooms", -26.2, 28.03, "", ""))
    }

    @Test fun `blank address parts are skipped`() {
        assertEquals("geo:0,0?q=Pretoria", NearbyMaps.locationUri("x", null, null, "", "Pretoria"))
    }
}
