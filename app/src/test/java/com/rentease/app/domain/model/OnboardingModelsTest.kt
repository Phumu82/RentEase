package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingModelsTest {

    private fun validStudent() = StudentOnboardingInput(
        fullName = "Thandi Nkosi", phone = "0821234567", studentNumber = "2024001", universityId = "uni-1",
        course = "BSc IT", yearOfStudy = 2, budgetMax = 4500.0, preferredRoomType = "Single room",
        earliestMoveIn = "2027-01-15", needsFurnished = true
    )

    private fun validLandlord() = LandlordOnboardingInput(
        displayName = "Sipho", contactPhone = "0831112222", legalName = "Sipho Dlamini", idNumber = "8001015009087",
        businessName = "", businessRegistrationNumber = "", businessAddress = "12 Main Rd", city = "Johannesburg",
        province = "Gauteng", postalCode = ""
    )

    @Test fun `valid student input has no problems`() = assertTrue(validStudent().validate().isEmpty())

    @Test fun `student phone is optional like the web`() =
        assertTrue(validStudent().copy(phone = "").validate().isEmpty())

    @Test fun `student validation flags every missing required field`() {
        val result = validStudent().copy(
            fullName = " ", studentNumber = "", universityId = null, course = "", budgetMax = null, earliestMoveIn = null
        ).validate()
        assertEquals(
            setOf(
                OnboardingField.FULL_NAME, OnboardingField.STUDENT_NUMBER, OnboardingField.UNIVERSITY,
                OnboardingField.COURSE, OnboardingField.BUDGET, OnboardingField.EARLIEST_MOVE_IN
            ),
            result
        )
    }

    @Test fun `zero or negative budget is invalid`() {
        assertTrue(OnboardingField.BUDGET in validStudent().copy(budgetMax = 0.0).validate())
        assertTrue(OnboardingField.BUDGET in validStudent().copy(budgetMax = -5.0).validate())
    }

    @Test fun `valid landlord input has no problems and company details are optional`() =
        assertTrue(validLandlord().validate().isEmpty())

    @Test fun `landlord validation flags missing required fields and unknown province`() {
        val result = validLandlord().copy(
            displayName = "", contactPhone = "", legalName = "", idNumber = "", businessAddress = "", city = "", province = "Atlantis"
        ).validate()
        assertEquals(
            setOf(
                OnboardingField.FULL_NAME, OnboardingField.CONTACT_PHONE, OnboardingField.LEGAL_NAME, OnboardingField.ID_NUMBER,
                OnboardingField.BUSINESS_ADDRESS, OnboardingField.CITY, OnboardingField.PROVINCE
            ),
            result
        )
    }

    // ---- documents ----

    @Test fun `document rules accept the four web types up to 5 MB`() {
        DocumentRules.ACCEPTED_MIME_TYPES.forEach { assertNull(DocumentRules.validate(it, 1024)) }
        assertNull(DocumentRules.validate("application/pdf", DocumentRules.MAX_BYTES))
    }

    @Test fun `document rules reject empty, wrong type and oversize`() {
        assertEquals(DocumentRules.Problem.EMPTY, DocumentRules.validate("application/pdf", 0))
        assertEquals(DocumentRules.Problem.UNSUPPORTED_TYPE, DocumentRules.validate("application/zip", 100))
        assertEquals(DocumentRules.Problem.UNSUPPORTED_TYPE, DocumentRules.validate(null, 100))
        assertEquals(DocumentRules.Problem.TOO_LARGE, DocumentRules.validate("image/png", DocumentRules.MAX_BYTES + 1))
    }

    @Test fun `object path starts with the user id folder required by storage policy`() {
        val path = StoragePaths.privateObjectPath("user-1", "id_document_url", "My ID (scan).pdf", 1700000000000)
        assertEquals("user-1/id_document_url/1700000000000-My-ID--scan-.pdf", path)
        assertEquals("user-1", path.substringBefore('/'))
    }

    @Test fun `safe name keeps only the last 80 characters`() {
        val name = "a".repeat(200) + ".pdf"
        assertEquals(80, StoragePaths.safeName(name).length)
        assertTrue(StoragePaths.safeName(name).endsWith(".pdf"))
    }

    // ---- verification submission ----

    private val both = mapOf(
        LandlordDocumentKind.ID_DOCUMENT to "u/id.pdf",
        LandlordDocumentKind.PROOF_OF_OWNERSHIP to "u/own.pdf"
    )

    @Test fun `submitted only once both required documents exist`() {
        assertFalse(shouldMarkSubmitted(VerificationStatus.PENDING, mapOf(LandlordDocumentKind.ID_DOCUMENT to "u/id.pdf")))
        assertTrue(shouldMarkSubmitted(VerificationStatus.PENDING, both))
    }

    @Test fun `resubmission allowed after documents_required or rejection`() {
        assertTrue(shouldMarkSubmitted(VerificationStatus.DOCUMENTS_REQUIRED, both))
        assertTrue(shouldMarkSubmitted(VerificationStatus.REJECTED, both))
    }

    @Test fun `a verified or already-submitted account is never changed by an upload`() {
        assertFalse(shouldMarkSubmitted(VerificationStatus.VERIFIED, both))
        assertFalse(shouldMarkSubmitted(VerificationStatus.SUBMITTED, both))
    }

    @Test fun `optional documents do not count toward the required total`() {
        val profile = LandlordOnboardingProfile(
            documents = mapOf(
                LandlordDocumentKind.PROOF_OF_ADDRESS to "u/addr.pdf",
                LandlordDocumentKind.ID_DOCUMENT to "u/id.pdf"
            )
        )
        assertEquals(1, profile.requiredDocumentsUploaded())
        assertEquals(2, REQUIRED_DOCUMENT_COUNT)
    }

    // ---- catalogs ----

    @Test fun `location catalog mirrors the nine web provinces`() {
        assertEquals(9, LocationCatalog.PROVINCES.size)
        assertTrue("Gauteng" in LocationCatalog.PROVINCES)
        assertTrue("Braamfontein" in LocationCatalog.citiesFor("Gauteng"))
        assertTrue(LocationCatalog.citiesFor(null).isEmpty())
        assertTrue(LocationCatalog.citiesFor("Atlantis").isEmpty())
    }

    @Test fun `room types match the web list`() =
        assertEquals(listOf("Single room", "Shared room", "En-suite", "Bachelor flat", "Any"), RoomTypeCatalog.ALL)

    // ---- dashboard total paid ----

    private fun payment(status: PaymentStatus, amount: Double) = Payment(
        id = "p", reference = "r", propertyTitle = "t", kind = PaymentKind.DEPOSIT, status = status,
        amount = amount, dueDate = null, paidAt = null, createdAt = "2026-10-01"
    )

    @Test fun `total paid sums only PAID payments`() {
        val total = listOf(
            payment(PaymentStatus.PAID, 3000.0), payment(PaymentStatus.PENDING, 500.0),
            payment(PaymentStatus.PAID, 1200.5), payment(PaymentStatus.REFUNDED, 900.0)
        ).totalPaid()
        assertEquals(4200.5, total, 0.001)
        assertEquals(0.0, emptyList<Payment>().totalPaid(), 0.0)
    }
}
