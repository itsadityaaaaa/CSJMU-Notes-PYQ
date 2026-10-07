package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Base64
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class DriveUploadResult(
    val driveFileId: String?,
    val driveFileUrl: String?,
    val fileSizeBytes: Long
)

/**
 * Scalable Remote Data Source backed by Firebase Firestore and Google Drive (via Google Apps Script).
 * Free Spark-tier compliant (Zero Firebase Storage, Zero Cloud Functions).
 */
interface IRemoteResourceDataSource {

    suspend fun fetchRemoteResources(
        collegeId: String,
        courseId: String,
        academicYear: Int,
        semester: Int?
    ): Result<List<ResourceItem>>

    suspend fun publishResourceToRemote(resource: ResourceItem): Result<Unit>

    suspend fun uploadDocumentToGoogleDrive(
        context: Context,
        fileUriString: String,
        fileName: String,
        mimeType: String
    ): Result<DriveUploadResult>

    suspend fun submitNoteToRemote(submission: Submission): Result<String>

    suspend fun fetchRemotePendingSubmissions(): Result<List<Submission>>

    suspend fun fetchAllRemoteSubmissions(): Result<List<Submission>>

    suspend fun updateRemoteSubmissionStatus(
        submissionId: String,
        status: ApprovalStatus,
        adminNotes: String?
    ): Result<Unit>

    suspend fun approveDocumentInDrive(driveFileId: String?): Result<Unit>

    suspend fun rejectDocumentInDrive(driveFileId: String?): Result<Unit>
}

