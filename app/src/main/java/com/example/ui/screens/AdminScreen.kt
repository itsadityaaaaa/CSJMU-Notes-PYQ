package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.ResourceType
import com.example.domain.model.Submission
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ResourceTypeBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AcademicNavyContainer
import com.example.ui.theme.AcademicNavyDark
import com.example.ui.theme.AcademicNavyPrimary
import com.example.ui.theme.EditorialError
import com.example.ui.theme.EditorialErrorContainer
import com.example.ui.theme.EditorialErrorText
import com.example.ui.theme.EditorialSuccess
import com.example.ui.theme.EditorialSuccessContainer
import com.example.ui.theme.EditorialWarningContainer
import com.example.ui.theme.EditorialWarningText
import com.example.ui.theme.NaturalBackground
import com.example.ui.theme.NaturalCardSurface
import com.example.ui.theme.NaturalOutline
import com.example.ui.theme.NaturalSurfaceVariant
import com.example.ui.theme.NaturalTextMuted
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onNavigateToExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allSubmissions by viewModel.allSubmissions.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val context = LocalContext.current

    // Sync cloud queue when entering Admin screen
    LaunchedEffect(uiState.isAdminUnlocked) {
        if (uiState.isAdminUnlocked) {
            viewModel.refreshSubmissions()
        }
    }

    // Feedback message toast
    LaunchedEffect(uiState.actionSuccessMessage) {
        uiState.actionSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(uiState.actionErrorMessage) {
        uiState.actionErrorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.dismissMessage()
        }
    }

    if (!uiState.isAdminUnlocked) {
        // --- RESTRICTED ADMIN AUTHENTICATION GATE ---
        AdminAuthenticationGate(
            adminIdInput = uiState.adminIdInput,
            lockoutSeconds = uiState.lockoutSecondsRemaining,
            errorMessage = uiState.authErrorMessage,
            onInputChange = { viewModel.updateAdminIdInput(it) },
            onAuthenticate = { viewModel.verifyAdminId() },
            modifier = modifier
        )
    } else {
        // --- AUTHENTICATED ADMIN MODERATION PANEL ---
        val pendingCount = allSubmissions.count { it.status == ApprovalStatus.PENDING }
        val approvedCount = allSubmissions.count { it.status == ApprovalStatus.APPROVED }
        val rejectedCount = allSubmissions.count { it.status == ApprovalStatus.REJECTED }

        val filteredList = when (uiState.selectedFilter) {
            null -> allSubmissions
            else -> allSubmissions.filter { it.status == uiState.selectedFilter }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("admin_screen")
        ) {
            // Top Navigation & KPI Summary
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CSJMU Moderation Panel",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Authenticated Session • Edit & Approve",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sync from Cloud button
                            IconButton(
                                onClick = { viewModel.refreshSubmissions() },
                                modifier = Modifier.testTag("admin_refresh_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync Cloud Submissions",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            // Lock / Sign Out button
                            OutlinedButton(
                                onClick = { viewModel.lockAdminSession() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("lock_admin_button")
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Lock", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metrics Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminMetricChip(
                            label = "Pending",
                            count = pendingCount,
                            isSelected = uiState.selectedFilter == ApprovalStatus.PENDING,
                            onClick = { viewModel.selectFilter(ApprovalStatus.PENDING) },
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricChip(
                            label = "Published",
                            count = approvedCount,
                            isSelected = uiState.selectedFilter == ApprovalStatus.APPROVED,
                            onClick = { viewModel.selectFilter(ApprovalStatus.APPROVED) },
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricChip(
                            label = "Rejected",
                            count = rejectedCount,
                            isSelected = uiState.selectedFilter == ApprovalStatus.REJECTED,
                            onClick = { viewModel.selectFilter(ApprovalStatus.REJECTED) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Submissions List or Empty State
            if (filteredList.isEmpty()) {
                val emptyTitle = when (uiState.selectedFilter) {
                    ApprovalStatus.PENDING -> "No Pending Submissions"
                    ApprovalStatus.APPROVED -> "No Published Notes Yet"
                    ApprovalStatus.REJECTED -> "No Rejected Submissions"
                    null -> "No Submissions Recorded"
                }
                val emptyMsg = when (uiState.selectedFilter) {
                    ApprovalStatus.PENDING -> "All student notes and question papers have been reviewed."
                    else -> "No materials match this filter."
                }

                EmptyStateView(
                    title = emptyTitle,
                    message = emptyMsg,
                    icon = Icons.Default.AdminPanelSettings,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { submission ->
                        AdminSubmissionCard(
                            submission = submission,
                            onReviewClick = { viewModel.openReview(submission) },
                            onEditClick = { viewModel.openEdit(submission) }
                        )
                    }
                }
            }
        }

        // Moderation Sheet
        uiState.selectedSubmissionForReview?.let { submission ->
            ModerationReviewBottomSheet(
                submission = submission,
                isProcessing = uiState.isProcessing,
                onDismiss = { viewModel.closeReview() },
                onEditAllFields = {
                    viewModel.closeReview()
                    viewModel.openEdit(submission)
                },
                onApprove = { notes -> viewModel.approveSubmission(submission, notes) },
                onReject = { reason -> viewModel.rejectSubmission(submission.id, reason) }
            )
        }

        // Full Admin Edit Submission Sheet (Can edit description and ALL other fields)
        uiState.selectedSubmissionForEdit?.let { submissionToEdit ->
            AdminEditSubmissionBottomSheet(
                submission = submissionToEdit,
                isProcessing = uiState.isProcessing,
                onDismiss = { viewModel.closeEdit() },
                onSaveEdits = { updated, approveNow, adminNotes ->
                    viewModel.saveEdits(updated, approveNow, adminNotes)
                }
            )
        }
    }
}

/**
 * Access Control Challenge Screen with 16-letter ID requirement and 30-second penalty lockout.
 */
@Composable
fun AdminAuthenticationGate(
    adminIdInput: String,
    lockoutSeconds: Int,
    errorMessage: String?,
    onInputChange: (String) -> Unit,
    onAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isLocked = lockoutSeconds > 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("admin_auth_gate"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Security Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isLocked) EditorialErrorContainer else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Timer else Icons.Default.Security,
                        contentDescription = "Admin Security Shield",
                        tint = if (isLocked) EditorialError else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Restricted Admin Portal",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Authorized CSJMU Academic Administrators & Faculty Only. Enter the 16-letter Master Admin ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Penalty Lockout Notification Banner
                if (isLocked) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EditorialErrorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("lockout_penalty_banner")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = EditorialError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Access Locked: Incorrect ID",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = EditorialError
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Security penalty enforced. Please wait $lockoutSeconds seconds before re-attempting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialErrorText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { lockoutSeconds / 30f },
                                modifier = Modifier.fillMaxWidth(),
                                color = EditorialError,
                                trackColor = EditorialError.copy(alpha = 0.2f)
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EditorialWarningContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialWarningText
                        )
                    }
                }

                // Admin ID Input Field
                OutlinedTextField(
                    value = adminIdInput,
                    onValueChange = onInputChange,
                    enabled = !isLocked,
                    label = { Text("16-Letter Admin ID") },
                    placeholder = { Text("Enter Admin ID") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = !isLocked
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle ID Visibility"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!isLocked) onAuthenticate() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_id_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AcademicNavyPrimary,
                        focusedLabelColor = AcademicNavyPrimary
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Unlock Button
                Button(
                    onClick = onAuthenticate,
                    enabled = !isLocked && adminIdInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcademicNavyPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_authenticate_button")
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLocked) "Locked (${lockoutSeconds}s)" else "Authenticate & Access",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Strict security: 30-second lockout applied after every incorrect entry.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaturalTextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AdminMetricChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun AdminSubmissionCard(
    submission: Submission,
    onReviewClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_item_${submission.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = NaturalCardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResourceTypeBadge(type = submission.resourceType)
                StatusBadge(status = submission.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = submission.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${submission.courseName} • Year ${submission.academicYear} (Sem ${submission.semester})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Subject: ${submission.subjectName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (submission.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = submission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "By ${submission.contributorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Edit Note Button
                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("edit_button_${submission.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Note", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    // Review Button
                    Button(
                        onClick = onReviewClick,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("review_button_${submission.id}")
                    ) {
                        Text(
                            text = if (submission.status == ApprovalStatus.PENDING) "Review" else "Details",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Moderation Review BottomSheet for quick review, approval or rejection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationReviewBottomSheet(
    submission: Submission,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onEditAllFields: () -> Unit,
    onApprove: (adminNotes: String) -> Unit,
    onReject: (reason: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var adminNotes by remember {
        mutableStateOf("Verified syllabus alignment & clarity. Approved for CSJMU catalog.")
    }
    var rejectionReason by remember {
        mutableStateOf("Incomplete syllabus coverage or unreadable document scan.")
    }
    var showRejectDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("moderation_review_sheet"),
        containerColor = NaturalCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResourceTypeBadge(type = submission.resourceType)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = submission.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = NaturalTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Curriculum details box
            Card(
                colors = CardDefaults.cardColors(containerColor = NaturalSurfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Curriculum Metadata",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = AcademicNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Course: ${submission.courseName}", style = MaterialTheme.typography.bodySmall, color = NaturalTextPrimary)
                    Text(text = "Subject: ${submission.subjectName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = NaturalTextPrimary)
                    Text(text = "Year / Sem: Year ${submission.academicYear} • Sem ${submission.semester}", style = MaterialTheme.typography.bodySmall, color = NaturalTextPrimary)
                    if (submission.examYear != null) {
                        Text(text = "Exam Session: ${submission.examYear}", style = MaterialTheme.typography.bodySmall, color = NaturalTextPrimary)
                    }
                    Text(text = "Contributor: ${submission.contributorName} (${submission.contributorEmail ?: "No email provided"})", style = MaterialTheme.typography.bodySmall, color = NaturalTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes Description",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = NaturalTextPrimary
                )

                // Edit Button
                OutlinedButton(
                    onClick = onEditAllFields,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = AcademicNavyPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Description & Fields", style = MaterialTheme.typography.labelSmall, color = AcademicNavyPrimary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = NaturalSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = submission.description.ifBlank { "No description provided." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaturalTextPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // File Attachment Card with Preview / Open action
            val targetPreviewUrl = submission.driveFileUrl ?: submission.fileUri
            Card(
                colors = CardDefaults.cardColors(containerColor = AcademicNavyContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!targetPreviewUrl.isNullOrBlank()) {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetPreviewUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, "Cannot open PDF preview", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = AcademicNavyPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = submission.fileName ?: "Academic_Document.pdf",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NaturalTextPrimary
                        )
                        Text(
                            text = "${submission.fileSizeFormatted ?: "Ready"} • Tap to Preview / Open in Drive",
                            style = MaterialTheme.typography.bodySmall,
                            color = NaturalTextSecondary
                        )
                    }
                    if (!targetPreviewUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetPreviewUrl))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }.onFailure {
                                    Toast.makeText(context, "Cannot open PDF preview", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Preview PDF",
                                tint = AcademicNavyPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin Review Notes field
            OutlinedTextField(
                value = adminNotes,
                onValueChange = { adminNotes = it },
                label = { Text("Admin Feedback / Review Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_notes_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Approval Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showRejectDialog = true },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EditorialError),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_reject_button")
                ) {
                    Icon(imageVector = Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reject")
                }

                Button(
                    onClick = { onApprove(adminNotes) },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = EditorialSuccess),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("admin_approve_button")
                ) {
                    Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Approve & Publish",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Rejection Confirmation Dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Submission") },
            text = {
                Column {
                    Text("Please provide the reason for rejection to send to the student:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        onReject(rejectionReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorialError)
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Comprehensive Admin Edit Sheet: Allows the administrator to edit the description
 * and ALL other fields of the uploaded notes prior to or following approval.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditSubmissionBottomSheet(
    submission: Submission,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onSaveEdits: (updated: Submission, approveNow: Boolean, adminNotes: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Editable state variables
    var title by remember { mutableStateOf(submission.title) }
    var description by remember { mutableStateOf(submission.description) }
    var subjectName by remember { mutableStateOf(submission.subjectName) }
    var courseName by remember { mutableStateOf(submission.courseName) }
    var academicYear by remember { mutableIntStateOf(submission.academicYear) }
    var semester by remember { mutableIntStateOf(submission.semester) }
    var resourceType by remember { mutableStateOf(submission.resourceType) }
    var examYear by remember { mutableStateOf(submission.examYear ?: "") }
    var contributorName by remember { mutableStateOf(submission.contributorName) }
    var contributorEmail by remember { mutableStateOf(submission.contributorEmail ?: "") }
    var adminFeedback by remember { mutableStateOf(submission.adminFeedback ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("admin_edit_submission_sheet"),
        containerColor = NaturalCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AcademicNavyContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = AcademicNavyPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Edit Note Details & Fields",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = NaturalTextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NaturalTextSecondary)
                }
            }

            Text(
                text = "Modify description, syllabus coverage, title, or curriculum information as an admin.",
                style = MaterialTheme.typography.bodySmall,
                color = NaturalTextSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            // 1. Note Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Note Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_title_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Note Description (Primary focus!)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Uploaded Note Description & Coverage") },
                placeholder = { Text("Curate description, topics covered, units breakdown, etc.") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_description_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Subject Name
            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("Subject Name / Paper") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_subject_name_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Course Name
            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it },
                label = { Text("Degree Course") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Academic Year Selector
            Text(text = "Academic Year:", style = MaterialTheme.typography.labelMedium, color = NaturalTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 4).forEach { yr ->
                    FilterChip(
                        selected = academicYear == yr,
                        onClick = { academicYear = yr },
                        label = { Text("Year $yr") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AcademicNavyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Semester Selector
            Text(text = "Semester:", style = MaterialTheme.typography.labelMedium, color = NaturalTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 2, 3, 4, 5, 6, 7, 8).forEach { sem ->
                    FilterChip(
                        selected = semester == sem,
                        onClick = { semester = sem },
                        label = { Text("S$sem") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AcademicNavyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Resource Type
            Text(text = "Resource Material Type:", style = MaterialTheme.typography.labelMedium, color = NaturalTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ResourceType.values().forEach { type ->
                    FilterChip(
                        selected = resourceType == type,
                        onClick = { resourceType = type },
                        label = { Text(type.badgeLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AcademicNavyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Exam Year (if PYQ)
            if (resourceType == ResourceType.PYQ) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = examYear,
                    onValueChange = { examYear = it },
                    label = { Text("Exam Session / Year") },
                    placeholder = { Text("e.g. 2023-2024") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 8. Contributor Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = contributorName,
                    onValueChange = { contributorName = it },
                    label = { Text("Contributor Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = contributorEmail,
                    onValueChange = { contributorEmail = it },
                    label = { Text("Contributor Email") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 9. Admin Feedback / Notes
            OutlinedTextField(
                value = adminFeedback,
                onValueChange = { adminFeedback = it },
                label = { Text("Admin Feedback / Review Notes") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val updatedSubmission = submission.copy(
                title = title.trim(),
                description = description.trim(),
                subjectName = subjectName.trim(),
                courseName = courseName.trim(),
                academicYear = academicYear,
                semester = semester,
                resourceType = resourceType,
                examYear = examYear.trim().takeIf { it.isNotBlank() },
                contributorName = contributorName.trim(),
                contributorEmail = contributorEmail.trim().takeIf { it.isNotBlank() },
                adminFeedback = adminFeedback.trim().takeIf { it.isNotBlank() }
            )

            // Action Buttons: Save Changes OR Save & Approve
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save Changes
                OutlinedButton(
                    onClick = {
                        onSaveEdits(updatedSubmission, false, null)
                    },
                    enabled = !isProcessing && title.isNotBlank() && subjectName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_edits_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes")
                }

                // If currently pending, provide Save & Approve Now button
                if (submission.status == ApprovalStatus.PENDING) {
                    Button(
                        onClick = {
                            val notes = adminFeedback.ifBlank { "Curated and approved by CSJMU admin" }
                            onSaveEdits(updatedSubmission, true, notes)
                        },
                        enabled = !isProcessing && title.isNotBlank() && subjectName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_and_approve_button")
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Publish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
