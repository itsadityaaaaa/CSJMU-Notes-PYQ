package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AcademicCatalog
import com.example.domain.model.College
import com.example.domain.model.Course
import com.example.domain.model.ResourceType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ResourceCard
import com.example.ui.theme.HeritageWinePrimary
import com.example.ui.viewmodel.ResourceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ResourceViewModel,
    onNavigateToUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filterState by viewModel.filterState.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val selectedDetail by viewModel.selectedResourceDetail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showCollegeDropdown by remember { mutableStateOf(false) }
    var showCourseDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }

    val availableCourses = AcademicCatalog.getCoursesForCollege(filterState.selectedCollege.id)
    val availableYears = AcademicCatalog.getYearsForCourse(filterState.selectedCourse)

    // Synchronize catalog from cloud on load
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen")
    ) {
        // Selector Section (College, Course, Year)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header with refresh button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CSJMU Notes & PYQ Repository",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.testTag("refresh_explore_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = HeritageWinePrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh from Cloud",
                                tint = HeritageWinePrimary
                            )
                        }
                    }
                }

                // College Dropdown Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { showCollegeDropdown = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("college_selector_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Campus / College",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = filterState.selectedCollege.shortCode,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select College")
                        }
                    }

                    DropdownMenu(
                        expanded = showCollegeDropdown,
                        onDismissRequest = { showCollegeDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        AcademicCatalog.COLLEGES.forEach { college ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = college.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = college.location,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectCollege(college)
                                    showCollegeDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Course Selector Button
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { showCourseDropdown = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("course_selector_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Academic Course",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = filterState.selectedCourse.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select Course")
                        }
                    }

                    DropdownMenu(
                        expanded = showCourseDropdown,
                        onDismissRequest = { showCourseDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        availableCourses.forEach { course ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = course.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "${course.code} • ${course.totalYears} Years",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectCourse(course)
                                    showCourseDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Academic Year Dropdown Picker (100% responsive, full 4-year support)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { showYearDropdown = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("year_selector_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Academic Year",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = filterState.selectedYear.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select Academic Year")
                        }
                    }

                    DropdownMenu(
                        expanded = showYearDropdown,
                        onDismissRequest = { showYearDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        availableYears.forEach { year ->
                            val isSelected = filterState.selectedYear.yearNumber == year.yearNumber
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = year.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.selectYear(year)
                                    showYearDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Reduced Compact One-Line Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search notes, topics or PYQs...",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_resources_input")
            )
        }

        // Resource Type Tabs (All, Notes, PYQ, Syllabus)
        val tabTypes = listOf(null, ResourceType.NOTES, ResourceType.PYQ, ResourceType.SYLLABUS)
        val selectedTabIndex = tabTypes.indexOf(filterState.selectedResourceType)

        ScrollableTabRow(
            selectedTabIndex = if (selectedTabIndex >= 0) selectedTabIndex else 0,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTypes.forEachIndexed { index, type ->
                Tab(
                    selected = (filterState.selectedResourceType == type),
                    onClick = { viewModel.selectResourceType(type) },
                    text = {
                        Text(
                            text = type?.displayName ?: "All Resources",
                            fontWeight = if (filterState.selectedResourceType == type) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("tab_${type?.name?.lowercase() ?: "all"}")
                )
            }
        }

        // Resources List or Empty State
        if (resources.isEmpty()) {
            EmptyStateView(
                title = "No Verified Resources Found",
                message = "No notes or previous year papers have been published yet for ${filterState.selectedCourse.code} (Year ${filterState.selectedYear.yearNumber}).\n\nUpload your class notes or PYQ now to contribute! It will be reviewed by admin before publication.",
                icon = Icons.Default.MenuBook,
                actionButtonText = "Upload Notes or PYQ",
                onActionClick = onNavigateToUpload,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Available Resources (${resources.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(resources, key = { it.id }) { resource ->
                    ResourceCard(
                        resource = resource,
                        onClick = { viewModel.openDetail(resource) }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet when an item is selected
    selectedDetail?.let { resource ->
        ResourceDetailBottomSheet(
            resource = resource,
            onDismiss = { viewModel.closeDetail() }
        )
    }
}
