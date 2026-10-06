package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.ResourceItem
import com.example.domain.model.ResourceType
import com.example.domain.model.Submission
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Scalable Remote Data Source backed by Firebase Firestore.
 * Connects directly to CSJMU cloud collections:
 * - 'resources': Public verified study materials
 * - 'submissions': Student contributions waiting for admin moderation
 */
interface IRemoteResourceDataSource {

    suspend fun fetchRemoteResources(
        collegeId: String,
        courseId: String,
        academicYear: Int,
        semester: Int?
    ): Result<List<ResourceItem>>

    suspend fun publishResourceToRemote(resource: ResourceItem): Result<Unit>

    suspend fun uploadDocumentFile(
        fileUriString: String,
        fileName: String,
        mimeType: String
    ): Result<String>

    suspend fun submitNoteToRemote(submission: Submission): Result<String>

    suspend fun fetchRemotePendingSubmissions(): Result<List<Submission>>

    suspend fun fetchAllRemoteSubmissions(): Result<List<Submission>>

    suspend fun updateRemoteSubmissionStatus(
        submissionId: String,
        status: ApprovalStatus,
        adminNotes: String?
    ): Result<Unit>
}

class RemoteResourceDataSourceImpl(
    private val firestore: FirebaseFirestore?
) : IRemoteResourceDataSource {

    companion object {
        private const val TAG = "FirestoreDataSource"
        private const val COLLECTION_RESOURCES = "resources"
        private const val COLLECTION_SUBMISSIONS = "submissions"

        fun create(context: Context): RemoteResourceDataSourceImpl {
            val dbId = try {
                context.getString(R.string.firestore_database_id)
            } catch (e: Exception) {
                null
            }

            val db = if (!dbId.isNullOrBlank()) {
                try {
                    FirebaseFirestore.getInstance(dbId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing named Firestore database: $dbId", e)
                    null
                }
            } else {
                null
            }

            return RemoteResourceDataSourceImpl(db)
        }
    }

    override suspend fun fetchRemoteResources(
        collegeId: String,
        courseId: String,
        academicYear: Int,
        semester: Int?
    ): Result<List<ResourceItem>> = runCatching {
        val db = firestore ?: return@runCatching emptyList()

        var query: Query = db.collection(COLLECTION_RESOURCES)
            .whereEqualTo("collegeId", collegeId)
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("academicYear", academicYear)

        if (semester != null) {
            query = query.whereEqualTo("semester", semester)
        }

        val snapshot = query.get().await()
        snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                ResourceItem(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    collegeId = data["collegeId"] as? String ?: collegeId,
                    courseId = data["courseId"] as? String ?: courseId,
                    academicYear = (data["academicYear"] as? Number)?.toInt() ?: academicYear,
                    semester = (data["semester"] as? Number)?.toInt() ?: 1,
                    subjectId = data["subjectId"] as? String ?: "",
                    subjectName = data["subjectName"] as? String ?: "",
                    resourceType = runCatching {
                        ResourceType.valueOf(data["resourceType"] as? String ?: "NOTES")
                    }.getOrDefault(ResourceType.NOTES),
                    examYear = data["examYear"] as? String,
                    fileUri = data["fileUri"] as? String,
                    fileName = data["fileName"] as? String,
                    fileSizeFormatted = data["fileSizeFormatted"] as? String,
                    fileMimeType = data["fileMimeType"] as? String,
                    contributorName = data["contributorName"] as? String ?: "Student",
                    contributorEmail = data["contributorEmail"] as? String,
                    isApproved = data["isApproved"] as? Boolean ?: true,
                    publishedTimestamp = (data["publishedTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    downloadCount = (data["downloadCount"] as? Number)?.toInt() ?: 0,
                    tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error mapping resource doc: ${doc.id}", e)
                null
            }
        }
    }

    override suspend fun publishResourceToRemote(resource: ResourceItem): Result<Unit> = runCatching {
        val db = firestore ?: return@runCatching Unit
        val payload = mapOf(
            "title" to resource.title,
            "description" to resource.description,
            "collegeId" to resource.collegeId,
            "courseId" to resource.courseId,
            "academicYear" to resource.academicYear,
            "semester" to resource.semester,
            "subjectId" to resource.subjectId,
            "subjectName" to resource.subjectName,
            "resourceType" to resource.resourceType.name,
            "examYear" to (resource.examYear ?: ""),
            "fileUri" to (resource.fileUri ?: ""),
            "fileName" to (resource.fileName ?: ""),
            "fileSizeFormatted" to (resource.fileSizeFormatted ?: ""),
            "fileMimeType" to (resource.fileMimeType ?: ""),
            "contributorName" to resource.contributorName,
            "contributorEmail" to (resource.contributorEmail ?: ""),
            "isApproved" to true,
            "publishedTimestamp" to resource.publishedTimestamp,
            "downloadCount" to resource.downloadCount,
            "tags" to resource.tags,
            "userId" to (Firebase.auth.currentUser?.uid ?: "csjmu_student")
        )

        db.collection(COLLECTION_RESOURCES)
            .document(resource.id)
            .set(payload)
            .await()
    }

    override suspend fun uploadDocumentFile(
        fileUriString: String,
        fileName: String,
        mimeType: String
    ): Result<String> {
        // Return local URI for offline-first, ready for Firebase Cloud Storage if provisioned
        return Result.success(fileUriString)
    }

    override suspend fun submitNoteToRemote(submission: Submission): Result<String> = runCatching {
        val db = firestore ?: return@runCatching submission.id
        val currentUserId = Firebase.auth.currentUser?.uid ?: "csjmu_student"

        val payload = mapOf(
            "title" to submission.title,
            "description" to submission.description,
            "collegeId" to submission.collegeId,
            "courseId" to submission.courseId,
            "courseName" to submission.courseName,
            "academicYear" to submission.academicYear,
            "semester" to submission.semester,
            "subjectId" to submission.subjectId,
            "subjectName" to submission.subjectName,
            "resourceType" to submission.resourceType.name,
            "examYear" to (submission.examYear ?: ""),
            "fileUri" to (submission.fileUri ?: ""),
            "fileName" to (submission.fileName ?: ""),
            "fileSizeFormatted" to (submission.fileSizeFormatted ?: ""),
            "fileMimeType" to (submission.fileMimeType ?: ""),
            "contributorName" to submission.contributorName,
            "contributorEmail" to (submission.contributorEmail ?: ""),
            "status" to submission.status.name,
            "adminFeedback" to (submission.adminFeedback ?: ""),
            "submittedTimestamp" to submission.submittedTimestamp,
            "reviewedTimestamp" to (submission.reviewedTimestamp ?: 0L),
            "userId" to currentUserId
        )

        db.collection(COLLECTION_SUBMISSIONS)
            .document(submission.id)
            .set(payload)
            .await()

        submission.id
    }

    override suspend fun fetchRemotePendingSubmissions(): Result<List<Submission>> = runCatching {
        val db = firestore ?: return@runCatching emptyList()
        val snapshot = db.collection(COLLECTION_SUBMISSIONS)
            .whereEqualTo("status", ApprovalStatus.PENDING.name)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc -> mapDocToSubmission(doc) }
    }

    override suspend fun fetchAllRemoteSubmissions(): Result<List<Submission>> = runCatching {
        val db = firestore ?: return@runCatching emptyList()
        val snapshot = db.collection(COLLECTION_SUBMISSIONS).get().await()
        snapshot.documents.mapNotNull { doc -> mapDocToSubmission(doc) }
    }

    override suspend fun updateRemoteSubmissionStatus(
        submissionId: String,
        status: ApprovalStatus,
        adminNotes: String?
    ): Result<Unit> = runCatching {
        val db = firestore ?: return@runCatching Unit
        val updates = mutableMapOf<String, Any>(
            "status" to status.name,
            "reviewedTimestamp" to System.currentTimeMillis()
        )
        if (adminNotes != null) {
            updates["adminFeedback"] = adminNotes
        }

        db.collection(COLLECTION_SUBMISSIONS)
            .document(submissionId)
            .update(updates)
            .await()
    }

    private fun mapDocToSubmission(doc: com.google.firebase.firestore.DocumentSnapshot): Submission? {
        val data = doc.data ?: return null
        return Submission(
            id = doc.id,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            collegeId = data["collegeId"] as? String ?: "",
            courseId = data["courseId"] as? String ?: "",
            courseName = data["courseName"] as? String ?: "",
            academicYear = (data["academicYear"] as? Number)?.toInt() ?: 1,
            semester = (data["semester"] as? Number)?.toInt() ?: 1,
            subjectId = data["subjectId"] as? String ?: "",
            subjectName = data["subjectName"] as? String ?: "",
            resourceType = runCatching {
                ResourceType.valueOf(data["resourceType"] as? String ?: "NOTES")
            }.getOrDefault(ResourceType.NOTES),
            examYear = data["examYear"] as? String,
            fileUri = data["fileUri"] as? String,
            fileName = data["fileName"] as? String,
            fileSizeFormatted = data["fileSizeFormatted"] as? String,
            fileMimeType = data["fileMimeType"] as? String,
            contributorName = data["contributorName"] as? String ?: "Student",
            contributorEmail = data["contributorEmail"] as? String,
            status = runCatching {
                ApprovalStatus.valueOf(data["status"] as? String ?: "PENDING")
            }.getOrDefault(ApprovalStatus.PENDING),
            adminFeedback = data["adminFeedback"] as? String,
            submittedTimestamp = (data["submittedTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            reviewedTimestamp = (data["reviewedTimestamp"] as? Number)?.toLong()
        )
    }
}
