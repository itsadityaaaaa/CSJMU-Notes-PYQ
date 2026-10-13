package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ResourceItem
import com.example.domain.model.ResourceType
import com.example.ui.components.ResourceTypeBadge
import com.example.ui.theme.HeritageWinePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailBottomSheet(
    resource: ResourceItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("resource_detail_bottom_sheet"),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResourceTypeBadge(type = resource.resourceType)

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_detail_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = resource.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    DetailRow(label = "Subject", value = resource.subjectName)
                    DetailRow(label = "Year / Semester", value = "Year ${resource.academicYear} • Semester ${resource.semester}")
                    if (resource.examYear != null) {
                        DetailRow(label = "Exam Session", value = resource.examYear)
                    }
                    DetailRow(label = "Contributor", value = resource.contributorName)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Description & Coverage",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Attached document preview card & inline visual thumbnail
            val driveFileId = resource.driveFileId ?: com.example.util.DocumentViewerUtil.extractDriveFileId(resource.driveFileUrl ?: resource.fileUri)
            val isImage = (resource.fileMimeType?.startsWith("image/") == true) ||
                    (resource.fileName?.lowercase()?.let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") } == true)
            val directImageUrl = com.example.util.DocumentViewerUtil.getDirectImageUrl(resource.driveFileUrl ?: resource.fileUri, driveFileId)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(HeritageWinePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = resource.fileName ?: "Academic_Resource.pdf",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1
                            )
                            Text(
                                text = "${resource.fileSizeFormatted ?: "Cloud Document"} • Tap to open",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // If it's an uploaded image note, render high-res thumbnail preview
                    if (isImage && !directImageUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            coil.compose.AsyncImage(
                                model = directImageUrl,
                                contentDescription = resource.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Open/Preview, Download to Device, and Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        com.example.util.DocumentViewerUtil.openResourceDocument(context, resource)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_document_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HeritageWinePrimary)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open / View", maxLines = 1)
                }

                androidx.compose.material3.FilledTonalButton(
                    onClick = {
                        com.example.util.DocumentViewerUtil.downloadResourceDocument(context, resource)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("download_document_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download", maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        shareResource(context, resource)
                    },
                    modifier = Modifier.testTag("share_document_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

private fun shareResource(context: Context, resource: ResourceItem) {
    val cloudLink = com.example.util.DocumentViewerUtil.getPreviewUrl(resource.driveFileUrl ?: resource.fileUri, resource.driveFileId)
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        val shareText = buildString {
            append("CSJMU Notes & PYQ:\n")
            append("${resource.title}\n")
            append("Subject: ${resource.subjectName}\n")
            append("Course: ${resource.courseId} (Sem ${resource.semester})\n")
            if (!cloudLink.isNullOrBlank()) {
                append("\nView Document:\n$cloudLink")
            }
        }
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share CSJMU Resource"))
}
