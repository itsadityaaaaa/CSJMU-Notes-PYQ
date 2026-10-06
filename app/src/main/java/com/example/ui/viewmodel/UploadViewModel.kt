package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AcademicCatalog
import com.example.domain.model.AcademicYear
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.College
import com.example.domain.model.Course
import com.example.domain.model.ResourceType
import com.example.domain.model.Subject
import com.example.domain.model.Submission
import com.example.domain.usecase.SubmitNoteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class UploadFormState(
    val college: College = AcademicCatalog.COLLEGES.first(),
    val course: Course = AcademicCatalog.COURSES.first(),
    val year: AcademicYear = AcademicCatalog.getYearsForCourse(AcademicCatalog.COURSES.first()).first(),
    val semester: Int = 1,
    // Fully descriptive user-filled subject fields
    val descriptiveSubjectName: String = "",
    val subjectCode: String = "",
    val topicUnitCoverage: String = "",
    val resourceType: ResourceType = ResourceType.NOTES,
    val examYear: String = "2023-24",
    val title: String = "",
    val description: String = "",
    val contributorName: String = "",
    val contributorEmail: String = "",
    val attachedFileUri: String? = null,
    val attachedFileName: String? = null,
    val attachedFileSize: String? = null,
    val attachedMimeType: String? = null,
    val isSubmitting: Boolean = false,
    val submissionSuccessId: String? = null,
    val errorMessage: String? = null
)

