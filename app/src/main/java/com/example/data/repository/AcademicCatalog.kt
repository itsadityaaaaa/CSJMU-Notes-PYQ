package com.example.data.repository

import com.example.domain.model.AcademicYear
import com.example.domain.model.College
import com.example.domain.model.Course
import com.example.domain.model.Subject

object AcademicCatalog {

    val COLLEGES: List<College> = listOf(
        College(
            id = "csjmu_uiet",
            name = "UIET - University Institute of Engineering & Technology",
            shortCode = "UIET Kanpur",
            location = "CSJMU Main Campus, Kalyanpur, Kanpur"
        ),
        College(
            id = "csjmu_campus_science",
            name = "CSJMU Campus - School of Basic Sciences & Computing",
            shortCode = "CSJMU Science",
            location = "CSJMU Main Campus, Kanpur"
        ),
        College(
            id = "csjmu_campus_mgmt",
            name = "CSJMU Campus - School of Business Management",
            shortCode = "CSJMU Management",
            location = "CSJMU Main Campus, Kanpur"
        ),
        College(
            id = "dav_college_kanpur",
            name = "D.A.V. Post Graduate College",
            shortCode = "DAV College",
            location = "Civil Lines, Kanpur"
        ),
        College(
            id = "dbs_college_kanpur",
            name = "D.B.S. Post Graduate College",
            shortCode = "DBS College",
            location = "Govind Nagar, Kanpur"
        ),
        College(
            id = "csjmu_affiliated_general",
            name = "CSJMU Affiliated Degree Colleges (All Districts)",
            shortCode = "Affiliated Colleges",
            location = "Kanpur, Unnao, Fatehpur, Auraiya, Kannauj"
        )
    )

    val COURSES: List<Course> = listOf(
        // Engineering (UIET)
        Course(
            id = "btech_cse",
            collegeId = "csjmu_uiet",
            name = "B.Tech - Computer Science & Engineering",
            code = "B.TECH CSE",
            totalYears = 4,
            department = "Engineering"
        ),
        Course(
            id = "btech_it",
            collegeId = "csjmu_uiet",
            name = "B.Tech - Information Technology",
            code = "B.TECH IT",
            totalYears = 4,
            department = "Engineering"
        ),
        Course(
            id = "btech_ece",
            collegeId = "csjmu_uiet",
            name = "B.Tech - Electronics & Communication",
            code = "B.TECH ECE",
            totalYears = 4,
            department = "Engineering"
        ),
        Course(
            id = "btech_me",
            collegeId = "csjmu_uiet",
            name = "B.Tech - Mechanical Engineering",
            code = "B.TECH ME",
            totalYears = 4,
            department = "Engineering"
        ),
        // Computer Applications
        Course(
            id = "bca",
            collegeId = "csjmu_campus_science",
            name = "Bachelor of Computer Applications (BCA)",
            code = "BCA",
            totalYears = 3,
            department = "Computer Applications"
        ),
        Course(
            id = "mca",
            collegeId = "csjmu_uiet",
            name = "Master of Computer Applications (MCA)",
            code = "MCA",
            totalYears = 2,
            department = "Computer Applications"
        ),
        // Sciences
        Course(
            id = "bsc_cs",
            collegeId = "csjmu_campus_science",
            name = "B.Sc - Computer Science",
            code = "B.SC CS",
            totalYears = 3,
            department = "Science"
        ),
        Course(
            id = "bsc_pcm",
            collegeId = "csjmu_campus_science",
            name = "B.Sc - Physics, Chemistry, Mathematics (PCM)",
            code = "B.SC PCM",
            totalYears = 3,
            department = "Science"
        ),
        // Management & Commerce
        Course(
            id = "bba",
            collegeId = "csjmu_campus_mgmt",
            name = "Bachelor of Business Administration (BBA)",
            code = "BBA",
            totalYears = 3,
            department = "Management"
        ),
        Course(
            id = "mba",
            collegeId = "csjmu_campus_mgmt",
            name = "Master of Business Administration (MBA)",
            code = "MBA",
            totalYears = 2,
            department = "Management"
        ),
        Course(
            id = "bcom",
            collegeId = "csjmu_campus_mgmt",
            name = "Bachelor of Commerce (B.Com)",
            code = "B.COM",
            totalYears = 3,
            department = "Commerce"
        )
    )

