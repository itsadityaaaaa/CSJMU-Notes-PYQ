package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.Submission
import com.example.domain.repository.ISubmissionRepository
import com.example.domain.usecase.ModerateSubmissionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminUiState(
    val isAdminUnlocked: Boolean = false, // Restricted by default: requires 16-letter Admin ID
    val adminIdInput: String = "",
    val authErrorMessage: String? = null,
    val lockoutSecondsRemaining: Int = 0, // 30-second penalty after incorrect entry
    val selectedFilter: ApprovalStatus? = ApprovalStatus.PENDING,
    val selectedSubmissionForReview: Submission? = null,
    val selectedSubmissionForEdit: Submission? = null,
    val isProcessing: Boolean = false,
    val actionSuccessMessage: String? = null,
    val actionErrorMessage: String? = null
)

class AdminViewModel(
    private val submissionRepository: ISubmissionRepository,
    private val moderateSubmissionUseCase: ModerateSubmissionUseCase
) : ViewModel() {

    companion object {
        // Hardcoded 16-letter Admin Master Passkey
        // Letters: C-S-J-M-U-A-D-M-I-N-S-E-C-U-R-E (16 letters)
        const val MASTER_ADMIN_ID = "CSJMUADMINSECURE"
        const val LOCKOUT_DURATION_SECONDS = 30
    }

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun refreshSubmissions() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                submissionRepository.syncFromRemote()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    val allSubmissions: StateFlow<List<Submission>> = submissionRepository.getAllSubmissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = submissionRepository.getPendingSubmissions()
        .combine(MutableStateFlow(Unit)) { list, _ -> list.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun updateAdminIdInput(input: String) {
        if (_uiState.value.lockoutSecondsRemaining > 0) return
        _uiState.value = _uiState.value.copy(
            adminIdInput = input,
            authErrorMessage = null
        )
    }

    fun verifyAdminId() {
        val currentState = _uiState.value
        if (currentState.lockoutSecondsRemaining > 0) {
            return
        }

        val entered = currentState.adminIdInput.trim()
        if (entered.equals(MASTER_ADMIN_ID, ignoreCase = true)) {
            // Access Granted
            _uiState.value = currentState.copy(
                isAdminUnlocked = true,
                adminIdInput = "",
                authErrorMessage = null,
                lockoutSecondsRemaining = 0
            )
        } else {
            // Wrong ID: enforce mandatory 30-second security lockout penalty
            startLockoutTimer()
        }
    }

    private fun startLockoutTimer() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isAdminUnlocked = false,
            lockoutSecondsRemaining = LOCKOUT_DURATION_SECONDS,
            authErrorMessage = "Incorrect Admin ID. Access locked. Please wait $LOCKOUT_DURATION_SECONDS seconds."
        )

        countdownJob = viewModelScope.launch {
            for (sec in (LOCKOUT_DURATION_SECONDS - 1) downTo 0) {
                delay(1000L)
                _uiState.value = _uiState.value.copy(
                    lockoutSecondsRemaining = sec,
                    authErrorMessage = if (sec > 0) {
                        "Incorrect Admin ID. Access locked. Please wait ${sec}s before retrying."
                    } else {
                        "Lockout expired. You may now re-enter the 16-letter Admin ID."
                    }
                )
            }
        }
    }

    fun lockAdminSession() {
        _uiState.value = _uiState.value.copy(
            isAdminUnlocked = false,
            adminIdInput = "",
            authErrorMessage = null
        )
    }

    fun selectFilter(status: ApprovalStatus?) {
        _uiState.value = _uiState.value.copy(selectedFilter = status)
    }

    fun openReview(submission: Submission) {
        _uiState.value = _uiState.value.copy(
            selectedSubmissionForReview = submission,
            selectedSubmissionForEdit = null
        )
    }

    fun closeReview() {
        _uiState.value = _uiState.value.copy(selectedSubmissionForReview = null)
    }

    fun openEdit(submission: Submission) {
        _uiState.value = _uiState.value.copy(
            selectedSubmissionForEdit = submission,
            selectedSubmissionForReview = null
        )
    }

    fun closeEdit() {
        _uiState.value = _uiState.value.copy(selectedSubmissionForEdit = null)
    }

    fun approveSubmission(submission: Submission, adminNotes: String?) {
        _uiState.value = _uiState.value.copy(isProcessing = true, actionErrorMessage = null)
        viewModelScope.launch {
            val result = moderateSubmissionUseCase.approve(submission, adminNotes)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    selectedSubmissionForReview = null,
                    selectedSubmissionForEdit = null,
                    actionSuccessMessage = "Note approved and published to the course catalog!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    actionErrorMessage = result.exceptionOrNull()?.message ?: "Failed to approve note"
                )
            }
        }
    }

    fun approveSubmissionById(submissionId: String, adminNotes: String?) {
        val target = allSubmissions.value.firstOrNull { it.id == submissionId }
            ?: _uiState.value.selectedSubmissionForReview
            ?: _uiState.value.selectedSubmissionForEdit

        if (target != null) {
            approveSubmission(target, adminNotes)
        } else {
            _uiState.value = _uiState.value.copy(actionErrorMessage = "Submission not found")
        }
    }

    fun saveEdits(
        updatedSubmission: Submission,
        approveNow: Boolean = false,
        adminNotes: String? = null
    ) {
        _uiState.value = _uiState.value.copy(isProcessing = true, actionErrorMessage = null)
        viewModelScope.launch {
            if (approveNow) {
                val result = moderateSubmissionUseCase.approve(updatedSubmission, adminNotes)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        selectedSubmissionForEdit = null,
                        selectedSubmissionForReview = null,
                        actionSuccessMessage = "Note details updated, approved and published to the catalog!"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        actionErrorMessage = result.exceptionOrNull()?.message ?: "Failed to approve note"
                    )
                }
            } else {
                val result = moderateSubmissionUseCase.saveEdits(updatedSubmission)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        selectedSubmissionForEdit = null,
                        actionSuccessMessage = "Note description and fields updated successfully."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        actionErrorMessage = result.exceptionOrNull()?.message ?: "Failed to save edits"
                    )
                }
            }
        }
    }

    fun rejectSubmission(submissionId: String, reason: String) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(actionErrorMessage = "Please provide a rejection reason")
            return
        }

        _uiState.value = _uiState.value.copy(isProcessing = true, actionErrorMessage = null)
        viewModelScope.launch {
            val result = moderateSubmissionUseCase.reject(submissionId, reason)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    selectedSubmissionForReview = null,
                    selectedSubmissionForEdit = null,
                    actionSuccessMessage = "Submission rejected with feedback sent to student."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    actionErrorMessage = result.exceptionOrNull()?.message ?: "Failed to reject submission"
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(actionSuccessMessage = null, actionErrorMessage = null)
    }

    class Factory(
        private val submissionRepository: ISubmissionRepository,
        private val moderateSubmissionUseCase: ModerateSubmissionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminViewModel(submissionRepository, moderateSubmissionUseCase) as T
        }
    }
}