class UploadViewModel(
    private val submitNoteUseCase: SubmitNoteUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(UploadFormState())
    val formState: StateFlow<UploadFormState> = _formState.asStateFlow()

    fun updateCollege(college: College) {
        val courses = AcademicCatalog.getCoursesForCollege(college.id)
        val defaultCourse = courses.firstOrNull() ?: AcademicCatalog.COURSES.first()
        val defaultYears = AcademicCatalog.getYearsForCourse(defaultCourse)
        val defaultYear = defaultYears.firstOrNull() ?: AcademicYear(1, "1st Year", listOf(1, 2))
        val defaultSem = defaultYear.semesters.firstOrNull() ?: 1

        _formState.value = _formState.value.copy(
            college = college,
            course = defaultCourse,
            year = defaultYear,
            semester = defaultSem
        )
    }

    fun updateCourse(course: Course) {
        val defaultYears = AcademicCatalog.getYearsForCourse(course)
        val defaultYear = defaultYears.firstOrNull() ?: AcademicYear(1, "1st Year", listOf(1, 2))
        val defaultSem = defaultYear.semesters.firstOrNull() ?: 1

        _formState.value = _formState.value.copy(
            course = course,
            year = defaultYear,
            semester = defaultSem
        )
    }

    fun updateYear(year: AcademicYear) {
        val defaultSem = year.semesters.firstOrNull() ?: (year.yearNumber * 2 - 1)
        _formState.value = _formState.value.copy(
            year = year,
            semester = defaultSem
        )
    }

    fun updateSemester(semester: Int) {
        _formState.value = _formState.value.copy(semester = semester)
    }

    fun updateDescriptiveSubjectName(name: String) {
        _formState.value = _formState.value.copy(
            descriptiveSubjectName = name,
            errorMessage = null
        )
    }

    fun updateSubjectCode(code: String) {
        _formState.value = _formState.value.copy(subjectCode = code)
    }

    fun updateTopicUnitCoverage(coverage: String) {
        _formState.value = _formState.value.copy(topicUnitCoverage = coverage)
    }

    fun updateResourceType(type: ResourceType) {
        _formState.value = _formState.value.copy(resourceType = type)
    }

    fun updateExamYear(examYear: String) {
        _formState.value = _formState.value.copy(examYear = examYear)
    }

    fun updateTitle(title: String) {
        _formState.value = _formState.value.copy(title = title, errorMessage = null)
    }

    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description, errorMessage = null)
    }

    fun updateContributorName(name: String) {
        _formState.value = _formState.value.copy(contributorName = name, errorMessage = null)
    }

    fun updateContributorEmail(email: String) {
        _formState.value = _formState.value.copy(contributorEmail = email)
    }

    fun setFileError(message: String) {
        _formState.value = _formState.value.copy(errorMessage = message)
    }

    fun setAttachedFile(uri: Uri, name: String, sizeFormatted: String, mimeType: String) {
        val lowerName = name.lowercase()
        val isAllowed = lowerName.endsWith(".pdf") ||
                lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".jpeg") ||
                mimeType == "application/pdf" ||
                mimeType == "image/jpeg" ||
                mimeType == "image/jpg"

        if (!isAllowed) {
            _formState.value = _formState.value.copy(
                errorMessage = "Unsupported file type. Uploads are strictly restricted to PDF (.pdf) and JPG/JPEG (.jpg, .jpeg) formats."
            )
            return
        }

        _formState.value = _formState.value.copy(
            attachedFileUri = uri.toString(),
            attachedFileName = name,
            attachedFileSize = sizeFormatted,
            attachedMimeType = if (lowerName.endsWith(".pdf") || mimeType == "application/pdf") "application/pdf" else "image/jpeg",
            errorMessage = null
        )
    }

    fun clearAttachedFile() {
        _formState.value = _formState.value.copy(
            attachedFileUri = null,
            attachedFileName = null,
            attachedFileSize = null,
            attachedMimeType = null
        )
    }

    fun submitNote() {
        val state = _formState.value

        // ONLY 4 REQUIRED FIELDS: College, Course, Subject Name, and Uploader Name
        if (state.descriptiveSubjectName.trim().isBlank()) {
            _formState.value = state.copy(errorMessage = "Please enter the Subject Name")
            return
        }

        if (state.contributorName.trim().isBlank()) {
            _formState.value = state.copy(errorMessage = "Please enter your Name as Uploader")
            return
        }

        val subjectCodeFormatted = if (state.subjectCode.trim().isNotBlank()) {
            " [${state.subjectCode.trim()}]"
        } else ""

        val effectiveSubjectName = "${state.descriptiveSubjectName.trim()}$subjectCodeFormatted"
        val effectiveSubjectId = "subj_${UUID.randomUUID().toString().take(8)}"

        // Title and Description are optional! Generate clear academic fallbacks if blank
        val effectiveTitle = if (state.title.trim().isNotBlank()) {
            state.title.trim()
        } else {
            "${state.resourceType.displayName} - ${state.descriptiveSubjectName.trim()}"
        }

        val effectiveDescription = buildString {
            if (state.topicUnitCoverage.trim().isNotBlank()) {
                append("Coverage: ${state.topicUnitCoverage.trim()}\n\n")
            }
            if (state.description.trim().isNotBlank()) {
                append(state.description.trim())
            } else {
                append("Academic study resource for ${state.descriptiveSubjectName.trim()} (${state.course.name}), Year ${state.year.yearNumber}, Semester ${state.semester}.")
            }
        }

        _formState.value = state.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val submission = Submission(
                id = "sub_${UUID.randomUUID()}",
                title = effectiveTitle,
                description = effectiveDescription,
                collegeId = state.college.id,
                courseId = state.course.id,
                courseName = state.course.name,
                academicYear = state.year.yearNumber,
                semester = state.semester,
                subjectId = effectiveSubjectId,
                subjectName = effectiveSubjectName,
                resourceType = state.resourceType,
                examYear = if (state.resourceType == ResourceType.PYQ) state.examYear.trim() else null,
                fileUri = state.attachedFileUri,
                fileName = state.attachedFileName ?: "Academic_Notes.pdf",
                fileSizeFormatted = state.attachedFileSize ?: "Document",
                fileMimeType = state.attachedMimeType ?: "application/pdf",
                contributorName = state.contributorName.trim(),
                contributorEmail = state.contributorEmail.trim().takeIf { it.isNotBlank() },
                status = ApprovalStatus.PENDING,
                submittedTimestamp = System.currentTimeMillis()
            )

            val result = submitNoteUseCase(submission)
            if (result.isSuccess) {
                _formState.value = state.copy(
                    isSubmitting = false,
                    submissionSuccessId = result.getOrNull(),
                    errorMessage = null
                )
            } else {
                _formState.value = state.copy(
                    isSubmitting = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit note for review"
                )
            }
        }
    }

    fun resetSuccess() {
        _formState.value = _formState.value.copy(
            submissionSuccessId = null,
            title = "",
            description = "",
            descriptiveSubjectName = "",
            subjectCode = "",
            topicUnitCoverage = "",
            attachedFileUri = null,
            attachedFileName = null,
            attachedFileSize = null,
            attachedMimeType = null
        )
    }

    class Factory(
        private val submitNoteUseCase: SubmitNoteUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UploadViewModel(submitNoteUseCase) as T
        }
    }
}