    fun getYearsForCourse(course: Course): List<AcademicYear> {
        return (1..course.totalYears).map { yearNum ->
            val sem1 = (yearNum * 2) - 1
            val sem2 = yearNum * 2
            val ordinal = when (yearNum) {
                1 -> "1st"
                2 -> "2nd"
                3 -> "3rd"
                else -> "${yearNum}th"
            }
            AcademicYear(
                yearNumber = yearNum,
                label = "$ordinal Year (Sem $sem1 & $sem2)",
                semesters = listOf(sem1, sem2)
            )
        }
    }

    val SUBJECTS: List<Subject> = listOf(
        // B.Tech CSE
        Subject("cse_s1_math1", "btech_cse", 1, 1, "BAS103", "Engineering Mathematics - I"),
        Subject("cse_s1_phy", "btech_cse", 1, 1, "BAS101", "Engineering Physics"),
        Subject("cse_s1_cprog", "btech_cse", 1, 1, "BCS101", "Programming for Problem Solving (C)"),
        Subject("cse_s1_ee", "btech_cse", 1, 1, "BEE101", "Basic Electrical Engineering"),
        Subject("cse_s2_math2", "btech_cse", 1, 2, "BAS203", "Engineering Mathematics - II"),
        Subject("cse_s2_chem", "btech_cse", 1, 2, "BAS202", "Engineering Chemistry"),
        Subject("cse_s2_ed", "btech_cse", 1, 2, "BCE201", "Engineering Graphics & Design"),
        Subject("cse_s2_ec", "btech_cse", 1, 2, "BEC201", "Basic Electronics"),

        Subject("cse_s3_dsa", "btech_cse", 2, 3, "BCS301", "Data Structures & Algorithms"),
        Subject("cse_s3_discrete", "btech_cse", 2, 3, "BCS302", "Discrete Structures & Theory of Logic"),
        Subject("cse_s3_coa", "btech_cse", 2, 3, "BCS303", "Computer Organization & Architecture"),
        Subject("cse_s3_de", "btech_cse", 2, 3, "BEC301", "Digital Electronics"),

        Subject("cse_s4_os", "btech_cse", 2, 4, "BCS401", "Operating Systems"),
        Subject("cse_s4_tafl", "btech_cse", 2, 4, "BCS402", "Theory of Automata & Formal Languages"),
        Subject("cse_s4_oops", "btech_cse", 2, 4, "BCS403", "Object Oriented Programming using Java"),
        Subject("cse_s4_se", "btech_cse", 2, 4, "BCS404", "Software Engineering"),

        Subject("cse_s5_dbms", "btech_cse", 3, 5, "BCS501", "Database Management Systems (DBMS)"),
        Subject("cse_s5_daa", "btech_cse", 3, 5, "BCS502", "Design & Analysis of Algorithms"),
        Subject("cse_s5_compiler", "btech_cse", 3, 5, "BCS503", "Compiler Design"),
        Subject("cse_s5_wt", "btech_cse", 3, 5, "BCS504", "Web Technology"),

        Subject("cse_s6_cn", "btech_cse", 3, 6, "BCS601", "Computer Networks"),
        Subject("cse_s6_ml", "btech_cse", 3, 6, "BCS602", "Machine Learning"),
        Subject("cse_s6_cloud", "btech_cse", 3, 6, "BCS603", "Cloud Computing"),
        Subject("cse_s6_cyber", "btech_cse", 3, 6, "BCS604", "Cyber Security & Laws"),

        Subject("cse_s7_ai", "btech_cse", 4, 7, "BCS701", "Artificial Intelligence"),
        Subject("cse_s7_crypto", "btech_cse", 4, 7, "BCS702", "Cryptography & Network Security"),
        Subject("cse_s7_bigdata", "btech_cse", 4, 7, "BCS703", "Big Data Analytics"),
        Subject("cse_s8_iot", "btech_cse", 4, 8, "BCS801", "Internet of Things (IoT)"),
        Subject("cse_s8_dl", "btech_cse", 4, 8, "BCS802", "Deep Learning & Neural Networks"),

        // BCA
        Subject("bca_s1_fit", "bca", 1, 1, "BCA101", "Fundamentals of Information Technology"),
        Subject("bca_s1_c", "bca", 1, 1, "BCA102", "Programming Principles in C"),
        Subject("bca_s1_math", "bca", 1, 1, "BCA103", "Basic Mathematics"),
        Subject("bca_s2_ds", "bca", 1, 2, "BCA201", "Data Structures using C"),
        Subject("bca_s2_arch", "bca", 1, 2, "BCA202", "Digital Computer Architecture"),
        Subject("bca_s3_cpp", "bca", 2, 3, "BCA301", "Object Oriented Programming in C++"),
        Subject("bca_s3_os", "bca", 2, 3, "BCA302", "Operating System Concepts"),
        Subject("bca_s4_dbms", "bca", 2, 4, "BCA401", "Database Management Systems"),
        Subject("bca_s4_net", "bca", 2, 4, "BCA402", "Computer Networks"),
        Subject("bca_s5_java", "bca", 3, 5, "BCA501", "Java Programming"),
        Subject("bca_s5_web", "bca", 3, 5, "BCA502", "Web Technologies & Internet"),
        Subject("bca_s6_python", "bca", 3, 6, "BCA601", "Python Programming & Project"),

        // BBA
        Subject("bba_s1_pom", "bba", 1, 1, "BBA101", "Principles of Management"),
        Subject("bba_s1_fa", "bba", 1, 1, "BBA102", "Financial Accounting"),
        Subject("bba_s1_eco", "bba", 1, 1, "BBA103", "Business Economics"),
        Subject("bba_s2_ob", "bba", 1, 2, "BBA201", "Organizational Behaviour"),
        Subject("bba_s2_mm", "bba", 1, 2, "BBA202", "Marketing Management"),
        Subject("bba_s3_hrm", "bba", 2, 3, "BBA301", "Human Resource Management"),
        Subject("bba_s3_fm", "bba", 2, 3, "BBA302", "Financial Management"),
        Subject("bba_s4_blaw", "bba", 2, 4, "BBA401", "Business Law"),
        Subject("bba_s4_stat", "bba", 2, 4, "BBA402", "Business Research Methods"),
        Subject("bba_s5_sm", "bba", 3, 5, "BBA501", "Strategic Management"),
        Subject("bba_s6_entre", "bba", 3, 6, "BBA601", "Entrepreneurship Development"),

        // B.Com
        Subject("bcom_s1_fa", "bcom", 1, 1, "BC101", "Financial Accounting"),
        Subject("bcom_s1_bo", "bcom", 1, 1, "BC102", "Business Organization"),
        Subject("bcom_s2_ca", "bcom", 1, 2, "BC201", "Corporate Accounting"),
        Subject("bcom_s2_law", "bcom", 1, 2, "BC202", "Company Law"),
        Subject("bcom_s3_cost", "bcom", 2, 3, "BC301", "Cost Accounting"),
        Subject("bcom_s4_gst", "bcom", 2, 4, "BC401", "Goods & Services Tax (GST)"),
        Subject("bcom_s5_it", "bcom", 3, 5, "BC501", "Income Tax Law & Accounts"),
        Subject("bcom_s6_audit", "bcom", 3, 6, "BC601", "Auditing & Corporate Governance")
    )

    fun getCoursesForCollege(collegeId: String): List<Course> {
        return COURSES.filter { it.collegeId == collegeId || collegeId == "csjmu_affiliated_general" }
    }

    fun getSubjects(courseId: String, year: Int, semester: Int? = null): List<Subject> {
        return SUBJECTS.filter {
            it.courseId == courseId && it.year == year && (semester == null || it.semester == semester)
        }
    }
}
