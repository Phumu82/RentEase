package com.rentease.app.presentation.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rentease.app.R
import com.rentease.app.domain.model.BookingStatus
import com.rentease.app.domain.model.MaintenancePriority
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.model.PaymentKind
import com.rentease.app.domain.model.PaymentStatus
import com.rentease.app.domain.model.PropertyStatus
import com.rentease.app.domain.model.RoomStatus
import com.rentease.app.domain.model.UserRole
import com.rentease.app.domain.model.VerificationStatus

/**
 * Localized display labels for every status enum used across the app.
 * Centralized here (rather than a statusLabel() copy-pasted per screen) so
 * every screen shows the same translated word for the same status, and so
 * a translator only has to find/fix one string per status per language.
 */
@Composable
fun BookingStatus.label(): String = stringResource(
    when (this) {
        BookingStatus.PENDING -> R.string.booking_status_pending
        BookingStatus.APPROVED -> R.string.booking_status_approved
        BookingStatus.REJECTED -> R.string.booking_status_rejected
        BookingStatus.CANCELLED -> R.string.booking_status_cancelled
        BookingStatus.ACTIVE -> R.string.booking_status_active
        BookingStatus.COMPLETED -> R.string.booking_status_completed
    }
)

@Composable
fun MaintenanceStatus.label(): String = stringResource(
    when (this) {
        MaintenanceStatus.OPEN -> R.string.maintenance_status_open
        MaintenanceStatus.IN_PROGRESS -> R.string.maintenance_status_in_progress
        MaintenanceStatus.RESOLVED -> R.string.maintenance_status_resolved
        MaintenanceStatus.CLOSED -> R.string.maintenance_status_closed
    }
)

@Composable
fun RoomStatus.label(): String = stringResource(
    when (this) {
        RoomStatus.AVAILABLE -> R.string.room_status_available
        RoomStatus.RESERVED -> R.string.room_status_reserved
        RoomStatus.OCCUPIED -> R.string.room_status_occupied
        RoomStatus.MAINTENANCE -> R.string.room_status_maintenance
    }
)

@Composable
fun PropertyStatus.label(): String = stringResource(
    when (this) {
        PropertyStatus.DRAFT -> R.string.property_status_draft
        PropertyStatus.PENDING -> R.string.property_status_pending
        PropertyStatus.PUBLISHED -> R.string.property_status_published
        PropertyStatus.REJECTED -> R.string.property_status_rejected
        PropertyStatus.ARCHIVED -> R.string.property_status_archived
    }
)

@Composable
fun VerificationStatus.label(): String = stringResource(
    when (this) {
        VerificationStatus.PENDING -> R.string.verification_status_pending
        VerificationStatus.SUBMITTED -> R.string.verification_status_submitted
        VerificationStatus.VERIFIED -> R.string.verification_status_verified
        VerificationStatus.REJECTED -> R.string.verification_status_rejected
        VerificationStatus.DOCUMENTS_REQUIRED -> R.string.verification_status_documents_required
    }
)

@Composable
fun MaintenancePriority.label(): String = stringResource(
    when (this) {
        MaintenancePriority.LOW -> R.string.priority_low
        MaintenancePriority.MEDIUM -> R.string.priority_medium
        MaintenancePriority.HIGH -> R.string.priority_high
        MaintenancePriority.URGENT -> R.string.priority_urgent
    }
)

@Composable
fun PaymentKind.label(): String = stringResource(
    when (this) {
        PaymentKind.DEPOSIT -> R.string.payment_kind_deposit
        PaymentKind.RENT -> R.string.payment_kind_rent
        PaymentKind.FEE -> R.string.payment_kind_fee
    }
)

@Composable
fun PaymentStatus.label(): String = stringResource(
    when (this) {
        PaymentStatus.PENDING -> R.string.payment_status_pending
        PaymentStatus.PAID -> R.string.payment_status_paid
        PaymentStatus.FAILED -> R.string.payment_status_failed
        PaymentStatus.REFUNDED -> R.string.payment_status_refunded
    }
)

@Composable
fun UserRole.label(): String = stringResource(
    when (this) {
        UserRole.STUDENT -> R.string.role_label_student
        UserRole.LANDLORD -> R.string.role_label_landlord
        UserRole.ADMIN -> R.string.role_label_admin
    }
)
