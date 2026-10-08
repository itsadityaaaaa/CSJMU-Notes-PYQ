package com.example.domain.repository

import com.example.domain.model.ApprovalStatus
import com.example.domain.model.ResourceItem
import com.example.domain.model.Submission
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for notes upload and moderation workflow.
 * Handles student submissions, moderation approval, and publication.
 */
interface ISubmissionRepository {
    fun getAllSubmissions(): Flow<List<Submission>>

    fun getPendingSubmissions(): Flow<List<Submission>>

    fun getSubmissionsByStatus(status: ApprovalStatus): Flow<List<Submission>>

    fun getSubmissionsByUploader(contributorName: String): Flow<List<Submission>>

    suspend fun submitForReview(submission: Submission): Result<String>

    suspend fun approveAndPublish(
        submission: Submission,
        adminNotes: String?
    ): Result<ResourceItem>

    suspend fun updateSubmission(submission: Submission): Result<Unit>

    suspend fun rejectSubmission(submissionId: String, rejectionReason: String): Result<Unit>

    fun getSubmissionById(id: String): Flow<Submission?>

    suspend fun syncFromRemote(): Result<Unit>
}
