package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AcademicCatalog
import com.example.domain.model.AcademicYear
import com.example.domain.model.College
import com.example.domain.model.Course
import com.example.domain.model.ResourceItem
import com.example.domain.model.ResourceType
import com.example.domain.model.Subject
import com.example.domain.repository.IResourceRepository
import com.example.domain.usecase.GetResourcesUseCase
import com.example.domain.usecase.SearchResourcesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ResourceFilterState(
    val selectedCollege: College = AcademicCatalog.COLLEGES.first(),
    val selectedCourse: Course = AcademicCatalog.COURSES.first(),
    val selectedYear: AcademicYear = AcademicCatalog.getYearsForCourse(AcademicCatalog.COURSES.first()).first(),
    val selectedSemester: Int? = null,
    val selectedSubject: Subject? = null,
    val selectedResourceType: ResourceType? = null,
    val searchQuery: String = ""
)

class ResourceViewModel(
    private val getResourcesUseCase: GetResourcesUseCase,
    private val searchResourcesUseCase: SearchResourcesUseCase
) : ViewModel() {

    private val _filterState = MutableStateFlow(ResourceFilterState())
    val filterState: StateFlow<ResourceFilterState> = _filterState.asStateFlow()

    private val _selectedResourceDetail = MutableStateFlow<ResourceItem?>(null)
    val selectedResourceDetail: StateFlow<ResourceItem?> = _selectedResourceDetail.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val state = _filterState.value
                getResourcesUseCase.refresh(state.selectedCollege.id, state.selectedCourse.id)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val resources: StateFlow<List<ResourceItem>> = _filterState
        .flatMapLatest { filter ->
            if (filter.searchQuery.isNotBlank()) {
                searchResourcesUseCase(
                    query = filter.searchQuery.trim(),
                    collegeId = null,
                    courseId = null
                )
            } else {
                getResourcesUseCase(
                    collegeId = filter.selectedCollege.id,
                    courseId = filter.selectedCourse.id,
                    academicYear = filter.selectedYear.yearNumber,
                    semester = filter.selectedSemester,
                    resourceType = filter.selectedResourceType
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectCollege(college: College) {
        val availableCourses = AcademicCatalog.getCoursesForCollege(college.id)
        val defaultCourse = availableCourses.firstOrNull() ?: AcademicCatalog.COURSES.first()
        val defaultYears = AcademicCatalog.getYearsForCourse(defaultCourse)
        val defaultYear = defaultYears.firstOrNull() ?: AcademicYear(1, "1st Year", listOf(1, 2))

        _filterState.value = _filterState.value.copy(
            selectedCollege = college,
            selectedCourse = defaultCourse,
            selectedYear = defaultYear,
            selectedSemester = null,
            selectedSubject = null
        )
    }

    fun selectCourse(course: Course) {
        val defaultYears = AcademicCatalog.getYearsForCourse(course)
        val defaultYear = defaultYears.firstOrNull() ?: AcademicYear(1, "1st Year", listOf(1, 2))

        _filterState.value = _filterState.value.copy(
            selectedCourse = course,
            selectedYear = defaultYear,
            selectedSemester = null,
            selectedSubject = null
        )
    }

    fun selectYear(year: AcademicYear) {
        _filterState.value = _filterState.value.copy(
            selectedYear = year,
            selectedSemester = null,
            selectedSubject = null
        )
    }

    fun selectSemester(semester: Int?) {
        _filterState.value = _filterState.value.copy(
            selectedSemester = semester,
            selectedSubject = null
        )
    }

    fun selectSubject(subject: Subject?) {
        _filterState.value = _filterState.value.copy(selectedSubject = subject)
    }

    fun selectResourceType(resourceType: ResourceType?) {
        _filterState.value = _filterState.value.copy(selectedResourceType = resourceType)
    }

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun openDetail(item: ResourceItem) {
        _selectedResourceDetail.value = item
    }

    fun closeDetail() {
        _selectedResourceDetail.value = null
    }

    class Factory(
        private val getResourcesUseCase: GetResourcesUseCase,
        private val searchResourcesUseCase: SearchResourcesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ResourceViewModel(getResourcesUseCase, searchResourcesUseCase) as T
        }
    }
}
