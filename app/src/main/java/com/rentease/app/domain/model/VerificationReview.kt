package com.rentease.app.domain.model

/** What an admin can decide about a landlord (the web's `Decision` type). */
enum class VerificationDecision(val dbValue: String, val notesRequired: Boolean) {
    VERIFIED("verified", false),
    DOCUMENTS_REQUIRED("documents_required", true),
    REJECTED("rejected", true)
}

/** Filter chips on the admin verification list. */
enum class VerificationFilter {
    AWAITING_REVIEW, DOCUMENTS_REQUIRED, REJECTED, VERIFIED, ALL;

    fun matches(status: VerificationStatus): Boolean = when (this) {
        AWAITING_REVIEW -> status == VerificationStatus.SUBMITTED
        DOCUMENTS_REQUIRED -> status == VerificationStatus.DOCUMENTS_REQUIRED
        REJECTED -> status == VerificationStatus.REJECTED
        VERIFIED -> status == VerificationStatus.VERIFIED
        ALL -> true
    }
}

object VerificationReview {
    const val NOTES_MAX = 600

    /** Both required documents must be on file before an account can be verified. */
    fun hasRequiredDocuments(item: LandlordVerificationItem): Boolean {
        val kinds = item.documents.map { it.kind }.toSet()
        return LandlordDocumentKind.entries.filter { it.required }.all { it in kinds }
    }

    sealed interface Check {
        data object Ok : Check
        data object NotesRequired : Check
        data object NotesTooLong : Check
        data object MissingDocuments : Check
    }

    fun check(item: LandlordVerificationItem, decision: VerificationDecision, notes: String): Check {
        val trimmed = notes.trim()
        if (trimmed.length > NOTES_MAX) return Check.NotesTooLong
        if (decision.notesRequired && trimmed.isEmpty()) return Check.NotesRequired
        if (decision == VerificationDecision.VERIFIED && !hasRequiredDocuments(item)) return Check.MissingDocuments
        return Check.Ok
    }

    /** Notes to send with a decision: trimmed, or null to leave existing notes alone. */
    fun notesToSend(notes: String): String? = notes.trim().ifEmpty { null }
}
