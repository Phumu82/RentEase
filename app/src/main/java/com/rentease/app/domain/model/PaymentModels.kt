package com.rentease.app.domain.model

/**
 * RentEase's "payments" are a self-reported ledger, not a card/EFT gateway
 * integration: a deposit row is auto-created the moment a student submits a
 * booking (mirrors `booking-dialog.tsx`'s two-insert flow exactly — see
 * [com.rentease.app.data.repository.ListingsRepositoryImpl.createBookingRequest]),
 * and the student later marks it "paid" themselves once they've actually
 * transferred the money by other means (bank EFT, in person, etc.). There is
 * no payment processor anywhere in this flow, on either platform.
 */
enum class PaymentKind {
    DEPOSIT, RENT, FEE;
    companion object {
        fun fromDb(value: String): PaymentKind = valueOf(value.uppercase())
    }
    fun toDb(): String = name.lowercase()
}

enum class PaymentStatus {
    PENDING, PAID, FAILED, REFUNDED;
    companion object {
        fun fromDb(value: String): PaymentStatus = valueOf(value.uppercase())
    }
    fun toDb(): String = name.lowercase()
}

data class Payment(
    val id: String,
    val reference: String,
    val propertyTitle: String,
    val kind: PaymentKind,
    val status: PaymentStatus,
    val amount: Double,
    val dueDate: String?,
    val paidAt: String?,
    val createdAt: String
)

/** Sum of payments whose status is PAID — the dashboard's "Total paid" figure (web `student.index`). */
fun List<Payment>.totalPaid(): Double = filter { it.status == PaymentStatus.PAID }.sumOf { it.amount }
