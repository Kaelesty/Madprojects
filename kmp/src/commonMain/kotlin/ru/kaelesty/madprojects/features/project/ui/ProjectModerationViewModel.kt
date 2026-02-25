package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.project.ProjectStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.project.domain.ProjectInfoUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class ProjectModerationViewModel(
    private val projectId: String,
    private val useCase: ProjectInfoUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class UiState(
        val isLoadingStatus: Boolean = true,
        val projectStatus: ProjectStatus? = null,
        val statusErrorMessage: String? = null,
        val bannerActionErrorMessage: String? = null,
    )

    data class ApproveDialogState(
        val isOpen: Boolean = false,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
    )

    data class DisapproveDialogState(
        val isOpen: Boolean = false,
        val reason: String = "",
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _approveDialogState = MutableStateFlow(ApproveDialogState())
    val approveDialogState = _approveDialogState.asStateFlow()

    private val _disapproveDialogState = MutableStateFlow(DisapproveDialogState())
    val disapproveDialogState = _disapproveDialogState.asStateFlow()

    init {
        KLogger.d(TAG) { "init: load moderation status projectId=$projectId" }
        loadStatus()
    }

    fun loadStatus() {
        KLogger.d(TAG) { "loadStatus start: projectId=$projectId" }
        _uiState.update {
            it.copy(
                isLoadingStatus = true,
                statusErrorMessage = null,
            )
        }
        viewModelScope.launch {
            when (val result = useCase.loadProject(projectId)) {
                is ProjectInfoUseCase.ProjectResult.Success -> {
                    val status = result.project.status
                    KLogger.i(TAG) { "loadStatus success: status=$status" }
                    _uiState.update {
                        it.copy(
                            isLoadingStatus = false,
                            projectStatus = status,
                            statusErrorMessage = null,
                        )
                    }
                }
                is ProjectInfoUseCase.ProjectResult.Fail -> {
                    val message = result.message ?: str.LoadError
                    KLogger.w(TAG) { "loadStatus failed: status=${result.status} message=$message" }
                    _uiState.update {
                        it.copy(
                            isLoadingStatus = false,
                            statusErrorMessage = message
                        )
                    }
                }
            }
        }
    }

    fun clearBannerActionError() {
        _uiState.update { it.copy(bannerActionErrorMessage = null) }
    }

    fun openApproveDialog() {
        KLogger.d(TAG) { "openApproveDialog" }
        _approveDialogState.value = ApproveDialogState(isOpen = true)
    }

    fun closeApproveDialog() {
        if (_approveDialogState.value.isSubmitting) return
        KLogger.d(TAG) { "closeApproveDialog" }
        _approveDialogState.value = ApproveDialogState()
    }

    fun submitApprove() {
        val dialog = _approveDialogState.value
        if (!dialog.isOpen || dialog.isSubmitting) return
        KLogger.d(TAG) { "submitApprove start" }
        _approveDialogState.value = dialog.copy(isSubmitting = true, errorMessage = null)
        _uiState.update { it.copy(bannerActionErrorMessage = null) }
        viewModelScope.launch {
            when (val result = useCase.approveProject(projectId)) {
                ProjectInfoUseCase.ActionResult.Success -> {
                    KLogger.i(TAG) { "submitApprove success" }
                    _approveDialogState.value = ApproveDialogState()
                    _uiState.update {
                        it.copy(
                            projectStatus = ProjectStatus.Approved,
                            bannerActionErrorMessage = null
                        )
                    }
                }
                is ProjectInfoUseCase.ActionResult.Fail -> {
                    val message = result.message ?: str.ProjectModerationApproveError
                    KLogger.w(TAG) { "submitApprove failed: status=${result.status} message=$message" }
                    _approveDialogState.update { it.copy(isSubmitting = false, errorMessage = message) }
                    _uiState.update { it.copy(bannerActionErrorMessage = message) }
                }
            }
        }
    }

    fun openDisapproveDialog() {
        KLogger.d(TAG) { "openDisapproveDialog" }
        _disapproveDialogState.value = DisapproveDialogState(isOpen = true)
    }

    fun closeDisapproveDialog() {
        if (_disapproveDialogState.value.isSubmitting) return
        KLogger.d(TAG) { "closeDisapproveDialog" }
        _disapproveDialogState.value = DisapproveDialogState()
    }

    fun setDisapproveReason(value: String) {
        _disapproveDialogState.update {
            it.copy(
                reason = value.take(MAX_DISAPPROVE_REASON_LENGTH),
                errorMessage = null
            )
        }
    }

    fun submitDisapprove() {
        val dialog = _disapproveDialogState.value
        if (!dialog.isOpen || dialog.isSubmitting) return
        val reason = dialog.reason.trim()
        KLogger.d(TAG) { "submitDisapprove start: reasonLength=${reason.length}" }
        _disapproveDialogState.value = dialog.copy(isSubmitting = true, errorMessage = null)
        _uiState.update { it.copy(bannerActionErrorMessage = null) }
        viewModelScope.launch {
            when (val result = useCase.disapproveProject(projectId, reason)) {
                ProjectInfoUseCase.ActionResult.Success -> {
                    KLogger.i(TAG) { "submitDisapprove success" }
                    _disapproveDialogState.value = DisapproveDialogState()
                    _uiState.update {
                        it.copy(
                            projectStatus = ProjectStatus.Unapproved,
                            bannerActionErrorMessage = null
                        )
                    }
                }
                is ProjectInfoUseCase.ActionResult.Fail -> {
                    val message = result.message ?: str.ProjectModerationDisapproveError
                    KLogger.w(TAG) { "submitDisapprove failed: status=${result.status} message=$message" }
                    _disapproveDialogState.update { it.copy(isSubmitting = false, errorMessage = message) }
                    _uiState.update { it.copy(bannerActionErrorMessage = message) }
                }
            }
        }
    }

    private companion object {
        private const val TAG = "ProjectModerationViewModel"
        private const val MAX_DISAPPROVE_REASON_LENGTH = 200
    }
}

