package com.rentease.app.data.repository

import com.rentease.app.data.remote.httpHint
import android.content.Context
import com.rentease.app.R
import com.rentease.app.data.auth.TokenManager
import com.rentease.app.data.remote.api.SupabaseSupportApi
import com.rentease.app.data.remote.dto.ComplaintCreateDto
import com.rentease.app.domain.model.Complaint
import com.rentease.app.domain.model.ComplaintInput
import com.rentease.app.domain.model.MaintenanceStatus
import com.rentease.app.domain.repository.SupportRepository
import com.rentease.app.domain.repository.SupportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepositoryImpl @Inject constructor(
    private val api: SupabaseSupportApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : SupportRepository {

    override suspend fun getMyComplaints(): SupportResult<List<Complaint>> {
        val userId = tokenManager.getUserId()
            ?: return SupportResult.Failure(context.getString(R.string.error_not_signed_in))

        val response = api.getMyComplaints(reporterIdFilter = "eq.$userId")
        if (!response.isSuccessful) {
            return SupportResult.Failure("${context.getString(R.string.error_could_not_load_my_complaints)}${context.httpHint(response.code())}")
        }

        return SupportResult.Success(
            response.body().orEmpty().map { dto ->
                Complaint(
                    id = dto.id,
                    subject = dto.subject,
                    category = dto.category,
                    description = dto.description,
                    status = MaintenanceStatus.fromDb(dto.status),
                    adminResponse = dto.adminResponse,
                    createdAt = dto.createdAt
                )
            }
        )
    }

    override suspend fun submitComplaint(input: ComplaintInput): SupportResult<Unit> {
        val userId = tokenManager.getUserId()
            ?: return SupportResult.Failure(context.getString(R.string.error_not_signed_in))

        val response = api.createComplaint(
            ComplaintCreateDto(
                reporterId = userId,
                subject = input.subject,
                category = input.category,
                description = input.description
            )
        )

        return if (response.isSuccessful) {
            SupportResult.Success(Unit)
        } else {
            SupportResult.Failure("${context.getString(R.string.error_complaint_submit_failed)}${context.httpHint(response.code())}")
        }
    }
}
