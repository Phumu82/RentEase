package com.rentease.app.domain.model

/**
 * Pure logic for the side-by-side listing comparison (user-defined feature).
 * Kept free of Android/Compose types so it is fully unit-testable.
 */
object PropertyComparison {
    const val MIN_ITEMS = 2
    const val MAX_ITEMS = 3

    /** Adds [slug] (up to [MAX_ITEMS]) or removes it if already selected. */
    fun toggleSelection(selected: List<String>, slug: String): List<String> = when {
        slug in selected -> selected - slug
        selected.size >= MAX_ITEMS -> selected
        else -> selected + slug
    }

    fun canCompare(selected: List<String>): Boolean = selected.size in MIN_ITEMS..MAX_ITEMS

    /**
     * Indices of the best value(s) in [values] (ties all win). Null entries are
     * ignored. Returns an empty set when fewer than two values exist or when
     * every value is identical - "best" is meaningless if nothing differs.
     */
    fun bestIndices(values: List<Double?>, higherIsBetter: Boolean): Set<Int> {
        val present = values.withIndex().filter { it.value != null }
        if (present.size < 2) return emptySet()
        val numbers = present.map { it.value!! }
        val best = if (higherIsBetter) numbers.max() else numbers.min()
        if (numbers.all { it == best }) return emptySet()
        return present.filter { it.value == best }.map { it.index }.toSet()
    }

    /** A listing with no reviews has no meaningful rating. */
    fun ratingOrNull(summary: PropertySummary): Double? =
        if (summary.reviewCount > 0) summary.rating else null

    data class Summary(
        val lowestRent: PropertyDetail?,
        val closest: PropertyDetail?,
        val topRated: PropertyDetail?
    )

    fun summarize(items: List<PropertyDetail>): Summary {
        fun pick(values: List<Double?>, higherIsBetter: Boolean): PropertyDetail? =
            bestIndices(values, higherIsBetter).firstOrNull()?.let { items.getOrNull(it) }
        return Summary(
            lowestRent = pick(items.map { it.summary.pricePerMonth }, higherIsBetter = false),
            closest = pick(items.map { it.summary.distanceKm }, higherIsBetter = false),
            topRated = pick(items.map { ratingOrNull(it.summary) }, higherIsBetter = true)
        )
    }
}
