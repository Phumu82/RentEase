package com.rentease.app.presentation.landlord.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rentease.app.R
import com.rentease.app.domain.model.VerificationBadge
import com.rentease.app.domain.model.VerificationProgress
import com.rentease.app.domain.model.VerificationStep

@StringRes
private fun VerificationStep.labelRes(): Int = when (this) {
    VerificationStep.ACCOUNT_CREATED -> R.string.progress_step_account
    VerificationStep.DOCUMENTS_SUBMITTED -> R.string.progress_step_documents
    VerificationStep.VERIFIED_BY_ADMIN -> R.string.progress_step_verified
    VerificationStep.FIRST_PROPERTY_ADDED -> R.string.progress_step_first_property
    VerificationStep.LISTING_PUBLISHED -> R.string.progress_step_published
}

@StringRes
private fun VerificationBadge.labelRes(): Int = when (this) {
    VerificationBadge.NEWCOMER -> R.string.progress_badge_newcomer
    VerificationBadge.DOCUMENTS_IN -> R.string.progress_badge_documents_in
    VerificationBadge.VERIFIED_HOST -> R.string.progress_badge_verified_host
    VerificationBadge.LISTING_CREATOR -> R.string.progress_badge_listing_creator
    VerificationBadge.FULLY_LIVE -> R.string.progress_badge_fully_live
}

/** Gamified verification progress: progress bar, earned badge, milestone checklist, next step. */
@Composable
fun VerificationProgressCard(progress: VerificationProgress, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.progress_badge_label, stringResource(progress.badge.labelRes())),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.progress_steps_complete, progress.completedCount, progress.totalSteps),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            VerificationStep.entries.forEach { step ->
                val done = progress.isCompleted(step)
                Text(
                    text = (if (done) "✓  " else "○  ") + stringResource(step.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (done) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            val next = progress.nextStep
            Text(
                text = if (next != null) stringResource(R.string.progress_next_step, stringResource(next.labelRes()))
                else stringResource(R.string.progress_all_done),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
