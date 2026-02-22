package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.project.Project
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.project.domain.ProjectInfoUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class ProjectInfoViewModel(
    private val projectId: String,
    private val useCase: ProjectInfoUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val project: Project? = null,
        val errorMessage: String? = null,
        val teamActionError: String? = null,
        val reposActionError: String? = null,
        val settingsActionError: String? = null,
        val removingMemberIds: Set<String> = emptySet(),
        val removingRepoIds: Set<String> = emptySet(),
    )

    data class EditMetaDialogState(
        val isOpen: Boolean = false,
        val title: String = "",
        val description: String = "",
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
    )

    data class InviteDialogState(
        val isOpen: Boolean = false,
        val inviteCode: String? = null,
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null,
    )

    data class RepoDialogState(
        val isOpen: Boolean = false,
        val link: String = "",
        val isValidating: Boolean = false,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
    )

    data class DeleteProjectDialogState(
        val isOpen: Boolean = false,
        val confirmText: String = "",
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
    ) {
        val isDeleteAllowed: Boolean get() = confirmText.trim() == DELETE_CONFIRM_WORD
    }

    sealed interface Event {
        data object ProjectDeleted : Event
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _editMetaDialogState = MutableStateFlow(EditMetaDialogState())
    val editMetaDialogState = _editMetaDialogState.asStateFlow()

    private val _inviteDialogState = MutableStateFlow(InviteDialogState())
    val inviteDialogState = _inviteDialogState.asStateFlow()

    private val _repoDialogState = MutableStateFlow(RepoDialogState())
    val repoDialogState = _repoDialogState.asStateFlow()

    private val _deleteProjectDialogState = MutableStateFlow(DeleteProjectDialogState())
    val deleteProjectDialogState = _deleteProjectDialogState.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        val hasContent = _uiState.value.project != null
        KLogger.d(TAG) { "load start: projectId=$projectId hasContent=$hasContent" }
        _uiState.update { current ->
            current.copy(
                isLoading = !hasContent,
                isRefreshing = hasContent,
                errorMessage = if (hasContent) current.errorMessage else null,
            )
        }
        viewModelScope.launch {
            when (val result = useCase.loadProject(projectId)) {
                is ProjectInfoUseCase.ProjectResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            project = result.project,
                            errorMessage = null,
                        )
                    }
                }

                is ProjectInfoUseCase.ProjectResult.Fail -> {
                    val message = result.message ?: str.LoadError
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = message,
                            project = current.project
                        )
                    }
                    KLogger.w(TAG) { "load failed: status=${result.status} message=$message" }
                }
            }
        }
    }

    fun clearTeamActionError() {
        _uiState.update { it.copy(teamActionError = null) }
    }

    fun clearReposActionError() {
        _uiState.update { it.copy(reposActionError = null) }
    }

    fun clearSettingsActionError() {
        _uiState.update { it.copy(settingsActionError = null) }
    }

    fun openEditMetaDialog() {
        val project = _uiState.value.project ?: return
        KLogger.d(TAG) { "openEditMetaDialog" }
        _editMetaDialogState.value = EditMetaDialogState(
            isOpen = true,
            title = project.meta.title,
            description = project.meta.desc,
        )
    }

    fun closeEditMetaDialog() {
        if (_editMetaDialogState.value.isSubmitting) return
        KLogger.d(TAG) { "closeEditMetaDialog" }
        _editMetaDialogState.value = EditMetaDialogState()
    }

    fun setEditTitle(value: String) {
        _editMetaDialogState.update { it.copy(title = value.take(MAX_TITLE_LENGTH), errorMessage = null) }
    }

    fun setEditDescription(value: String) {
        _editMetaDialogState.update { it.copy(description = value.take(MAX_DESCRIPTION_LENGTH), errorMessage = null) }
    }

    fun submitEditMeta() {
        val dialog = _editMetaDialogState.value
        val project = _uiState.value.project ?: return
        if (dialog.isSubmitting) return

        val title = dialog.title.trim()
        val desc = dialog.description.trim()
        when {
            title.isBlank() -> {
                _editMetaDialogState.value = dialog.copy(errorMessage = str.ProjectInfoValidationTitleEmpty)
                return
            }

            desc.isBlank() -> {
                _editMetaDialogState.value = dialog.copy(errorMessage = str.ProjectInfoValidationDescriptionEmpty)
                return
            }
        }

        val changedTitle = title.takeIf { it != project.meta.title }
        val changedDesc = desc.takeIf { it != project.meta.desc }
        if (changedTitle == null && changedDesc == null) {
            _editMetaDialogState.value = dialog.copy(errorMessage = str.ProjectInfoNoChanges)
            return
        }

        _editMetaDialogState.value = dialog.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = useCase.updateProjectMeta(projectId, changedTitle, changedDesc)) {
                ProjectInfoUseCase.ActionResult.Success -> {
                    KLogger.i(TAG) { "submitEditMeta success" }
                    _editMetaDialogState.value = EditMetaDialogState()
                    load()
                }

                is ProjectInfoUseCase.ActionResult.Fail -> {
                    val message = result.message ?: str.ProjectInfoUpdateError
                    KLogger.w(TAG) { "submitEditMeta failed: status=${result.status} message=$message" }
                    _editMetaDialogState.update { it.copy(isSubmitting = false, errorMessage = message) }
                }
            }
        }
    }

    fun openInviteDialog() {
        KLogger.d(TAG) { "openInviteDialog" }
        _inviteDialogState.update { current ->
            current.copy(
                isOpen = true,
                errorMessage = null,
            )
        }
        if (_inviteDialogState.value.inviteCode.isNullOrBlank()) {
            loadInvite(isRefresh = false)
        }
    }

    fun closeInviteDialog() {
        if (_inviteDialogState.value.isLoading || _inviteDialogState.value.isRefreshing) return
        KLogger.d(TAG) { "closeInviteDialog" }
        _inviteDialogState.update { it.copy(isOpen = false) }
    }

    fun refreshInvite() {
        if (_inviteDialogState.value.isLoading || _inviteDialogState.value.isRefreshing) return
        loadInvite(isRefresh = true)
    }

    private fun loadInvite(isRefresh: Boolean) {
        KLogger.d(TAG) { "loadInvite start: refresh=$isRefresh" }
        _inviteDialogState.update {
            it.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            val result = if (isRefresh) useCase.refreshInvite(projectId) else useCase.getInvite(projectId)
            when (result) {
                is ProjectInfoUseCase.InviteResult.Success -> {
                    KLogger.i(TAG) { "loadInvite success" }
                    _inviteDialogState.update {
                        it.copy(
                            inviteCode = result.invite,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                        )
                    }
                }

                is ProjectInfoUseCase.InviteResult.Fail -> {
                    val message = result.message ?: str.LoadError
                    KLogger.w(TAG) { "loadInvite failed: status=${result.status} message=$message" }
                    _inviteDialogState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = message,
                        )
                    }
                }
            }
        }
    }

    fun removeMember(memberId: String) {
        val current = _uiState.value
        if (memberId in current.removingMemberIds) return
        KLogger.d(TAG) { "removeMember start: memberId=$memberId" }
        _uiState.update {
            it.copy(
                teamActionError = null,
                removingMemberIds = it.removingMemberIds + memberId
            )
        }
        viewModelScope.launch {
            when (val result = useCase.removeMember(projectId, memberId)) {
                ProjectInfoUseCase.ActionResult.Success -> {
                    KLogger.i(TAG) { "removeMember success: memberId=$memberId" }
                    _uiState.update { it.copy(removingMemberIds = it.removingMemberIds - memberId) }
                    load()
                }

                is ProjectInfoUseCase.ActionResult.Fail -> {
                    val message = result.message ?: str.ProjectInfoRemoveMemberError
                    KLogger.w(TAG) { "removeMember failed: status=${result.status} message=$message" }
                    _uiState.update {
                        it.copy(
                            removingMemberIds = it.removingMemberIds - memberId,
                            teamActionError = message
                        )
                    }
                }
            }
        }
    }

    fun openRepoDialog() {
        KLogger.d(TAG) { "openRepoDialog" }
        _repoDialogState.value = RepoDialogState(isOpen = true)
    }

    fun closeRepoDialog() {
        val current = _repoDialogState.value
        if (current.isValidating || current.isSubmitting) return
        KLogger.d(TAG) { "closeRepoDialog" }
        _repoDialogState.value = RepoDialogState()
    }

    fun setRepoLink(value: String) {
        _repoDialogState.update { it.copy(link = value.take(MAX_REPO_LINK_LENGTH), errorMessage = null) }
    }

    fun submitAddRepository() {
        val current = _repoDialogState.value
        val project = _uiState.value.project ?: return
        if (current.isSubmitting || current.isValidating) return
        val link = current.link.trim()
        if (link.isBlank()) {
            _repoDialogState.value = current.copy(errorMessage = str.CreateProjectRepoLinkEmpty)
            return
        }
        if (project.repos.any { it.link.equals(link, ignoreCase = true) }) {
            _repoDialogState.value = current.copy(errorMessage = str.CreateProjectRepoLinkDuplicate)
            return
        }

        _repoDialogState.value = current.copy(isValidating = true, errorMessage = null)
        viewModelScope.launch {
            when (val verify = useCase.verifyRepoLink(link)) {
                ProjectInfoUseCase.ActionResult.Success -> {
                    _repoDialogState.update { it.copy(isValidating = false, isSubmitting = true) }
                    when (val addResult = useCase.addRepository(projectId, link)) {
                        ProjectInfoUseCase.ActionResult.Success -> {
                            KLogger.i(TAG) { "submitAddRepository success" }
                            _repoDialogState.value = RepoDialogState()
                            _uiState.update { it.copy(reposActionError = null) }
                            load()
                        }

                        is ProjectInfoUseCase.ActionResult.Fail -> {
                            val message = addResult.message ?: str.ProjectInfoAddRepoError
                            KLogger.w(TAG) { "submitAddRepository failed: status=${addResult.status} message=$message" }
                            _repoDialogState.update {
                                it.copy(
                                    isSubmitting = false,
                                    errorMessage = message
                                )
                            }
                        }
                    }
                }

                is ProjectInfoUseCase.ActionResult.Fail -> {
                    val message = mapVerifyRepoError(verify.status, verify.message)
                    KLogger.w(TAG) { "submitAddRepository verify failed: status=${verify.status} message=$message" }
                    _repoDialogState.update {
                        it.copy(
                            isValidating = false,
                            errorMessage = message
                        )
                    }
                }
            }
        }
    }

    fun removeRepository(repoId: String) {
        val current = _uiState.value
        if (repoId in current.removingRepoIds) return
        KLogger.d(TAG) { "removeRepository start: repoId=$repoId" }
        _uiState.update {
            it.copy(
                reposActionError = null,
                removingRepoIds = it.removingRepoIds + repoId
            )
        }
        viewModelScope.launch {
            when (val result = useCase.removeRepository(projectId, repoId)) {
                ProjectInfoUseCase.ActionResult.Success -> {
                    KLogger.i(TAG) { "removeRepository success: repoId=$repoId" }
                    _uiState.update { it.copy(removingRepoIds = it.removingRepoIds - repoId) }
                    load()
                }

                is ProjectInfoUseCase.ActionResult.Fail -> {
                    val message = result.message ?: str.ProjectInfoRemoveRepoError
                    KLogger.w(TAG) { "removeRepository failed: status=${result.status} message=$message" }
                    _uiState.update {
                        it.copy(
                            removingRepoIds = it.removingRepoIds - repoId,
                            reposActionError = message
                        )
                    }
                }
            }
        }
    }

    fun openDeleteProjectDialog() {
        KLogger.d(TAG) { "openDeleteProjectDialog" }
        _deleteProjectDialogState.value = DeleteProjectDialogState(isOpen = true)
    }

    fun closeDeleteProjectDialog() {
        if (_deleteProjectDialogState.value.isSubmitting) return
        KLogger.d(TAG) { "closeDeleteProjectDialog" }
        _deleteProjectDialogState.value = DeleteProjectDialogState()
    }

    fun setDeleteProjectConfirmText(value: String) {
        _deleteProjectDialogState.update { it.copy(confirmText = value.take(DELETE_CONFIRM_WORD.length), errorMessage = null) }
    }

    fun submitDeleteProject() {
        val current = _deleteProjectDialogState.value
        if (current.isSubmitting) return
        if (!current.isDeleteAllowed) {
            _deleteProjectDialogState.value = current.copy(errorMessage = str.ProjectInfoDeleteProjectConfirmError)
            return
        }

        _deleteProjectDialogState.value = current.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = useCase.deleteProject(projectId)) {
                ProjectInfoUseCase.ActionResult.Success -> {
                    KLogger.i(TAG) { "deleteProject success" }
                    _deleteProjectDialogState.value = DeleteProjectDialogState()
                    _events.tryEmit(Event.ProjectDeleted)
                }

                is ProjectInfoUseCase.ActionResult.Fail -> {
                    val message = result.message ?: str.ProjectInfoDeleteProjectError
                    KLogger.w(TAG) { "deleteProject failed: status=${result.status} message=$message" }
                    _deleteProjectDialogState.update { it.copy(isSubmitting = false, errorMessage = message) }
                    _uiState.update { it.copy(settingsActionError = message) }
                }
            }
        }
    }

    private fun mapVerifyRepoError(status: HttpStatusCode?, fallback: String?): String {
        return when (status) {
            HttpStatusCode.TooEarly -> str.CreateProjectRepoLinkNoGithub
            HttpStatusCode.MethodNotAllowed -> str.CreateProjectRepoLinkPrivate
            HttpStatusCode.NotFound, HttpStatusCode.BadRequest -> str.CreateProjectRepoLinkInvalid
            else -> fallback ?: str.ProjectInfoAddRepoError
        }
    }

    private companion object {
        private const val TAG = "ProjectInfoViewModel"
        private const val DELETE_CONFIRM_WORD = "Подтверждаю"
        private const val MAX_TITLE_LENGTH = 32
        private const val MAX_DESCRIPTION_LENGTH = 1000
        private const val MAX_REPO_LINK_LENGTH = 256
    }
}
