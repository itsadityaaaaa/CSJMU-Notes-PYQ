package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NavigationTab

const val PRIVACY_POLICY_URL = "https://itsadityaaaaa.github.io/csjmu-notes-privacy-policy/"

@Composable
fun AcademicDrawerContent(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    pendingSubmissionsCount: Int,
    isAdminUnlocked: Boolean,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .testTag("academic_sidebar_drawer"),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Sidebar Header with University Branding
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "CSJMU Crest",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Sidebar",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CSJMU Notes & PYQ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "University Institute of Engineering & Technology (UIET)\nCSJM University, Kanpur",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Student Academic Portal • v1.0.0",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Sections
            Text(
                text = "ACADEMIC SECTIONS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            // 1. Explore Catalog
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                label = { Text("Explore Notes & PYQs", fontWeight = FontWeight.SemiBold) },
                selected = currentTab == NavigationTab.EXPLORE,
                onClick = {
                    onTabSelected(NavigationTab.EXPLORE)
                    onCloseDrawer()
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_explore")
            )

            // 2. Upload Notes
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                label = { Text("Upload Study Material", fontWeight = FontWeight.SemiBold) },
                selected = currentTab == NavigationTab.UPLOAD,
                onClick = {
                    onTabSelected(NavigationTab.UPLOAD)
                    onCloseDrawer()
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_upload")
            )

            // 3. Submissions
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                label = { Text("My Submissions & Status", fontWeight = FontWeight.SemiBold) },
                selected = currentTab == NavigationTab.SUBMISSIONS,
                onClick = {
                    onTabSelected(NavigationTab.SUBMISSIONS)
                    onCloseDrawer()
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_submissions")
            )

            // 4. Admin Portal
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                label = { Text("Admin Moderation", fontWeight = FontWeight.SemiBold) },
                badge = {
                    if (isAdminUnlocked && pendingSubmissionsCount > 0) {
                        Badge { Text(pendingSubmissionsCount.toString()) }
                    }
                },
                selected = currentTab == NavigationTab.ADMIN,
                onClick = {
                    onTabSelected(NavigationTab.ADMIN)
                    onCloseDrawer()
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_admin")
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(12.dp))

            // Information & Privacy Section
            Text(
                text = "ABOUT & LEGAL",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            // About Item
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                label = { Text("About CSJMU Notes") },
                selected = false,
                onClick = {
                    showAboutDialog = true
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_about")
            )

            // Privacy Policy Item (Opens in-app dialog + link)
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Security, contentDescription = null) },
                label = { Text("Privacy Policy") },
                selected = false,
                onClick = {
                    showPrivacyDialog = true
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_privacy")
            )

            // Direct Link to Online Privacy Policy
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Online Privacy Policy")
                    }
                },
                selected = false,
                onClick = {
                    uriHandler.openUri(PRIVACY_POLICY_URL)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_online_policy")
            )

            // Theme Toggle Item
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null
                    )
                },
                label = {
                    Text(if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode")
                },
                selected = false,
                onClick = onToggleTheme,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_theme")
            )

            Spacer(modifier = Modifier.weight(1f))

            // Drawer Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "CSJMU Notes & PYQ v1.0.0",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Crafted for students of CSJMU & UIET Kanpur",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    // About Dialog
    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false },
            onOpenOnlinePolicy = { uriHandler.openUri(PRIVACY_POLICY_URL) }
        )
    }
}

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CSJMU Notes & PYQ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Version 1.0.0 (Google Play Edition)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "About CSJMU Academic Hub",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "CSJMU Notes & PYQ is a dedicated academic resource platform built to support the students of Chhatrapati Shahu Ji Maharaj University (CSJMU), Kanpur, and the University Institute of Engineering & Technology (UIET).\n\n" +
                            "It simplifies semester exam preparations by organizing lecture notes, subject-wise previous year question papers (PYQs), course syllabi, and lab manuals by college campus, degree course, academic year, and semester.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Key Features",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Instant access to B.Tech, BCA, MCA, B.Sc, M.Sc, B.Com, and MBA curriculum\n" +
                            "• Download question papers and notes for offline reading\n" +
                            "• Student contribution workflow with smart image-to-PDF compression\n" +
                            "• Admin moderation to ensure verified and relevant study materials",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Developer & Contact",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Created by Aditya for the CSJMU student community.\nFor academic queries or syllabus updates, reach out to community administration.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit,
    onOpenOnlinePolicy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "CSJMU Notes & PYQ • Data Safety",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Highlighted Card with Online Link
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Official Privacy Policy Webpage",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = PRIVACY_POLICY_URL,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onOpenOnlinePolicy() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onOpenOnlinePolicy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Browser", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "1. Information We Collect",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Anonymous Usage Telemetry: We use Google Firebase Analytics to track application usage, active student sessions, popular courses, and download frequency. This data is strictly aggregated and does not personally identify individual users.\n" +
                            "• User Submissions: When you voluntarily upload study notes or question papers, we collect your provided contributor name (for public attribution) and optional contact email (used solely for verification or administrative notification).\n" +
                            "• Uploaded Content: The academic documents (PDFs, JPG notes) uploaded by users are stored securely in Google Drive infrastructure for educational sharing.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "2. Device Permissions & Safety",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Zero Invasive Permissions: The app does NOT request access to contacts, precise device location, microphone, or external storage scanning.\n" +
                            "• Secure File Picker: File selection operates entirely via Android's native zero-permission Photo Picker and Storage Access Framework (SAF).",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "3. Third-Party Services",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Google Firebase Analytics & Firestore (by Google LLC)\n" +
                            "• Google Apps Script & Google Drive (by Google LLC)\n" +
                            "No personal data or academic submissions are sold to third parties, data brokers, or advertising networks.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "4. Academic Takedown & Copyright Notice",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "If you believe any content on CSJMU Notes violates your copyright or educational guidelines, you can request immediate removal via the admin moderation portal or by contacting administrator at vaditya094@gmail.com.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onOpenOnlinePolicy) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Visit Web Page")
            }
        }
    )
}
