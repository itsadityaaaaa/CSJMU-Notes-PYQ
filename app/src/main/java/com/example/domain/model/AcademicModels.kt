package com.example.domain.model

enum class ResourceType(val displayName: String, val badgeLabel: String) {
    NOTES("Lecture Notes", "Notes"),
    PYQ("Previous Year Question Paper", "PYQ"),
    SYLLABUS("Syllabus & Curriculum", "Syllabus"),
    FORMULA_SHEET("Formula / Quick Revision", "Revision")
}

enum class ApprovalStatus(val displayName: String) {
    PENDING("Pending Review"),
    APPROVED("Approved & Published"),
    REJECTED("Rejected")
}

data class College(
    val id: String,
    val name: String,
    val shortCode: String,
    val location: String
)

data class Course(
    val id: String,
    val collegeId: String,
    val name: String,
    val code: String,
    val totalYears: Int,
    val department: String
)

data class AcademicYear(
    val yearNumber: Int,
    val label: String,
    val semesters: List<Int>
)

data class Subject(
    val id: String,
    val courseId: String,
    val year: Int,
    val semester: Int,
    val code: String,
    val name: String
)
