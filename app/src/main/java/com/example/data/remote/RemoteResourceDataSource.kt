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
import com.google.firebase.FirebaseApp
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
import org.json.JSONArray
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
        mimeType: String,
        submission: Submission? = null
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

            // Initialize Firestore with fallback to default instance
            val db: FirebaseFirestore? = runCatching {
                if (!dbId.isNullOrBlank()) {
                    try {
                        FirebaseFirestore.getInstance(dbId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Named Firestore DB ($dbId) init failed; using default instance", e)
                        FirebaseFirestore.getInstance()
                    }
                } else {
                    FirebaseFirestore.getInstance()
                }
            }.getOrElse { e ->
                Log.e(TAG, "FirebaseFirestore initialization error", e)
                null
            }

            // Resolved Google Apps Script Web App URL from strings.xml or default constant
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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun fetchRemoteResources(
        collegeId: String,
        courseId: String,
        academicYear: Int,
        semester: Int?
    ): Result<List<ResourceItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val itemsMap = mutableMapOf<String, ResourceItem>()

            // 1. Fetch from Firestore if available
            if (firestore != null) {
                try {
                    var query: Query = firestore.collection(COLLECTION_RESOURCES)
                        .whereEqualTo("collegeId", collegeId)
                        .whereEqualTo("courseId", courseId)
                        .whereEqualTo("academicYear", academicYear)
                        .whereEqualTo("isApproved", true)

                    if (semester != null) {
                        query = query.whereEqualTo("semester", semester)
                    }

                    val snapshot = query.get().await()
                    for (doc in snapshot.documents) {
                        mapDocToResource(doc, collegeId, courseId, academicYear)?.let {
                            itemsMap[it.id] = it
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore query for resources failed, falling back to Apps Script: ${e.message}")
                }
            }

            // 2. Fetch approved resources from Google Apps Script backend
            if (!backendUrl.isNullOrBlank()) {
                try {
                    val requestUrl = "$backendUrl?action=getApproved"
                    val request = Request.Builder().url(requestUrl).get().build()
                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful && responseBody.isNotBlank()) {
                        val json = JSONObject(responseBody)
                        if (json.optBoolean("success", false)) {
                            val array = json.optJSONArray("resources") ?: JSONArray()
                            for (i in 0 until array.length()) {
                                val itemObj = array.optJSONObject(i) ?: continue
                                val resItem = mapJsonToResource(itemObj)
                                if (resItem != null) {
                                    val matchesCollege = resItem.collegeId.isBlank() || resItem.collegeId == collegeId
                                    val matchesCourse = resItem.courseId.isBlank() || resItem.courseId == courseId
                                    val matchesYear = resItem.academicYear == 0 || resItem.academicYear == academicYear
                                    val matchesSem = semester == null || resItem.semester == 0 || resItem.semester == semester

                                    if (matchesCollege && matchesCourse && matchesYear && matchesSem) {
                                        itemsMap.putIfAbsent(resItem.id, resItem)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Apps Script fetchApproved failed: ${e.message}")
                }
            }

            itemsMap.values.toList()
        }
    }

    override suspend fun publishResourceToRemote(resource: ResourceItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val db = firestore
            val currentUserId = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "csjmu_student"

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
                "userId" to currentUserId
            )

            if (db != null) {
                try {
                    db.collection(COLLECTION_RESOURCES)
                        .document(resource.id)
                        .set(payload)
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error publishing resource to Firestore", e)
                }
            }

            Unit
        }
    }

    override suspend fun uploadDocumentToGoogleDrive(
        context: Context,
        fileUriString: String,
        fileName: String,
        mimeType: String,
        submission: Submission?
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

                    if (submission != null) {
                        put("submissionId", submission.id)
                        put("title", submission.title)
                        put("description", submission.description)
                        put("collegeId", submission.collegeId)
                        put("courseId", submission.courseId)
                        put("courseName", submission.courseName)
                        put("academicYear", submission.academicYear)
                        put("semester", submission.semester)
                        put("subjectId", submission.subjectId)
                        put("subjectName", submission.subjectName)
                        put("resourceType", submission.resourceType.name)
                        put("examYear", submission.examYear ?: "")
                        put("contributorName", submission.contributorName)
                        put("contributorEmail", submission.contributorEmail ?: "")
                        put("fileSizeFormatted", submission.fileSizeFormatted)
                    }
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonPayload.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(backendUrl)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val jsonResp = JSONObject(responseBody)
                    if (jsonResp.optBoolean("success", false)) {
                        val fileId = jsonResp.optString("fileId", null)
                        val fileUrl = jsonResp.optString("fileUrl", null)
                            ?: jsonResp.optString("downloadUrl", null)
                        Log.i(TAG, "Uploaded to Google Drive successfully. ID: $fileId")
                        return@runCatching DriveUploadResult(fileId, fileUrl, fileSizeBytes)
                    } else {
                        val err = jsonResp.optString("error", "Failed to upload to Google Drive")
                        Log.e(TAG, "Apps Script upload error: $err")
                    }
                } else {
                    Log.w(TAG, "Apps Script upload HTTP ${response.code}: $responseBody")
                }
            }

            // Fallback: local file uri retained
            DriveUploadResult(
                driveFileId = null,
                driveFileUrl = fileUriString,
                fileSizeBytes = fileSizeBytes
            )
        }
    }

    override suspend fun submitNoteToRemote(submission: Submission): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val db = firestore
            val currentUserId = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "csjmu_student"

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

            if (db != null) {
                try {
                    db.collection(COLLECTION_SUBMISSIONS)
                        .document(submission.id)
                        .set(payload)
                        .await()
                    Log.i(TAG, "Submission written to Firestore: ${submission.id}")
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore write submission error: ${e.message}")
                }
            }

            submission.id
        }
    }

    override suspend fun fetchRemotePendingSubmissions(): Result<List<Submission>> = withContext(Dispatchers.IO) {
        runCatching {
            val itemsMap = mutableMapOf<String, Submission>()
            val now = System.currentTimeMillis()

            // 1. Fetch from Firestore
            if (firestore != null) {
                try {
                    val snapshot = firestore.collection(COLLECTION_SUBMISSIONS)
                        .whereEqualTo("status", ApprovalStatus.PENDING.name)
                        .get()
                        .await()

                    for (doc in snapshot.documents) {
                        val sub = mapDocToSubmission(doc) ?: continue
                        if (sub.expiresAt <= now) {
                            runCatching { doc.reference.delete() }
                        } else {
                            itemsMap[sub.id] = sub
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore pending submissions query failed: ${e.message}")
                }
            }

            // 2. Fetch from Google Apps Script Web App
            if (!backendUrl.isNullOrBlank()) {
                try {
                    val requestUrl = "$backendUrl?action=getPending"
                    val request = Request.Builder().url(requestUrl).get().build()
                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful && responseBody.isNotBlank()) {
                        val json = JSONObject(responseBody)
                        if (json.optBoolean("success", false)) {
                            val array = json.optJSONArray("submissions") ?: JSONArray()
                            for (i in 0 until array.length()) {
                                val itemObj = array.optJSONObject(i) ?: continue
                                val sub = mapJsonToSubmission(itemObj)
                                if (sub != null && sub.status == ApprovalStatus.PENDING && sub.expiresAt > now) {
                                    itemsMap.putIfAbsent(sub.id, sub)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Apps Script fetchPending failed: ${e.message}")
                }
            }

            itemsMap.values.toList()
        }
    }

    override suspend fun fetchAllRemoteSubmissions(): Result<List<Submission>> = withContext(Dispatchers.IO) {
        runCatching {
            val itemsMap = mutableMapOf<String, Submission>()

            // 1. Fetch from Firestore
            if (firestore != null) {
                try {
                    val snapshot = firestore.collection(COLLECTION_SUBMISSIONS).get().await()
                    for (doc in snapshot.documents) {
                        mapDocToSubmission(doc)?.let { itemsMap[it.id] = it }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore all submissions query failed: ${e.message}")
                }
            }

            // 2. Fetch from Google Apps Script backend
            if (!backendUrl.isNullOrBlank()) {
                try {
                    val requestUrl = "$backendUrl?action=getAll"
                    val request = Request.Builder().url(requestUrl).get().build()
                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful && responseBody.isNotBlank()) {
                        val json = JSONObject(responseBody)
                        if (json.optBoolean("success", false)) {
                            val array = json.optJSONArray("submissions") ?: JSONArray()
                            for (i in 0 until array.length()) {
                                val itemObj = array.optJSONObject(i) ?: continue
                                mapJsonToSubmission(itemObj)?.let { itemsMap.putIfAbsent(it.id, it) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Apps Script fetchAll failed: ${e.message}")
                }
            }

            itemsMap.values.toList()
        }
    }

    override suspend fun updateRemoteSubmissionStatus(
        submissionId: String,
        status: ApprovalStatus,
        adminNotes: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "reviewedTimestamp" to System.currentTimeMillis()
            )
            if (adminNotes != null) {
                updates["adminFeedback"] = adminNotes
            }

            if (firestore != null) {
                try {
                    firestore.collection(COLLECTION_SUBMISSIONS)
                        .document(submissionId)
                        .update(updates)
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore update submission status failed: ${e.message}")
                }
            }

            // Also notify Google Apps Script backend
            if (!backendUrl.isNullOrBlank()) {
                try {
                    val actionName = if (status == ApprovalStatus.APPROVED) "approveDocument" else "rejectDocument"
                    val jsonPayload = JSONObject().apply {
                        put("action", actionName)
                        put("submissionId", submissionId)
                        if (adminNotes != null) put("adminNotes", adminNotes)
                    }
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val request = Request.Builder()
                        .url(backendUrl)
                        .post(jsonPayload.toString().toRequestBody(mediaType))
                        .build()
                    httpClient.newCall(request).execute().close()
                } catch (e: Exception) {
                    Log.w(TAG, "Apps Script update status failed: ${e.message}")
                }
            }

            Unit
        }
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

    private fun mapDocToResource(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        defaultCollege: String,
        defaultCourse: String,
        defaultYear: Int
    ): ResourceItem? {
        val data = doc.data ?: return null
        return ResourceItem(
            id = doc.id,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            collegeId = data["collegeId"] as? String ?: defaultCollege,
            courseId = data["courseId"] as? String ?: defaultCourse,
            academicYear = (data["academicYear"] as? Number)?.toInt() ?: defaultYear,
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
    }

    private fun mapJsonToResource(json: JSONObject): ResourceItem? {
        val id = json.optString("id", "")
        if (id.isBlank()) return null
        val fileUrl = json.optString("driveFileUrl", json.optString("fileUri", json.optString("previewUrl", null)))
        return ResourceItem(
            id = if (id.startsWith("res_")) id else "res_$id",
            title = json.optString("title", "Academic Resource"),
            description = json.optString("description", ""),
            collegeId = json.optString("collegeId", ""),
            courseId = json.optString("courseId", ""),
            academicYear = json.optInt("academicYear", 1),
            semester = json.optInt("semester", 1),
            subjectId = json.optString("subjectId", ""),
            subjectName = json.optString("subjectName", ""),
            resourceType = runCatching {
                ResourceType.valueOf(json.optString("resourceType", "NOTES"))
            }.getOrDefault(ResourceType.NOTES),
            examYear = json.optString("examYear", null),
            fileUri = fileUrl,
            driveFileId = json.optString("driveFileId", null),
            driveFileUrl = fileUrl,
            fileName = json.optString("fileName", "Document.pdf"),
            fileSizeFormatted = json.optString("fileSizeFormatted", "PDF Document"),
            fileMimeType = json.optString("fileMimeType", "application/pdf"),
            contributorName = json.optString("contributorName", "Student"),
            contributorEmail = json.optString("contributorEmail", null),
            isApproved = json.optBoolean("isApproved", true),
            publishedTimestamp = json.optLong("publishedTimestamp", json.optLong("submittedTimestamp", System.currentTimeMillis())),
            downloadCount = json.optInt("downloadCount", 0),
            tags = emptyList()
        )
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

    private fun mapJsonToSubmission(json: JSONObject): Submission? {
        val id = json.optString("id", "")
        if (id.isBlank()) return null
        val submittedTime = json.optLong("submittedTimestamp", System.currentTimeMillis())
        val defaultExpires = submittedTime + (3 * 24 * 60 * 60 * 1000L)
        val fileUrl = json.optString("driveFileUrl", json.optString("fileUri", json.optString("previewUrl", null)))

        return Submission(
            id = id,
            title = json.optString("title", "Untitled"),
            description = json.optString("description", ""),
            collegeId = json.optString("collegeId", ""),
            courseId = json.optString("courseId", ""),
            courseName = json.optString("courseName", ""),
            academicYear = json.optInt("academicYear", 1),
            semester = json.optInt("semester", 1),
            subjectId = json.optString("subjectId", ""),
            subjectName = json.optString("subjectName", ""),
            resourceType = runCatching {
                ResourceType.valueOf(json.optString("resourceType", "NOTES"))
            }.getOrDefault(ResourceType.NOTES),
            examYear = json.optString("examYear", null),
            fileUri = fileUrl,
            driveFileId = json.optString("driveFileId", null),
            driveFileUrl = fileUrl,
            fileName = json.optString("fileName", "Document.pdf"),
            fileSizeFormatted = json.optString("fileSizeFormatted", "Document"),
            fileMimeType = json.optString("fileMimeType", "application/pdf"),
            fileSizeBytes = json.optLong("fileSizeBytes", 0L),
            contributorName = json.optString("contributorName", "Student"),
            contributorEmail = json.optString("contributorEmail", null),
            status = runCatching {
                ApprovalStatus.valueOf(json.optString("status", "PENDING"))
            }.getOrDefault(ApprovalStatus.PENDING),
            adminFeedback = json.optString("adminFeedback", null),
            submittedTimestamp = submittedTime,
            reviewedTimestamp = if (json.has("reviewedTimestamp") && !json.isNull("reviewedTimestamp")) json.optLong("reviewedTimestamp") else null,
            expiresAt = json.optLong("expiresAt", defaultExpires)
        )
    }
}
