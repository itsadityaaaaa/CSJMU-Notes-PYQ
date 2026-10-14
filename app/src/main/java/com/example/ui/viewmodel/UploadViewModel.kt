package com.example.ui.viewmodel

import android.content.Context
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
import com.example.domain.model.Submission
import com.example.domain.usecase.SubmitNoteUseCase
import com.example.util.AnalyticsHelper
import com.example.util.FileCompressionUtil
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
    val attachedFileCount: Int = 0,
    // WhatsApp-style compression information
    val isCompressing: Boolean = false,
    val compressionSavingsText: String? = null,
    val originalFileSizeFormatted: String? = null,
    val isCompressed: Boolean = false,
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
        val years = AcademicCatalog.getYearsForCourse(course)
        val defaultYear = years.firstOrNull() ?: AcademicYear(1, "1st Year", listOf(1, 2))
        val defaultSem = defaultYear.semesters.firstOrNull() ?: 1

        _formState.value = _formState.value.copy(
            course = course,
            year = defaultYear,
            semester = defaultSem
        )
    }

    fun updateYear(year: AcademicYear) {
        val defaultSem = year.semesters.firstOrNull() ?: 1
        _formState.value = _formState.value.copy(
            year = year,
            semester = defaultSem
        )
    }

    fun updateSemester(semester: Int) {
        _formState.value = _formState.value.copy(semester = semester)
    }

    fun updateDescriptiveSubjectName(name: String) {
        _formState.value = _formState.value.copy(descriptiveSubjectName = name, errorMessage = null)
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

    /**
     * Accepts file selection and automatically applies WhatsApp-style smart compression:
     * - Downsamples oversized JPG photos (camera shots) to optimal reading dimensions
     * - Re-encodes at 82% quality to slash file size by 60-80% while retaining razor-sharp text
     * - Optimizes PDF pages if oversized
     */
    fun setAttachedFileWithCompression(
        context: Context,
        uri: Uri,
        name: String,
        initialSizeFormatted: String,
        mimeType: String
    ) {
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

        val effectiveMime = if (lowerName.endsWith(".pdf") || mimeType == "application/pdf") {
            "application/pdf"
        } else {
            "image/jpeg"
        }

        // Set initial state and trigger compression
        _formState.value = _formState.value.copy(
            attachedFileUri = uri.toString(),
            attachedFileName = name,
            attachedFileSize = initialSizeFormatted,
            attachedMimeType = effectiveMime,
            originalFileSizeFormatted = initialSizeFormatted,
            isCompressing = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val result = FileCompressionUtil.compressFile(
                    context = context,
                    sourceUri = uri,
                    fileName = name,
                    mimeType = effectiveMime
                )

                if (result.isCompressed && result.savingsPercentage > 0) {
                    _formState.value = _formState.value.copy(
                        attachedFileUri = result.compressedUri.toString(),
                        attachedFileName = result.displayName,
                        attachedFileSize = result.formattedCompressedSize,
                        attachedMimeType = result.mimeType,
                        isCompressing = false,
                        isCompressed = true,
                        compressionSavingsText = "Saved ${result.savingsPercentage}% size (${result.formattedOriginalSize} → ${result.formattedCompressedSize})"
                    )
                } else {
                    _formState.value = _formState.value.copy(
                        attachedFileUri = uri.toString(),
                        attachedFileName = name,
                        attachedFileSize = initialSizeFormatted,
                        attachedMimeType = effectiveMime,
                        isCompressing = false,
                        isCompressed = false,
                        compressionSavingsText = null
                    )
                }
            } catch (e: Exception) {
                // Fallback gracefully to original
                _formState.value = _formState.value.copy(
                    attachedFileUri = uri.toString(),
                    attachedFileName = name,
                    attachedFileSize = initialSizeFormatted,
                    attachedMimeType = effectiveMime,
                    isCompressing = false,
                    isCompressed = false,
                    compressionSavingsText = null
                )
            }
        }
    }

    /**
     * Accepts multiple JPG/JPEG images in a single selection and automatically:
     * 1. Compresses every photo WhatsApp-style (scales down oversized camera shots & quality optimization)
     * 2. Stitches them seamlessly into an organized single multi-page PDF document
     * 3. Prepares it for unified upload to Google Drive and review by admins!
     */
    fun setMultipleImagesWithCompression(
        context: Context,
        uris: List<Uri>,
        initialSummaryName: String,
        totalSizeFormatted: String
    ) {
        if (uris.isEmpty()) return

        if (uris.size == 1) {
            val uri = uris.first()
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            setAttachedFileWithCompression(context, uri, initialSummaryName, totalSizeFormatted, mimeType)
            return
        }

        _formState.value = _formState.value.copy(
            attachedFileUri = uris.first().toString(),
            attachedFileName = "$initialSummaryName (${uris.size} photos)",
            attachedFileSize = totalSizeFormatted,
            attachedMimeType = "application/pdf",
            attachedFileCount = uris.size,
            originalFileSizeFormatted = totalSizeFormatted,
            isCompressing = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val subjectTag = _formState.value.descriptiveSubjectName.ifBlank { "Notes" }
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val result = FileCompressionUtil.convertImagesToPdf(
                    context = context,
                    imageUris = uris,
                    outputBaseName = "${subjectTag}_compiled"
                )

                _formState.value = _formState.value.copy(
                    attachedFileUri = result.compressedUri.toString(),
                    attachedFileName = "${result.displayName} (${uris.size} pages compiled)",
                    attachedFileSize = result.formattedCompressedSize,
                    attachedMimeType = "application/pdf",
                    attachedFileCount = uris.size,
                    isCompressing = false,
                    isCompressed = true,
                    compressionSavingsText = if (result.savingsPercentage > 0) {
                        "Merged ${uris.size} JPGs into PDF! Saved ${result.savingsPercentage}% size (${result.formattedOriginalSize} → ${result.formattedCompressedSize})"
                    } else {
                        "Merged ${uris.size} JPGs into optimized multi-page PDF (${result.formattedCompressedSize})"
                    }
                )
            } catch (e: Exception) {
                // If PDF conversion fails, fallback to first image
                _formState.value = _formState.value.copy(
                    attachedFileUri = uris.first().toString(),
                    attachedFileName = initialSummaryName,
                    attachedFileSize = totalSizeFormatted,
                    attachedMimeType = "image/jpeg",
                    attachedFileCount = 1,
                    isCompressing = false,
                    isCompressed = false,
                    compressionSavingsText = null
                )
            }
        }
    }

    fun clearAttachedFile() {
        _formState.value = _formState.value.copy(
            attachedFileUri = null,
            attachedFileName = null,
            attachedFileSize = null,
            attachedMimeType = null,
            attachedFileCount = 0,
            isCompressing = false,
            isCompressed = false,
            compressionSavingsText = null,
            originalFileSizeFormatted = null
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
                AnalyticsHelper.logDocumentUpload(
                    course = submission.courseName,
                    year = submission.academicYear,
                    subject = submission.subjectName,
                    type = submission.resourceType.name
                )
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
            attachedMimeType = null,
            isCompressing = false,
            isCompressed = false,
            compressionSavingsText = null,
            originalFileSizeFormatted = null
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
