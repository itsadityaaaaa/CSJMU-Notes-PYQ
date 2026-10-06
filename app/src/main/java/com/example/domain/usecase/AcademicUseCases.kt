package com.example.domain.usecase

import com.example.domain.model.ApprovalStatus
import com.example.domain.model.ResourceItem
import com.example.domain.model.ResourceType
import com.example.domain.model.Submission
import com.example.domain.repository.IResourceRepository
import com.example.domain.repository.ISubmissionRepository
import kotlinx.coroutines.flow.Flow

class GetResourcesUseCase(
    private val repository: IResourceRepository
) {
    operator fun invoke(
        collegeId: String,
        courseId: String,
        academicYear: Int,
        semester: Int? = null,
        resourceType: ResourceType? = null
    ): Flow<List<ResourceItem>> {
        return repository.getResources(collegeId, courseId, academicYear, semester, resourceType)
    }
}

class SearchResourcesUseCase(
    private val repository: IResourceRepository
) {
    operator fun invoke(
        query: String,
        collegeId: String? = null,
        courseId: String? = null
    ): Flow<List<ResourceItem>> {
        return repository.searchResources(query, collegeId, courseId)
    }
}

class SubmitNoteUseCase(
    private val submissionRepository: ISubmissionRepository
) {
    suspend operator fun invoke(submission: Submission): Result<String> {
        if (submission.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Title is required"))
        }
        if (submission.description.isBlank()) {
            return Result.failure(IllegalArgumentException("Description is required"))
        }
        if (submission.contributorName.isBlank()) {
            return Result.failure(IllegalArgumentException("Contributor name is required"))
        }
        return submissionRepository.submitForReview(submission)
    }
}

class ModerateSubmissionUseCase(
    private val submissionRepository: ISubmissionRepository
) {
    suspend fun approve(submission: Submission, adminNotes: String? = null): Result<ResourceItem> {
        return submissionRepository.approveAndPublish(submission, adminNotes)
    }

    suspend fun saveEdits(submission: Submission): Result<Unit> {
        return submissionRepository.updateSubmission(submission)
    }

    suspend fun reject(submissionId: String, reason: String): Result<Unit> {
        if (reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Rejection reason cannot be empty"))
        }
        return submissionRepository.rejectSubmission(submissionId, reason)
    }
}
