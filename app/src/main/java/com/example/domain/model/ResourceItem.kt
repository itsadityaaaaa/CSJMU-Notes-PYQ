package com.example.domain.model

data class ResourceItem(
    val id: String,
    val title: String,
    val description: String,
    val collegeId: String,
    val courseId: String,
    val academicYear: Int,
    val semester: Int,
    val subjectId: String,
    val subjectName: String,
    val resourceType: ResourceType,
    val examYear: String? = null, // e.g. "2023-24 (End Sem)" for PYQ
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileSizeFormatted: String? = null,
    val fileMimeType: String? = null,
    val contributorName: String,
    val contributorEmail: String? = null,
    val isApproved: Boolean = true,
    val publishedTimestamp: Long = System.currentTimeMillis(),
    val downloadCount: Int = 0,
    val tags: List<String> = emptyList()
)

data class Submission(
    val id: String,
    val title: String,
    val description: String,
    val collegeId: String,
    val courseId: String,
    val courseName: String,
    val academicYear: Int,
    val semester: Int,
    val subjectId: String,
    val subjectName: String,
    val resourceType: ResourceType,
    val examYear: String? = null,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileSizeFormatted: String? = null,
    val fileMimeType: String? = null,
    val contributorName: String,
    val contributorEmail: String? = null,
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val adminFeedback: String? = null,
    val submittedTimestamp: Long = System.currentTimeMillis(),
    val reviewedTimestamp: Long? = null
)
