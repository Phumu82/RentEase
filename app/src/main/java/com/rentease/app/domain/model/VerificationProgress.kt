package com.rentease.app.domain.model

/**
 * Gamified landlord onboarding (user-defined feature): five milestones from
 * "account created" to "listing live", a progress fraction and a badge that
 * levels up as milestones are completed. Pure logic - derived entirely from the
 * landlord's real profile (landlord_profiles.verification_status) and real
 * properties (properties.status), so it cannot drift from the backend state.
 */
enum class VerificationStep {
    ACCOUNT_CREATED,
    DOCUMENTS_SUBMITTED,
    VERIFIED_BY_ADMIN,
    FIRST_PROPERTY_ADDED,
    LISTING_PUBLISHED
}

enum class VerificationBadge { NEWCOMER, DOCUMENTS_IN, VERIFIED_HOST, LISTING_CREATOR, FULLY_LIVE }

data class VerificationProgress(val completed: Set<VerificationStep>) {
    val totalSteps: Int get() = VerificationStep.entries.size
    val completedCount: Int get() = completed.size
    val fraction: Float get() = completedCount.toFloat() / totalSteps
    val isComplete: Boolean get() = completedCount == totalSteps

    /** First milestone (in order) not yet completed, or null when everything is done. */
    val nextStep: VerificationStep? get() = VerificationStep.entries.firstOrNull { it !in completed }

    /** Badge earned so far; the account itself always earns at least NEWCOMER. */
    val badge: VerificationBadge
        get() = VerificationBadge.entries[(completedCount - 1).coerceIn(0, VerificationBadge.entries.size - 1)]

    fun isCompleted(step: VerificationStep): Boolean = step in completed
}

object VerificationProgressCalculator {

    fun compute(profile: LandlordProfile, properties: List<LandlordProperty>): VerificationProgress {
        val done = mutableSetOf(VerificationStep.ACCOUNT_CREATED)
        if (profile.verificationStatus in DOCUMENTS_SUBMITTED_STATUSES) done += VerificationStep.DOCUMENTS_SUBMITTED
        if (profile.verificationStatus == VerificationStatus.VERIFIED) done += VerificationStep.VERIFIED_BY_ADMIN
        if (properties.isNotEmpty()) done += VerificationStep.FIRST_PROPERTY_ADDED
        if (properties.any { it.status == PropertyStatus.PUBLISHED }) done += VerificationStep.LISTING_PUBLISHED
        return VerificationProgress(done)
    }

    /** Documents have been handed in once the account is past PENDING / DOCUMENTS_REQUIRED. */
    private val DOCUMENTS_SUBMITTED_STATUSES = setOf(
        VerificationStatus.SUBMITTED,
        VerificationStatus.VERIFIED,
        VerificationStatus.REJECTED
    )
}