class RemoteResourceDataSourceImpl(
    private val firestore: FirebaseFirestore?,
    private val backendUrl: String? = null
) : IRemoteResourceDataSource {

    companion object {
        private const val TAG = "RemoteDataSource"
        private const val COLLECTION_RESOURCES = "resources"
        private const val COLLECTION_SUBMISSIONS = "submissions"
        private const val MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024 // 25 MB
        const val DEFAULT_APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzvfQQ_m3OWJBs5_LQdHu5nhnp90DeHtVvJNy_Zc1yl-YMq96Qubgp5TOevPdzMTXZ5pQ/exec"

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

            // Resolved Google Apps Script Web App URL from string resource or default constant
            val appsScriptUrl = try {
                val resId = context.resources.getIdentifier("apps_script_url", "string", context.packageName)
                if (resId != 0) context.getString(resId) else null
            } catch (e: Exception) {
                null
            } ?: DEFAULT_APPS_SCRIPT_URL

            return RemoteResourceDataSourceImpl(db, appsScriptUrl)
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

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
            .whereEqualTo("isApproved", true)

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
                    driveFileId = data["driveFileId"] as? String,
                    driveFileUrl = data["driveFileUrl"] as? String,
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
            "driveFileId" to (resource.driveFileId ?: ""),
            "driveFileUrl" to (resource.driveFileUrl ?: ""),
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

    override suspend fun uploadDocumentToGoogleDrive(
        context: Context,
        fileUriString: String,
        fileName: String,
        mimeType: String
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(fileUriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() } ?: ByteArray(0)

            if (bytes.size > MAX_FILE_SIZE_BYTES) {
                throw IllegalArgumentException("Document exceeds the 25 MB limit.")
            }

            val fileSizeBytes = bytes.size.toLong()

            // If a Google Apps Script Web App URL is available, send to Google Drive
            if (!backendUrl.isNullOrBlank()) {
                val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val jsonPayload = JSONObject().apply {
                    put("action", "uploadPending")
                    put("fileName", fileName)
                    put("mimeType", mimeType)
                    put("base64Data", base64Data)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonPayload.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(backendUrl)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val jsonResp = JSONObject(responseBody)
                    if (jsonResp.optBoolean("success", false)) {
                        val fileId = jsonResp.optString("fileId", null)
                        val fileUrl = jsonResp.optString("fileUrl", null)
                        return@runCatching DriveUploadResult(fileId, fileUrl, fileSizeBytes)
                    } else {
                        val err = jsonResp.optString("error", "Failed to upload to Google Drive")
                        Log.e(TAG, "Apps Script upload error: $err")
                    }
                }
            }

            // Fallback for offline / direct testing
            DriveUploadResult(
                driveFileId = null,
                driveFileUrl = fileUriString,
                fileSizeBytes = fileSizeBytes
            )
        }
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
            "driveFileId" to (submission.driveFileId ?: ""),
            "driveFileUrl" to (submission.driveFileUrl ?: ""),
            "fileName" to (submission.fileName ?: ""),
            "fileSizeFormatted" to (submission.fileSizeFormatted ?: ""),
            "fileMimeType" to (submission.fileMimeType ?: ""),
            "fileSizeBytes" to submission.fileSizeBytes,
            "contributorName" to submission.contributorName,
            "contributorEmail" to (submission.contributorEmail ?: ""),
            "status" to submission.status.name,
            "adminFeedback" to (submission.adminFeedback ?: ""),
            "submittedTimestamp" to submission.submittedTimestamp,
            "reviewedTimestamp" to (submission.reviewedTimestamp ?: 0L),
            "expiresAt" to submission.expiresAt,
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
        val now = System.currentTimeMillis()

        val snapshot = db.collection(COLLECTION_SUBMISSIONS)
            .whereEqualTo("status", ApprovalStatus.PENDING.name)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            val sub = mapDocToSubmission(doc) ?: return@mapNotNull null
            // Automatically exclude expired submissions (> 3 days)
            if (sub.expiresAt <= now) {
                // Expired: remove from Firestore moderation queue
                doc.reference.delete()
                null
            } else {
                sub
            }
        }
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

    override suspend fun approveDocumentInDrive(driveFileId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (driveFileId.isNullOrBlank() || backendUrl.isNullOrBlank()) return@runCatching

            val jsonPayload = JSONObject().apply {
                put("action", "approveDocument")
                put("fileId", driveFileId)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(backendUrl)
                .post(jsonPayload.toString().toRequestBody(mediaType))
                .build()

            httpClient.newCall(request).execute().close()
        }
    }

    override suspend fun rejectDocumentInDrive(driveFileId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (driveFileId.isNullOrBlank() || backendUrl.isNullOrBlank()) return@runCatching

            val jsonPayload = JSONObject().apply {
                put("action", "rejectDocument")
                put("fileId", driveFileId)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(backendUrl)
                .post(jsonPayload.toString().toRequestBody(mediaType))
                .build()

            httpClient.newCall(request).execute().close()
        }
    }

    private fun mapDocToSubmission(doc: com.google.firebase.firestore.DocumentSnapshot): Submission? {
        val data = doc.data ?: return null
        val submittedTime = (data["submittedTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val defaultExpires = submittedTime + (3 * 24 * 60 * 60 * 1000L)

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
            driveFileId = data["driveFileId"] as? String,
            driveFileUrl = data["driveFileUrl"] as? String,
            fileName = data["fileName"] as? String,
            fileSizeFormatted = data["fileSizeFormatted"] as? String,
            fileMimeType = data["fileMimeType"] as? String,
            fileSizeBytes = (data["fileSizeBytes"] as? Number)?.toLong() ?: 0L,
            contributorName = data["contributorName"] as? String ?: "Student",
            contributorEmail = data["contributorEmail"] as? String,
            status = runCatching {
                ApprovalStatus.valueOf(data["status"] as? String ?: "PENDING")
            }.getOrDefault(ApprovalStatus.PENDING),
            adminFeedback = data["adminFeedback"] as? String,
            submittedTimestamp = submittedTime,
            reviewedTimestamp = (data["reviewedTimestamp"] as? Number)?.toLong(),
            expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: defaultExpires
        )
    }
}
