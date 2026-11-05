package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UniversityDirectoryTest {
    private val all = listOf(
        University("1", "University of Johannesburg", "UJ", "Johannesburg", "Gauteng", students = 50000, averageRent = 4200.0),
        University("2", "University of Cape Town", "UCT", "Cape Town", "Western Cape"),
        University("3", "Wits University", "Wits", "Johannesburg", "Gauteng")
    )

    @Test fun blankKeywordReturnsEverything() {
        assertEquals(all, UniversityDirectory.filter(all, "  "))
    }

    @Test fun matchesNameShortNameCityAndProvinceCaseInsensitively() {
        assertEquals(listOf("2"), UniversityDirectory.filter(all, "cape").map { it.id })
        assertEquals(listOf("1"), UniversityDirectory.filter(all, "uj").map { it.id })
        assertEquals(listOf("1", "3"), UniversityDirectory.filter(all, "JOHANNESBURG").map { it.id })
        assertEquals(listOf("1", "3"), UniversityDirectory.filter(all, "gauteng").map { it.id })
    }

    @Test fun noMatchReturnsEmpty() {
        assertEquals(emptyList<University>(), UniversityDirectory.filter(all, "zzz"))
    }
}
