package com.example.data.repository

import com.example.data.local.ResourceDao
import com.example.data.local.SubmissionDao
import com.example.data.model.ResourceEntity
import com.example.data.model.SubmissionEntity
import com.example.data.remote.IRemoteResourceDataSource
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.ResourceItem
import com.example.domain.model.Submission
import com.example.domain.repository.ISubmissionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SubmissionRepositoryImpl(
    private val submissionDao: SubmissionDao,
    private val resourceDao: ResourceDao,
    private val remoteDataSource: IRemoteResourceDataSource,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ISubmissionRepository {

    override fun getAllSubmissions(): Flow<List<Submission>> {
        scope.launch {
            val remoteResult = remoteDataSource.fetchAllRemoteSubmissions()
            remoteResult.getOrNull()?.forEach { sub ->
                submissionDao.insertSubmission(SubmissionEntity.fromDomain(sub))
            }
        }

        return submissionDao.getAllSubmissions().map { list -> list.map { it.toDomain() } }
    }

    override fun getPendingSubmissions(): Flow<List<Submission>> {
        scope.launch {
            val remoteResult = remoteDataSource.fetchRemotePendingSubmissions()
            remoteResult.getOrNull()?.forEach { sub ->
                submissionDao.insertSubmission(SubmissionEntity.fromDomain(sub))
            }
        }

        return submissionDao.getPendingSubmissions().map { list -> list.map { it.toDomain() } }
    }

    override fun getSubmissionsByStatus(status: ApprovalStatus): Flow<List<Submission>> {
        return submissionDao.getSubmissionsByStatus(status).map { list -> list.map { it.toDomain() } }
    }

    override fun getSubmissionsByUploader(contributorName: String): Flow<List<Submission>> {
        return submissionDao.getSubmissionsByUploader(contributorName).map { list -> list.map { it.toDomain() } }
    }

    override fun getSubmissionById(id: String): Flow<Submission?> {
        return submissionDao.getSubmissionById(id).map { it?.toDomain() }
    }

    override suspend fun submitForReview(submission: Submission): Result<String> {
        return runCatching {
            val entity = SubmissionEntity.fromDomain(
                submission.copy(status = ApprovalStatus.PENDING)
            )
            submissionDao.insertSubmission(entity)
            remoteDataSource.submitNoteToRemote(submission)
            submission.id
        }
    }

    override suspend fun updateSubmission(submission: Submission): Result<Unit> {
        return runCatching {
            submissionDao.updateSubmission(SubmissionEntity.fromDomain(submission))
            remoteDataSource.submitNoteToRemote(submission)

            // If this submission was already approved, update the published Resource as well
            if (submission.status == ApprovalStatus.APPROVED) {
                val now = submission.reviewedTimestamp ?: System.currentTimeMillis()
                val resourceItem = ResourceItem(
                    id = "res_${submission.id}",
                    title = submission.title,
                    description = submission.description,
                    collegeId = submission.collegeId,
                    courseId = submission.courseId,
                    academicYear = submission.academicYear,
                    semester = submission.semester,
                    subjectId = submission.subjectId,
                    subjectName = submission.subjectName,
                    resourceType = submission.resourceType,
                    examYear = submission.examYear,
                    fileUri = submission.fileUri,
                    fileName = submission.fileName,
                    fileSizeFormatted = submission.fileSizeFormatted,
                    fileMimeType = submission.fileMimeType,
                    contributorName = submission.contributorName,
                    contributorEmail = submission.contributorEmail,
                    isApproved = true,
                    publishedTimestamp = now,
                    downloadCount = 0,
                    tags = listOf(
                        submission.subjectName,
                        submission.courseName,
                        "Sem ${submission.semester}"
                    )
                )
                resourceDao.insertOrUpdate(ResourceEntity.fromDomain(resourceItem))
                remoteDataSource.publishResourceToRemote(resourceItem)
            }
        }
    }

    override suspend fun approveAndPublish(
        submission: Submission,
        adminNotes: String?
    ): Result<ResourceItem> {
        return runCatching {
            val now = System.currentTimeMillis()
            val finalSubmission = submission.copy(
                status = ApprovalStatus.APPROVED,
                adminFeedback = adminNotes ?: "Approved by CSJMU Academic Admin Panel",
                reviewedTimestamp = now
            )

            // Update in local database
            submissionDao.updateSubmission(SubmissionEntity.fromDomain(finalSubmission))

            // Convert to published ResourceItem with any curated admin edits
            val resourceItem = ResourceItem(
                id = "res_${finalSubmission.id}",
                title = finalSubmission.title,
                description = finalSubmission.description,
                collegeId = finalSubmission.collegeId,
                courseId = finalSubmission.courseId,
                academicYear = finalSubmission.academicYear,
                semester = finalSubmission.semester,
                subjectId = finalSubmission.subjectId,
                subjectName = finalSubmission.subjectName,
                resourceType = finalSubmission.resourceType,
                examYear = finalSubmission.examYear,
                fileUri = finalSubmission.fileUri,
                fileName = finalSubmission.fileName,
                fileSizeFormatted = finalSubmission.fileSizeFormatted,
                fileMimeType = finalSubmission.fileMimeType,
                contributorName = finalSubmission.contributorName,
                contributorEmail = finalSubmission.contributorEmail,
                isApproved = true,
                publishedTimestamp = now,
                downloadCount = 0,
                tags = listOf(
                    finalSubmission.subjectName,
                    finalSubmission.courseName,
                    "Sem ${finalSubmission.semester}"
                )
            )

            resourceDao.insertOrUpdate(ResourceEntity.fromDomain(resourceItem))

            // Sync approved status & publish to Firestore
            remoteDataSource.updateRemoteSubmissionStatus(
                submissionId = finalSubmission.id,
                status = ApprovalStatus.APPROVED,
                adminNotes = adminNotes
            )
            remoteDataSource.publishResourceToRemote(resourceItem)

            resourceItem
        }
    }

    override suspend fun rejectSubmission(
        submissionId: String,
        rejectionReason: String
    ): Result<Unit> {
        return runCatching {
            val entity = submissionDao.findSubmissionById(submissionId)
            if (entity != null) {
                val updatedEntity = entity.copy(
                    status = ApprovalStatus.REJECTED,
                    adminFeedback = rejectionReason,
                    reviewedTimestamp = System.currentTimeMillis()
                )
                submissionDao.updateSubmission(updatedEntity)
            }

            remoteDataSource.updateRemoteSubmissionStatus(
                submissionId = submissionId,
                status = ApprovalStatus.REJECTED,
                adminNotes = rejectionReason
            )
        }
    }
}
