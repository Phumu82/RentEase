package com.rentease.app.domain.repository

import com.rentease.app.domain.model.Complaint
import com.rentease.app.domain.model.ComplaintInput

sealed interface SupportResult<out T> {
    data class Success<T>(val data: T) : SupportResult<T>
    data class Failure(val message: String) : SupportResult<Nothing>
}

/**
 * Shared (role-agnostic) complaint submission — deliberately its own small
 * repository rather than folded into [ListingsRepository] (student-only) or
 * [LandlordRepository] (landlord-only), the same reasoning
 * [NotificationsRepository] already documents: both a student and a landlord
 * screen need this, and neither of those repositories is the right home for
 * something that belongs to whoever is signed in, regardless of role.
 */
interface SupportRepository {
    /** The signed-in user's own complaints, most recent first. */
    suspend fun getMyComplaints(): SupportResult<List<Complaint>>

    suspend fun submitComplaint(input: ComplaintInput): SupportResult<Unit>
}
