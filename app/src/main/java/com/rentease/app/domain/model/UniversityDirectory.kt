package com.rentease.app.domain.model

/** Search over the public universities directory (name, short name, city, province). */
object UniversityDirectory {
    fun filter(all: List<University>, keyword: String): List<University> {
        val q = keyword.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter {
            it.name.lowercase().contains(q) || it.shortName.lowercase().contains(q) ||
                it.city.lowercase().contains(q) || it.province.lowercase().contains(q)
        }
    }
}
