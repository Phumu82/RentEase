package com.rentease.app.domain.model

/**
 * Room types a landlord can pick (same values as the website's ROOM_TYPES) with the typical
 * monthly rent range for student rooms near campus. The range is GUIDANCE shown under the
 * price field, never stored or enforced: landlords can charge what they like.
 */
enum class RoomTypeGuide(val dbValue: String, val typicalMin: Int, val typicalMax: Int, val defaultCapacity: Int) {
    SINGLE("Single", 3_500, 5_500, 1),
    SHARED("Shared", 2_800, 3_800, 2),
    ENSUITE("Ensuite", 4_500, 6_500, 1),
    STUDIO("Studio", 4_500, 7_000, 1);

    /** True when [price] is far outside the typical range (under half the minimum or over double the maximum). */
    fun looksUnrealistic(price: Double): Boolean = price < typicalMin / 2.0 || price > typicalMax * 2.0

    companion object {
        fun fromDb(value: String?): RoomTypeGuide? = entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) }
    }
}
