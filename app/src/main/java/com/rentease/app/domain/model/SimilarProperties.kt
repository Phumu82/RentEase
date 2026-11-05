package com.rentease.app.domain.model

/** Picks "similar properties" for a detail page: same area, closest rent first, never itself. */
object SimilarProperties {
    const val LIMIT = 4

    fun rank(current: PropertySummary, candidates: List<PropertySummary>, limit: Int = LIMIT): List<PropertySummary> =
        candidates
            .filter { it.id != current.id }
            .sortedWith(
                compareByDescending<PropertySummary> { it.roomsAvailable > 0 }
                    .thenBy { kotlin.math.abs(it.pricePerMonth - current.pricePerMonth) }
            )
            .take(limit)
}
