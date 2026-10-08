package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.data.auth.FirebaseAuthManager
import com.example.data.local.AppDatabase
import com.example.data.remote.RemoteResourceDataSourceImpl
import com.example.data.repository.ResourceRepositoryImpl
import com.example.data.repository.SubmissionRepositoryImpl
import com.example.domain.usecase.GetResourcesUseCase
import com.example.domain.usecase.ModerateSubmissionUseCase
import com.example.domain.usecase.SearchResourcesUseCase
import com.example.domain.usecase.SubmitNoteUseCase
import com.example.ui.components.AcademicHeader
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.SubmissionsScreen
import com.example.ui.screens.UploadScreen
import com.example.ui.theme.HeritageBrassSecondary
import com.example.ui.theme.HeritageWinePrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.ResourceViewModel
import com.example.ui.viewmodel.UploadViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    EXPLORE("Explore", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_explore"),
    UPLOAD("Upload Notes", Icons.Filled.UploadFile, Icons.Outlined.UploadFile, "nav_upload"),
    SUBMISSIONS("My Status", Icons.Filled.Assignment, Icons.Outlined.Assignment, "nav_submissions"),
    ADMIN("Admin Panel", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_admin")
}

class MainActivity : ComponentActivity() {

    private lateinit var authManager: FirebaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Local Database & Remote Firestore Data Source
        val database = AppDatabase.getInstance(applicationContext)
        val resourceDao = database.resourceDao()
        val submissionDao = database.submissionDao()
        val remoteDataSource = RemoteResourceDataSourceImpl.create(applicationContext)

        // 2. Authentication Manager (Google Sign-In via Credential Manager)
        authManager = FirebaseAuthManager(applicationContext, lifecycleScope)
        authManager.attemptAutoSignIn()

        // 3. Cloud-Synchronized Repositories
        val resourceRepository = ResourceRepositoryImpl(resourceDao, remoteDataSource, lifecycleScope)
        val submissionRepository = SubmissionRepositoryImpl(submissionDao, resourceDao, remoteDataSource, lifecycleScope, applicationContext)

        // 4. Clean Architecture Use Cases
        val getResourcesUseCase = GetResourcesUseCase(resourceRepository)
        val searchResourcesUseCase = SearchResourcesUseCase(resourceRepository)
        val submitNoteUseCase = SubmitNoteUseCase(submissionRepository)
        val moderateSubmissionUseCase = ModerateSubmissionUseCase(submissionRepository)

        // 5. Presentation Layer ViewModels
        val resourceViewModel: ResourceViewModel by viewModels {
            ResourceViewModel.Factory(getResourcesUseCase, searchResourcesUseCase)
        }

        val uploadViewModel: UploadViewModel by viewModels {
            UploadViewModel.Factory(submitNoteUseCase)
        }

        val adminViewModel: AdminViewModel by viewModels {
            AdminViewModel.Factory(submissionRepository, moderateSubmissionUseCase)
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainApp(
                    activity = this@MainActivity,
                    authManager = authManager,
                    resourceViewModel = resourceViewModel,
                    uploadViewModel = uploadViewModel,
                    adminViewModel = adminViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@Composable
fun MainApp(
    activity: ComponentActivity,
    authManager: FirebaseAuthManager,
    resourceViewModel: ResourceViewModel,
    uploadViewModel: UploadViewModel,
    adminViewModel: AdminViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var currentTab by remember { mutableStateOf(NavigationTab.EXPLORE) }
    val allSubmissions by adminViewModel.allSubmissions.collectAsState()
    val pendingCount = allSubmissions.count { it.status == com.example.domain.model.ApprovalStatus.PENDING }
    val currentUser by authManager.currentUser.collectAsState()

    var showAuthDialog by remember { mutableStateOf(false) }

    // Pre-fill student info in UploadScreen if logged in
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            val name = user.displayName ?: ""
            val email = user.email ?: ""
            if (name.isNotBlank()) uploadViewModel.updateContributorName(name)
            if (email.isNotBlank()) uploadViewModel.updateContributorEmail(email)
        }
    }

    // Android back navigation: Return to Explore if currently on another tab
    BackHandler(enabled = currentTab != NavigationTab.EXPLORE) {
        currentTab = NavigationTab.EXPLORE
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("csjmu_main_scaffold"),
        topBar = {
            AcademicHeader(
                userDisplayName = currentUser?.displayName ?: currentUser?.email,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onAuthClick = { showAuthDialog = true },
                onAdminClick = { currentTab = NavigationTab.ADMIN }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == NavigationTab.ADMIN && pendingCount > 0) {
                                BadgedBox(badge = { Badge { Text(pendingCount.toString()) } }) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    NavigationTab.EXPLORE -> {
                        ExploreScreen(
                            viewModel = resourceViewModel,
                            onNavigateToUpload = { currentTab = NavigationTab.UPLOAD }
                        )
                    }
                    NavigationTab.UPLOAD -> {
                        UploadScreen(
                            viewModel = uploadViewModel,
                            onNavigateToAdmin = { currentTab = NavigationTab.ADMIN },
                            onNavigateToSubmissions = { currentTab = NavigationTab.SUBMISSIONS }
                        )
                    }
                    NavigationTab.SUBMISSIONS -> {
                        SubmissionsScreen(
                            adminViewModel = adminViewModel,
                            onNavigateToUpload = { currentTab = NavigationTab.UPLOAD }
                        )
                    }
                    NavigationTab.ADMIN -> {
                        AdminScreen(
                            viewModel = adminViewModel,
                            onNavigateToExplore = { currentTab = NavigationTab.EXPLORE }
                        )
                    }
                }
            }
        }
    }

    // Google Sign-In & User Account Profile Dialog
    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = HeritageWinePrimary
                )
            },
            title = {
                Text(
                    text = if (currentUser != null) "Student / Contributor Account" else "Sign In with Google",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    if (currentUser != null) {
                        Text(text = "Signed in as: ${currentUser?.displayName ?: "CSJMU Student"}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your uploads are linked to this Google account for verification and cloud synchronization.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "Connect your Google account via Jetpack Credential Manager to sync your notes to the Firebase Cloud Database and track moderation approvals across devices."
                        )
                    }
                }
            },
            confirmButton = {
                if (currentUser == null) {
                    Button(
                        onClick = {
                            showAuthDialog = false
                            authManager.signInWithGoogle(
                                activity = activity,
                                onSuccess = { user ->
                                    Toast.makeText(activity, "Signed in as ${user.displayName}", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(activity, "Sign in failed: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HeritageWinePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sign In with Google")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            showAuthDialog = false
                            authManager.signOut {
                                Toast.makeText(activity, "Signed out", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sign Out")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAuthDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }
}
