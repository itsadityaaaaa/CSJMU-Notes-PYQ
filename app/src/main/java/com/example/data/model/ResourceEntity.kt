package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.ResourceItem
import com.example.domain.model.ResourceType
import com.example.domain.model.Submission

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val collegeId: String,
    val courseId: String,
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
    val isApproved: Boolean = true,
    val publishedTimestamp: Long = System.currentTimeMillis(),
    val downloadCount: Int = 0,
    val tags: List<String> = emptyList()
) {
    fun toDomain(): ResourceItem = ResourceItem(
        id = id,
        title = title,
        description = description,
        collegeId = collegeId,
        courseId = courseId,
        academicYear = academicYear,
        semester = semester,
        subjectId = subjectId,
        subjectName = subjectName,
        resourceType = resourceType,
        examYear = examYear,
        fileUri = fileUri,
        fileName = fileName,
        fileSizeFormatted = fileSizeFormatted,
        fileMimeType = fileMimeType,
        contributorName = contributorName,
        contributorEmail = contributorEmail,
        isApproved = isApproved,
        publishedTimestamp = publishedTimestamp,
        downloadCount = downloadCount,
        tags = tags
    )

    companion object {
        fun fromDomain(item: ResourceItem): ResourceEntity = ResourceEntity(
            id = item.id,
            title = item.title,
            description = item.description,
            collegeId = item.collegeId,
            courseId = item.courseId,
            academicYear = item.academicYear,
            semester = item.semester,
            subjectId = item.subjectId,
            subjectName = item.subjectName,
            resourceType = item.resourceType,
            examYear = item.examYear,
            fileUri = item.fileUri,
            fileName = item.fileName,
            fileSizeFormatted = item.fileSizeFormatted,
            fileMimeType = item.fileMimeType,
            contributorName = item.contributorName,
            contributorEmail = item.contributorEmail,
            isApproved = item.isApproved,
            publishedTimestamp = item.publishedTimestamp,
            downloadCount = item.downloadCount,
            tags = item.tags
        )
    }
}

@Entity(tableName = "submissions")
data class SubmissionEntity(
    @PrimaryKey val id: String,
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
) {
    fun toDomain(): Submission = Submission(
        id = id,
        title = title,
        description = description,
        collegeId = collegeId,
        courseId = courseId,
        courseName = courseName,
        academicYear = academicYear,
        semester = semester,
        subjectId = subjectId,
        subjectName = subjectName,
        resourceType = resourceType,
        examYear = examYear,
        fileUri = fileUri,
        fileName = fileName,
        fileSizeFormatted = fileSizeFormatted,
        fileMimeType = fileMimeType,
        contributorName = contributorName,
        contributorEmail = contributorEmail,
        status = status,
        adminFeedback = adminFeedback,
        submittedTimestamp = submittedTimestamp,
        reviewedTimestamp = reviewedTimestamp
    )

    companion object {
        fun fromDomain(sub: Submission): SubmissionEntity = SubmissionEntity(
            id = sub.id,
            title = sub.title,
            description = sub.description,
            collegeId = sub.collegeId,
            courseId = sub.courseId,
            courseName = sub.courseName,
            academicYear = sub.academicYear,
            semester = sub.semester,
            subjectId = sub.subjectId,
            subjectName = sub.subjectName,
            resourceType = sub.resourceType,
            examYear = sub.examYear,
            fileUri = sub.fileUri,
            fileName = sub.fileName,
            fileSizeFormatted = sub.fileSizeFormatted,
            fileMimeType = sub.fileMimeType,
            contributorName = sub.contributorName,
            contributorEmail = sub.contributorEmail,
            status = sub.status,
            adminFeedback = sub.adminFeedback,
            submittedTimestamp = sub.submittedTimestamp,
            reviewedTimestamp = sub.reviewedTimestamp
        )
    }
}
