package com.rentease.app.domain.model

/**
 * Mirrors the Postgres `role` enum from the RentEase schema (student, landlord, admin).
 * Admin is never selectable at public registration — it can only be granted via the
 * server-side admin-access flow (see AdminAccessRepository), matching the web app's
 * admin-access.server.ts behaviour.
 */
enum class UserRole {
    STUDENT,
    LANDLORD,
    ADMIN;

    companion object {
        fun fromDb(value: String): UserRole = when (value.lowercase()) {
            "student" -> STUDENT
            "landlord" -> LANDLORD
            "admin" -> ADMIN
            else -> throw IllegalArgumentException("Unknown role from server: $value")
        }
    }

    fun toDb(): String = name.lowercase()
}
