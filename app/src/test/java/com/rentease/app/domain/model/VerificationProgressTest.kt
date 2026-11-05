package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationProgressTest {

    private fun profile(status: VerificationStatus) = LandlordProfile(
        userId = "l1", legalName = "L", businessName = "B", verificationStatus = status, onboardingComplete = true
    )

    private fun property(status: PropertyStatus) = LandlordProperty(
        id = "p", slug = "p", title = "P", description = "", address = "", city = "", province = "",
        universityId = null, distanceKm = 0.0, pricePerMonth = 1.0, deposit = 1.0, bedrooms = 1, bathrooms = 1, roomsAvailable = 1,
        furnished = false, amenities = emptyList(), coverImageUrl = null, images = emptyList(),
        status = status, verified = false, rating = 0.0, reviewCount = 0
    )

    @Test
    fun `a brand-new landlord awaiting documents has only the account step and the Newcomer badge`() {
        val p = VerificationProgressCalculator.compute(profile(VerificationStatus.PENDING), emptyList())
        assertEquals(1, p.completedCount)
        assertEquals(VerificationBadge.NEWCOMER, p.badge)
        assertEquals(VerificationStep.DOCUMENTS_SUBMITTED, p.nextStep)
    }

    @Test
    fun `submitting documents completes step two`() {
        val p = VerificationProgressCalculator.compute(profile(VerificationStatus.SUBMITTED), emptyList())
        assertEquals(2, p.completedCount)
        assertEquals(VerificationBadge.DOCUMENTS_IN, p.badge)
        assertEquals(VerificationStep.VERIFIED_BY_ADMIN, p.nextStep)
    }

    @Test
    fun `admin verification completes step three and earns Verified Host`() {
        val p = VerificationProgressCalculator.compute(profile(VerificationStatus.VERIFIED), emptyList())
        assertEquals(3, p.completedCount)
        assertEquals(VerificationBadge.VERIFIED_HOST, p.badge)
        assertEquals(VerificationStep.FIRST_PROPERTY_ADDED, p.nextStep)
    }

    @Test
    fun `a draft property completes step four but is not yet published`() {
        val p = VerificationProgressCalculator.compute(profile(VerificationStatus.VERIFIED), listOf(property(PropertyStatus.DRAFT)))
        assertEquals(4, p.completedCount)
        assertEquals(VerificationBadge.LISTING_CREATOR, p.badge)
        assertFalse(p.isCompleted(VerificationStep.LISTING_PUBLISHED))
        assertEquals(VerificationStep.LISTING_PUBLISHED, p.nextStep)
    }

    @Test
    fun `a published listing completes everything`() {
        val p = VerificationProgressCalculator.compute(profile(VerificationStatus.VERIFIED), listOf(property(PropertyStatus.PUBLISHED)))
        assertTrue(p.isComplete)
        assertEquals(1f, p.fraction, 0.0001f)
        assertEquals(VerificationBadge.FULLY_LIVE, p.badge)
        assertNull(p.nextStep)
    }

    @Test
    fun `a rejected landlord has submitted documents but is not verified`() {
        val p = VerificationProgressCalculator.compute(profile(VerificationStatus.REJECTED), emptyList())
        assertTrue(p.isCompleted(VerificationStep.DOCUMENTS_SUBMITTED))
        assertFalse(p.isCompleted(VerificationStep.VERIFIED_BY_ADMIN))
    }

    @Test
    fun `fraction is completed over total`() {
        val p = VerificationProgressCalculator.compute(profile(VerificationStatus.SUBMITTED), emptyList())
        assertEquals(2f / 5f, p.fraction, 0.0001f)
    }
}
