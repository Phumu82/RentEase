package com.rentease.app.domain.model

import java.net.URLEncoder

/** "What's nearby" shortcuts: each opens the phone's map app searching around the property. */
enum class NearbyCategory(val query: String) {
    TRANSPORT("taxi rank bus stop"),
    SHOPS("shopping centre supermarket"),
    FOOD("restaurants cafes"),
    HEALTH("hospital clinic pharmacy")
}

/**
 * Builds `geo:` URIs for the nearby shortcuts. Uses the property's coordinates when it has them,
 * otherwise searches by its address. Pure (no Android classes) so it is unit-testable.
 */
object NearbyMaps {
    fun searchUri(category: NearbyCategory, latitude: Double?, longitude: Double?, address: String, city: String): String =
        if (latitude != null && longitude != null) {
            "geo:$latitude,$longitude?q=${encode(category.query)}"
        } else {
            "geo:0,0?q=${encode(category.query + " near " + listOf(address, city).filter { it.isNotBlank() }.joinToString(", "))}"
        }

    /** Pin for the property itself. */
    fun locationUri(title: String, latitude: Double?, longitude: Double?, address: String, city: String): String =
        if (latitude != null && longitude != null) {
            "geo:$latitude,$longitude?q=$latitude,$longitude(${encode(title)})"
        } else {
            "geo:0,0?q=${encode(listOf(address, city).filter { it.isNotBlank() }.joinToString(", "))}"
        }

    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}
