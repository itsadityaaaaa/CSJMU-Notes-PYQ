package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AcademicCatalog
import com.example.domain.model.ResourceType
import com.example.ui.theme.AcademicNavyContainer
import com.example.ui.theme.AcademicNavyDark
import com.example.ui.theme.AcademicNavyPrimary
import com.example.ui.theme.AcademicOnNavyContainer
import com.example.ui.theme.EditorialError
import com.example.ui.theme.EditorialErrorContainer
import com.example.ui.theme.EditorialErrorText
import com.example.ui.theme.EditorialSuccess
import com.example.ui.theme.EditorialSuccessContainer
import com.example.ui.theme.EditorialWarningContainer
import com.example.ui.theme.EditorialWarningText
import com.example.ui.theme.NaturalCardSurface
import com.example.ui.theme.NaturalOutline
import com.example.ui.theme.NaturalSurfaceVariant
import com.example.ui.theme.NaturalTextMuted
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.ui.viewmodel.UploadViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onNavigateToAdmin: () -> Unit,
    onNavigateToSubmissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val context = LocalContext.current

    var showCollegeMenu by remember { mutableStateOf(false) }
    var showCourseMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }

    val availableCourses = AcademicCatalog.getCoursesForCollege(formState.college.id)
    val availableYears = AcademicCatalog.getYearsForCourse(formState.course)

    // Android SAF File Picker for single PDF or single JPG
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val (displayName, sizeFormatted) = getFileMetadata(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: ""
            viewModel.setAttachedFileWithCompression(context, uri, displayName, sizeFormatted, mimeType)
        }
    }

    // Android SAF Multiple Images Picker (allows selecting multiple JPG/JPEG pages at once)
    val multiImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (uris.size == 1) {
                val uri = uris.first()
                val (displayName, sizeFormatted) = getFileMetadata(context, uri)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                viewModel.setAttachedFileWithCompression(context, uri, displayName, sizeFormatted, mimeType)
            } else {
                // Calculate total size across all selected images
                var totalBytes = 0L
                uris.forEach { u ->
                    try {
                        context.contentResolver.openFileDescriptor(u, "r")?.use { pfd ->
                            totalBytes += pfd.statSize
                        }
                    } catch (_: Exception) {}
                }
                val totalFormatted = when {
                    totalBytes >= 1024 * 1024 -> String.format("%.2f MB", totalBytes / (1024.0 * 1024.0))
                    totalBytes >= 1024 -> String.format("%d KB", totalBytes / 1024)
                    else -> "$totalBytes B"
                }
                val firstMetadata = getFileMetadata(context, uris.first())
                val summaryName = firstMetadata.first.substringBeforeLast(".")
                viewModel.setMultipleImagesWithCompression(
                    context = context,
                    uris = uris,
                    initialSummaryName = summaryName,
                    totalSizeFormatted = totalFormatted
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("upload_screen")
    ) {
        // Section Header
        Text(
            text = "Upload Academic Notes & PYQ",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Upload student notes and question papers for CSJMU Kanpur. Submissions are reviewed by our academic admin panel before public publication.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        // Administrative Moderation Notice
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Moderation Gate Active",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your submission is placed in the review queue. Once verified by an admin, it is published to the live CSJMU course catalog.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- STEP 1: ACADEMIC LEVEL ---
        Text(
            text = "1. College & Degree Program",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // College Picker - REQUIRED (*)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                onClick = { showCollegeMenu = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_college_select"),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Campus / College",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AcademicNavyPrimary
                            )
                            Text(
                                text = " *",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EditorialError
                            )
                        }
                        Text(
                            text = formState.college.shortCode,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NaturalTextPrimary
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NaturalTextSecondary)
                }
            }
            DropdownMenu(expanded = showCollegeMenu, onDismissRequest = { showCollegeMenu = false }) {
                AcademicCatalog.COLLEGES.forEach { col ->
                    DropdownMenuItem(
                        text = { Text(col.name, color = NaturalTextPrimary) },
                        onClick = {
                            viewModel.updateCollege(col)
                            showCollegeMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Course Picker - REQUIRED (*)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                onClick = { showCourseMenu = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_course_select"),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Degree Course",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AcademicNavyPrimary
                            )
                            Text(
                                text = " *",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EditorialError
                            )
                        }
                        Text(
                            text = formState.course.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NaturalTextPrimary
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NaturalTextSecondary)
                }
            }
            DropdownMenu(expanded = showCourseMenu, onDismissRequest = { showCourseMenu = false }) {
                availableCourses.forEach { crs ->
                    DropdownMenuItem(
                        text = { Text(crs.name, color = NaturalTextPrimary) },
                        onClick = {
                            viewModel.updateCourse(crs)
                            showCourseMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Academic Year Dropdown Picker (Full 4-Year Support, completely responsive)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                onClick = { showYearMenu = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_year_select"),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Academic Year",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AcademicNavyPrimary
                            )
                            Text(
                                text = " *",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EditorialError
                            )
                        }
                        Text(
                            text = formState.year.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NaturalTextPrimary
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NaturalTextSecondary)
                }
            }
            DropdownMenu(expanded = showYearMenu, onDismissRequest = { showYearMenu = false }) {
                availableYears.forEach { yr ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = yr.label,
                                color = NaturalTextPrimary,
                                fontWeight = if (formState.year.yearNumber == yr.yearNumber) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            viewModel.updateYear(yr)
                            showYearMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Semester Selection Chips
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Academic Semester",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AcademicNavyPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                formState.year.semesters.forEach { sem ->
                    FilterChip(
                        selected = formState.semester == sem,
                        onClick = { viewModel.updateSemester(sem) },
                        label = { Text("Semester $sem") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AcademicNavyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // --- STEP 2: DESCRIPTIVE SUBJECT DETAILS ---
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "2. Subject Details",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AcademicNavyPrimary
        )
        Text(
            text = "Enter the subject name and optional paper code or unit coverage.",
            style = MaterialTheme.typography.bodySmall,
            color = NaturalTextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )

        // Descriptive Subject Name - REQUIRED (*)
        OutlinedTextField(
            value = formState.descriptiveSubjectName,
            onValueChange = { viewModel.updateDescriptiveSubjectName(it) },
            label = {
                Row {
                    Text("Subject Name / Paper Title")
                    Text(" *", color = EditorialError, fontWeight = FontWeight.Bold)
                }
            },
            placeholder = { Text("e.g. Data Structures & Algorithms, Engineering Chemistry, Business Law") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("upload_subject_name_input"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AcademicNavyPrimary,
                focusedLabelColor = AcademicNavyPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subject Code & Topic Coverage (Optional - no asterisk)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = formState.subjectCode,
                onValueChange = { viewModel.updateSubjectCode(it) },
                label = { Text("Paper Code (Optional)") },
                placeholder = { Text("e.g. BCS-301") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("upload_subject_code_input"),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = formState.topicUnitCoverage,
                onValueChange = { viewModel.updateTopicUnitCoverage(it) },
                label = { Text("Units / Coverage") },
                placeholder = { Text("e.g. Units 1–3, Full") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("upload_coverage_input"),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // --- STEP 3: RESOURCE MATERIAL & TYPE ---
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "3. Resource Material & Type",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AcademicNavyPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Resource Type Chips (No asterisk)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResourceType.values().forEach { type ->
                FilterChip(
                    selected = formState.resourceType == type,
                    onClick = { viewModel.updateResourceType(type) },
                    label = { Text(type.badgeLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AcademicNavyPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("upload_type_${type.name.lowercase()}")
                )
            }
        }

        if (formState.resourceType == ResourceType.PYQ) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = formState.examYear,
                onValueChange = { viewModel.updateExamYear(it) },
                label = { Text("Exam Session / Year") },
                placeholder = { Text("e.g. 2023-2024 End Semester Examination") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Document File Attachment - RESTRICTED TO PDF and JPG/JPEG ONLY
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Attach Study Material",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = NaturalTextPrimary
                )
                Text(
                    text = "Supported file formats: PDF (.pdf) and JPG/JPEG (.jpg, .jpeg) only",
                    style = MaterialTheme.typography.bodySmall,
                    color = NaturalTextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (formState.attachedFileName != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AcademicNavyContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = AcademicNavyPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = formState.attachedFileName ?: "Document",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = NaturalTextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${formState.attachedFileSize ?: "Ready"} • Ready for upload",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NaturalTextSecondary
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.clearAttachedFile() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove File", tint = NaturalTextSecondary)
                                }
                            }

                            if (formState.isCompressing) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AcademicNavyPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Optimizing and compressing file (WhatsApp-style)...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AcademicNavyPrimary
                                    )
                                }
                            } else if (formState.compressionSavingsText != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = EditorialSuccessContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EditorialSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formState.compressionSavingsText ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = EditorialSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: Select multiple JPG images in a single selection
                        Button(
                            onClick = {
                                multiImagePickerLauncher.launch(
                                    arrayOf(
                                        "image/jpeg",
                                        "image/jpg"
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("choose_multiple_images_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AcademicNavyPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Multiple JPG Images (Compiles to PDF)")
                        }

                        // Option 2: Select single PDF or document from Device
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "image/jpeg",
                                        "image/jpg"
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("choose_file_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, tint = AcademicNavyPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Single PDF Document or Photo", color = AcademicNavyPrimary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Contributor Identity: Uploader Name - REQUIRED (*)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = formState.contributorName,
                onValueChange = { viewModel.updateContributorName(it) },
                label = {
                    Row {
                        Text("Uploader Name")
                        Text(" *", color = EditorialError, fontWeight = FontWeight.Bold)
                    }
                },
                placeholder = { Text("e.g. Rahul Sharma") },
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("upload_contributor_name_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AcademicNavyPrimary,
                    focusedLabelColor = AcademicNavyPrimary
                )
            )

            OutlinedTextField(
                value = formState.contributorEmail,
                onValueChange = { viewModel.updateContributorEmail(it) },
                label = { Text("Email (Optional)") },
                placeholder = { Text("email@csjmu.ac.in") },
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Error Banner
        formState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EditorialErrorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EditorialError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = EditorialErrorText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Submit Button
        Button(
            onClick = { viewModel.submitNote() },
            enabled = !formState.isSubmitting,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AcademicNavyPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_notes_button")
        ) {
            if (formState.isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submitting to Review Queue...")
            } else {
                Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit for Admin Approval",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Submission Success Dialog
    formState.submissionSuccessId?.let {
        AlertDialog(
            onDismissRequest = { viewModel.resetSuccess() },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EditorialSuccess,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Submitted for Admin Review",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NaturalTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Thank you for contributing to the CSJMU academic repository!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaturalTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your submission will be reviewed by the admin panel. Once approved, it will be published for all students to download and read.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetSuccess()
                        onNavigateToSubmissions()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcademicNavyPrimary)
                ) {
                    Text("Check Status in My Uploads")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.resetSuccess() }) {
                    Text("Upload Another Note")
                }
            }
        )
    }
}

private fun getFileMetadata(context: Context, uri: Uri): Pair<String, String> {
    var name = "Document"
    var size = "Ready"

    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex) ?: "Document"
                }
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    val bytes = it.getLong(sizeIndex)
                    size = when {
                        bytes > 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                        bytes > 1024 -> String.format("%d KB", bytes / 1024)
                        else -> "$bytes B"
                    }
                }
            }
        }
    } catch (_: Exception) { }

    return Pair(name, size)
}
