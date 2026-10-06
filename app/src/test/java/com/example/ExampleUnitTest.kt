package com.example

import com.example.data.repository.AcademicCatalog
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.ResourceType
import com.example.domain.model.Submission
import com.example.ui.viewmodel.AdminViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAcademicCatalog_CoursesAndYears() {
        val btechCourse = AcademicCatalog.COURSES.first { it.id == "btech_cse" }
        assertEquals(4, btechCourse.totalYears)

        val years = AcademicCatalog.getYearsForCourse(btechCourse)
        assertEquals(4, years.size)
        assertEquals(listOf(1, 2), years[0].semesters)
        assertEquals(listOf(7, 8), years[3].semesters)
    }

    @Test
    fun testAcademicCatalog_SubjectsMapping() {
        val cseSubjectsSem3 = AcademicCatalog.getSubjects("btech_cse", 2, 3)
        assertTrue(cseSubjectsSem3.isNotEmpty())
        assertTrue(cseSubjectsSem3.any { it.name.contains("Data Structures") })
    }

    @Test
    fun testSubmission_PendingInitialState() {
        val submission = Submission(
            id = "sub_test_01",
            title = "Operating Systems Unit 1 Notes",
            description = "Complete process management notes",
            collegeId = "csjmu_uiet",
            courseId = "btech_cse",
            courseName = "B.Tech CSE",
            academicYear = 2,
            semester = 4,
            subjectId = "cse_s4_os",
            subjectName = "Operating Systems",
            resourceType = ResourceType.NOTES,
            contributorName = "Student A"
        )

        assertEquals(ApprovalStatus.PENDING, submission.status)
        assertNotNull(submission.submittedTimestamp)
    }

    @Test
    fun testAdminKey_SixteenLettersAndLockoutDuration() {
        assertEquals(16, AdminViewModel.MASTER_ADMIN_ID.length)
        assertEquals("CSJMUADMINSECURE", AdminViewModel.MASTER_ADMIN_ID)
        assertEquals(30, AdminViewModel.LOCKOUT_DURATION_SECONDS)
    }

    @Test
    fun testAdminEditSubmissionFields() {
        val original = Submission(
            id = "sub_edit_01",
            title = "Rough Notes",
            description = "Initial draft",
            collegeId = "csjmu_uiet",
            courseId = "btech_cse",
            courseName = "B.Tech CSE",
            academicYear = 1,
            semester = 1,
            subjectId = "subj_01",
            subjectName = "Engineering Maths",
            resourceType = ResourceType.NOTES,
            contributorName = "Student Contributor"
        )

        // Admin modifies description and fields
        val edited = original.copy(
            title = "Engineering Mathematics-I Complete Lecture Notes",
            description = "Curated Unit 1-5 notes with solved calculus problems and formula sheet.",
            subjectName = "Engineering Mathematics-I [BAS-103]",
            adminFeedback = "Curated by CSJMU Admin for quality"
        )

        assertEquals("Engineering Mathematics-I Complete Lecture Notes", edited.title)
        assertTrue(edited.description.contains("Curated Unit 1-5"))
        assertEquals("Engineering Mathematics-I [BAS-103]", edited.subjectName)
        assertEquals("Curated by CSJMU Admin for quality", edited.adminFeedback)
    }

    @Test
    fun testRestrictedFileExtensions() {
        val allowedExtensions = listOf("pdf", "jpg", "jpeg")
        val testFiles = mapOf(
            "notes.pdf" to true,
            "scan.jpg" to true,
            "diagram.jpeg" to true,
            "document.docx" to false,
            "archive.zip" to false,
            "app.apk" to false
        )

        testFiles.forEach { (filename, expectedAllowed) ->
            val ext = filename.substringAfterLast('.', "")
            val isAllowed = allowedExtensions.contains(ext.lowercase())
            assertEquals("Failed for $filename", expectedAllowed, isAllowed)
        }
    }
}
