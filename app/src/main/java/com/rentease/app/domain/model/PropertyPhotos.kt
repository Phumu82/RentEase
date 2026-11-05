package com.rentease.app.domain.model

/**
 * Ordered photo list for a listing. The FIRST photo is the cover shown on cards, like the
 * website's gallery. Pure functions so the rules can be unit-tested.
 */
object PropertyPhotos {
    const val MAX_PHOTOS = 10

    /** Existing gallery plus the legacy single cover (kept first), without duplicates. */
    fun initial(cover: String?, gallery: List<String>): List<String> =
        (listOfNotNull(cover?.takeIf { it.isNotBlank() }) + gallery).distinct().take(MAX_PHOTOS)

    fun add(current: List<String>, url: String): List<String> =
        if (url in current || current.size >= MAX_PHOTOS) current else current + url

    fun remove(current: List<String>, url: String): List<String> = current - url

    fun makeCover(current: List<String>, url: String): List<String> =
        if (url !in current) current else listOf(url) + (current - url)

    fun remainingSlots(current: List<String>): Int = (MAX_PHOTOS - current.size).coerceAtLeast(0)
}
