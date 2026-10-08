package com.example.data.repository

import android.content.Context
import android.util.Log
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val context: Context? = null
) : ISubmissionRepository {

    companion object {
        private const val TAG = "SubmissionRepo"
    }

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
            var updatedSubmission = submission.copy(status = ApprovalStatus.PENDING)

            // If a local content URI is attached, upload directly to Google Drive via Google Apps Script
            val localUri = submission.fileUri
            if (context != null && !localUri.isNullOrBlank() && (localUri.startsWith("content://") || localUri.startsWith("file://"))) {
                try {
                    val driveResult = remoteDataSource.uploadDocumentToGoogleDrive(
                        context = context,
                        fileUriString = localUri,
                        fileName = submission.fileName ?: "Academic_Notes.pdf",
                        mimeType = submission.fileMimeType ?: "application/pdf",
                        submission = updatedSubmission
                    )

                    if (driveResult.isSuccess) {
                        val upload = driveResult.getOrThrow()
                        updatedSubmission = updatedSubmission.copy(
                            driveFileId = upload.driveFileId,
                            driveFileUrl = upload.driveFileUrl,
                            fileSizeBytes = upload.fileSizeBytes,
                            fileUri = upload.driveFileUrl ?: localUri
                        )
                        Log.i(TAG, "Uploaded to Google Drive: fileId=${upload.driveFileId}")
                    } else {
                        Log.w(TAG, "Drive upload warning: ${driveResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed uploading file to Drive", e)
                }
            }

            // Save in local SQLite Room database
            val entity = SubmissionEntity.fromDomain(updatedSubmission)
            submissionDao.insertSubmission(entity)

            // Sync to Firestore & cloud backend
            val remoteResult = remoteDataSource.submitNoteToRemote(updatedSubmission)
            if (remoteResult.isFailure) {
                Log.w(TAG, "Cloud sync warning: ${remoteResult.exceptionOrNull()?.message}")
            }

            updatedSubmission.id
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
                    driveFileId = submission.driveFileId,
                    driveFileUrl = submission.driveFileUrl,
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

            // 1. Move file from Google Drive Pending to Approved
            finalSubmission.driveFileId?.let { driveId ->
                if (driveId.isNotBlank()) {
                    remoteDataSource.approveDocumentInDrive(driveId)
                }
            }

            // 2. Update local SQLite DB
            submissionDao.updateSubmission(SubmissionEntity.fromDomain(finalSubmission))

            // 3. Convert to published ResourceItem
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
                driveFileId = finalSubmission.driveFileId,
                driveFileUrl = finalSubmission.driveFileUrl,
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

            // 4. Sync approved status & publish to Firestore & Google Drive metadata
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
            val driveId = entity?.driveFileId

            // 1. Move file to trash in Google Drive
            if (!driveId.isNullOrBlank()) {
                remoteDataSource.rejectDocumentInDrive(driveId)
            }

            // 2. Update local database
            if (entity != null) {
                val updatedEntity = entity.copy(
                    status = ApprovalStatus.REJECTED,
                    adminFeedback = rejectionReason,
                    reviewedTimestamp = System.currentTimeMillis()
                )
                submissionDao.updateSubmission(updatedEntity)
            }

            // 3. Sync rejection status to Firestore & cloud
            remoteDataSource.updateRemoteSubmissionStatus(
                submissionId = submissionId,
                status = ApprovalStatus.REJECTED,
                adminNotes = rejectionReason
            )
        }
    }

    override suspend fun syncFromRemote(): Result<Unit> {
        return runCatching {
            val remoteResult = remoteDataSource.fetchAllRemoteSubmissions()
            remoteResult.getOrNull()?.forEach { sub ->
                submissionDao.insertSubmission(SubmissionEntity.fromDomain(sub))
            }
            Unit
        }
    }
}
