package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.ResourceItem
import com.example.domain.model.ResourceType
import com.example.ui.theme.EditorialError
import com.example.ui.theme.EditorialErrorContainer
import com.example.ui.theme.EditorialSuccess
import com.example.ui.theme.EditorialSuccessContainer
import com.example.ui.theme.EditorialWarning
import com.example.ui.theme.EditorialWarningContainer
import com.example.ui.theme.HeritageBrassContainer
import com.example.ui.theme.HeritageBrassSecondary
import com.example.ui.theme.HeritageOnBrassContainer
import com.example.ui.theme.HeritageOnSageContainer
import com.example.ui.theme.HeritageOnWineContainer
import com.example.ui.theme.HeritageSageContainer
import com.example.ui.theme.HeritageWineContainer
import com.example.ui.theme.HeritageWineDark
import com.example.ui.theme.HeritageWinePrimary
import com.example.ui.theme.WarmOutline
import com.example.ui.theme.WarmSurfaceVariant

@Composable
fun AcademicHeader(
    modifier: Modifier = Modifier,
    userDisplayName: String? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: (() -> Unit)? = null,
    onAuthClick: (() -> Unit)? = null,
    onAdminClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("academic_header"),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "CSJMU Crest",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "CSJMU Notes & PYQ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Academic Resource Repository",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Theme Switch Button (Black & White Light / Dark toggle)
                    if (onToggleTheme != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleTheme() }
                                .testTag("theme_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Switch Theme",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isDarkTheme) "Light" else "Dark",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    if (onAuthClick != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAuthClick() }
                                .testTag("google_auth_header_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = userDisplayName?.take(10) ?: "Sign In",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    if (onAdminClick != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAdminClick() }
                                .testTag("admin_header_badge")
                        ) {
                            Text(
                                text = "Admin",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceTypeBadge(
    type: ResourceType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (type) {
        ResourceType.NOTES -> Triple(HeritageWineContainer, HeritageOnWineContainer, Icons.Default.MenuBook)
        ResourceType.PYQ -> Triple(HeritageBrassContainer, HeritageOnBrassContainer, Icons.Default.Assignment)
        ResourceType.SYLLABUS -> Triple(HeritageSageContainer, HeritageOnSageContainer, Icons.Default.Description)
        ResourceType.FORMULA_SHEET -> Triple(WarmSurfaceVariant, Color(0xFF44403C), Icons.Default.Description)
    }

    Surface(
        modifier = modifier.testTag("badge_${type.name.lowercase()}"),
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = type.badgeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: ApprovalStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        ApprovalStatus.PENDING -> Triple(EditorialWarningContainer, EditorialWarning, Icons.Default.HourglassEmpty)
        ApprovalStatus.APPROVED -> Triple(EditorialSuccessContainer, EditorialSuccess, Icons.Default.CheckCircle)
        ApprovalStatus.REJECTED -> Triple(EditorialErrorContainer, EditorialError, Icons.Default.Error)
    }

    Surface(
        modifier = modifier.testTag("status_${status.name.lowercase()}"),
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResourceCard(
    resource: ResourceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("resource_card_${resource.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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
                ResourceTypeBadge(type = resource.resourceType)

                if (resource.examYear != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = WarmSurfaceVariant
                    ) {
                        Text(
                            text = resource.examYear,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = resource.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TagChip(label = resource.subjectName)
                TagChip(label = "Sem ${resource.semester}")
                TagChip(label = "Year ${resource.academicYear}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = resource.contributorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (resource.fileSizeFormatted != null) {
                    Text(
                        text = resource.fileSizeFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = HeritageWinePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun TagChip(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = WarmSurfaceVariant,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyStateView(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.MenuBook,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("empty_state_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(WarmSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HeritageWinePrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (actionButtonText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HeritageWinePrimary),
                    modifier = Modifier.testTag("empty_state_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = actionButtonText)
                }
            }
        }
    }
}
