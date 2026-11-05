package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationReviewTest {
    private fun item(vararg kinds: LandlordDocumentKind, status: VerificationStatus = VerificationStatus.SUBMITTED) =
        LandlordVerificationItem(
            userId = "u", fullName = "L", email = null, businessName = null,
            idDocumentUrl = null, proofOfOwnershipUrl = null, status = status,
            documents = kinds.map { VerificationDocument(it, "u/${it.column}/f.pdf") }
        )

    private val both = arrayOf(LandlordDocumentKind.ID_DOCUMENT, LandlordDocumentKind.PROOF_OF_OWNERSHIP)

    @Test fun requiredDocumentsNeedBothIdAndOwnership() {
        assertTrue(VerificationReview.hasRequiredDocuments(item(*both)))
        assertFalse(VerificationReview.hasRequiredDocuments(item(LandlordDocumentKind.ID_DOCUMENT)))
        assertFalse(VerificationReview.hasRequiredDocuments(item(LandlordDocumentKind.PROOF_OF_ADDRESS, LandlordDocumentKind.BUSINESS_REGISTRATION)))
    }

    @Test fun verifyingWithoutRequiredDocumentsIsBlocked() {
        assertEquals(
            VerificationReview.Check.MissingDocuments,
            VerificationReview.check(item(LandlordDocumentKind.ID_DOCUMENT), VerificationDecision.VERIFIED, "")
        )
        assertEquals(VerificationReview.Check.Ok, VerificationReview.check(item(*both), VerificationDecision.VERIFIED, ""))
    }

    @Test fun rejectAndRequestDocumentsNeedNotes() {
        listOf(VerificationDecision.REJECTED, VerificationDecision.DOCUMENTS_REQUIRED).forEach {
            assertEquals(VerificationReview.Check.NotesRequired, VerificationReview.check(item(*both), it, "   "))
            assertEquals(VerificationReview.Check.Ok, VerificationReview.check(item(*both), it, "ID is blurry"))
        }
    }

    @Test fun rejectingDoesNotRequireDocuments() {
        assertEquals(VerificationReview.Check.Ok, VerificationReview.check(item(), VerificationDecision.REJECTED, "No papers"))
    }

    @Test fun overlongNotesAreRefused() {
        val long = "x".repeat(VerificationReview.NOTES_MAX + 1)
        assertEquals(VerificationReview.Check.NotesTooLong, VerificationReview.check(item(*both), VerificationDecision.REJECTED, long))
    }

    @Test fun notesToSendTrimsAndOmitsBlank() {
        assertEquals("fix it", VerificationReview.notesToSend("  fix it "))
        assertNull(VerificationReview.notesToSend("   "))
    }

    @Test fun decisionDbValuesMatchTheEnum() {
        assertEquals(listOf("verified", "documents_required", "rejected"), VerificationDecision.entries.map { it.dbValue })
    }

    @Test fun filtersMatchStatuses() {
        assertTrue(VerificationFilter.AWAITING_REVIEW.matches(VerificationStatus.SUBMITTED))
        assertFalse(VerificationFilter.AWAITING_REVIEW.matches(VerificationStatus.PENDING))
        assertTrue(VerificationStatus.entries.all { VerificationFilter.ALL.matches(it) })
        assertTrue(VerificationFilter.VERIFIED.matches(VerificationStatus.VERIFIED))
    }
}
