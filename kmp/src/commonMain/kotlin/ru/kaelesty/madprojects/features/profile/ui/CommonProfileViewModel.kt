package ru.kaelesty.madprojects.features.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.profile.ProfileProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import domain.profile.CommonProfileResponse
import ru.kaelesty.madprojects.api.profile.UpdateProfileRequest
import ru.kaelesty.madprojects.features.profile.domain.GetCommonProfileUseCase
import ru.kaelesty.madprojects.features.profile.domain.GetUserProjectsUseCase
import ru.kaelesty.madprojects.features.profile.domain.JoinProjectUseCase
import ru.kaelesty.madprojects.features.profile.domain.UpdateCommonProfileUseCase
import ru.kaelesty.madprojects.features.auth.domain.StartGithubOauthUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger
import io.ktor.http.HttpStatusCode

class CommonProfileViewModel(
    private val useCase: GetCommonProfileUseCase,
    private val projectsUseCase: GetUserProjectsUseCase,
    private val joinProjectUseCase: JoinProjectUseCase,
    private val updateProfileUseCase: UpdateCommonProfileUseCase,
    private val startGithubOauthUseCase: StartGithubOauthUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(val profile: CommonProfileResponse) : State
        data object Error : State
    }

    sealed interface ProjectsState {
        data object Loading : ProjectsState
        data class Loaded(val projects: List<ProfileProject>) : ProjectsState
        data object Error : ProjectsState
    }

    data class JoinDialogState(
        val isOpen: Boolean = false,
        val inviteCode: String = "",
        val errorMessage: String? = null,
        val isSubmitting: Boolean = false,
    )

    data class EditDialogState(
        val isOpen: Boolean = false,
        val firstName: String = "",
        val secondName: String = "",
        val lastName: String = "",
        val group: String = "",
        val errorMessage: String? = null,
        val isSubmitting: Boolean = false,
    )

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    private val _projectsState = MutableStateFlow<ProjectsState>(ProjectsState.Loading)
    val projectsState = _projectsState.asStateFlow()

    private val _joinDialogState = MutableStateFlow(JoinDialogState())
    val joinDialogState = _joinDialogState.asStateFlow()

    private val _editDialogState = MutableStateFlow(EditDialogState())
    val editDialogState = _editDialogState.asStateFlow()

    init {
        KLogger.d(TAG) { "init: loading profile and projects" }
        load()
        loadProjects()
    }

    fun load() {
        KLogger.d(TAG) { "load profile start" }
        _state.value = State.Loading
        viewModelScope.launch {
            when (val result = useCase.load()) {
                is GetCommonProfileUseCase.Result.Success -> {
                    KLogger.i(TAG) { "load profile success: projects=${result.profile.projects.size} githubMeta=${result.profile.githubMeta != null}" }
                    _state.value = State.Loaded(result.profile)
                }
                GetCommonProfileUseCase.Result.Fail -> {
                    KLogger.w(TAG) { "load profile failed" }
                    _state.value = State.Error
                }
            }
        }
    }

    fun loadProjects() {
        KLogger.d(TAG) { "load projects start" }
        _projectsState.value = ProjectsState.Loading
        viewModelScope.launch {
            when (val result = projectsUseCase.load()) {
                is GetUserProjectsUseCase.Result.Success -> {
                    KLogger.i(TAG) { "load projects success: count=${result.projects.size}" }
                    _projectsState.value = ProjectsState.Loaded(result.projects)
                }
                GetUserProjectsUseCase.Result.Fail -> {
                    KLogger.w(TAG) { "load projects failed" }
                    _projectsState.value = ProjectsState.Error
                }
            }
        }
    }

    fun openJoinDialog() {
        KLogger.d(TAG) { "openJoinDialog" }
        _joinDialogState.value = JoinDialogState(isOpen = true)
    }

    fun closeJoinDialog() {
        if (_joinDialogState.value.isSubmitting) {
            KLogger.d(TAG) { "closeJoinDialog skipped: submitting" }
            return
        }
        KLogger.d(TAG) { "closeJoinDialog" }
        _joinDialogState.value = JoinDialogState()
        loadProjects()
    }

    fun setInviteCode(value: String) {
        _joinDialogState.value = _joinDialogState.value.copy(
            inviteCode = value,
            errorMessage = null
        )
    }

    fun openEditDialog() {
        val profile = (state.value as? State.Loaded)?.profile ?: run {
            KLogger.w(TAG) { "openEditDialog skipped: profile not loaded" }
            return
        }
        KLogger.d(TAG) { "openEditDialog" }
        _editDialogState.value = EditDialogState(
            isOpen = true,
            firstName = profile.firstName,
            secondName = profile.secondName,
            lastName = profile.lastName,
            group = profile.group,
        )
    }

    fun closeEditDialog() {
        if (_editDialogState.value.isSubmitting) {
            KLogger.d(TAG) { "closeEditDialog skipped: submitting" }
            return
        }
        KLogger.d(TAG) { "closeEditDialog" }
        _editDialogState.value = EditDialogState()
    }

    fun setEditFirstName(value: String) {
        _editDialogState.value = _editDialogState.value.copy(
            firstName = value.take(MAX_NAME_LENGTH),
            errorMessage = null,
        )
    }

    fun setEditSecondName(value: String) {
        _editDialogState.value = _editDialogState.value.copy(
            secondName = value.take(MAX_NAME_LENGTH),
            errorMessage = null,
        )
    }

    fun setEditLastName(value: String) {
        _editDialogState.value = _editDialogState.value.copy(
            lastName = value.take(MAX_NAME_LENGTH),
            errorMessage = null,
        )
    }

    fun setEditGroup(value: String) {
        _editDialogState.value = _editDialogState.value.copy(
            group = value.take(MAX_GROUP_LENGTH),
            errorMessage = null,
        )
    }

    fun submitEdit() {
        val loaded = _state.value as? State.Loaded ?: run {
            KLogger.w(TAG) { "submitEdit skipped: profile not loaded" }
            return
        }
        val current = _editDialogState.value
        if (current.isSubmitting) {
            KLogger.d(TAG) { "submitEdit skipped: submitting" }
            return
        }
        val firstName = current.firstName.trim()
        val secondName = current.secondName.trim()
        val lastName = current.lastName.trim()
        val group = current.group.trim()
        when {
            firstName.isBlank() -> return setEditError(str.ProfileEditErrorFirstNameEmpty)
            lastName.isBlank() -> return setEditError(str.ProfileEditErrorLastNameEmpty)
            secondName.isBlank() -> return setEditError(str.ProfileEditErrorSecondNameEmpty)
            group.isBlank() -> return setEditError(str.ProfileEditErrorGroupEmpty)
        }
        val profile = loaded.profile
        val request = UpdateProfileRequest(
            firstName = firstName.takeIf { it != profile.firstName },
            secondName = secondName.takeIf { it != profile.secondName },
            lastName = lastName.takeIf { it != profile.lastName },
            data = group.takeIf { it != profile.group }
        )
        if (request.firstName == null && request.secondName == null && request.lastName == null && request.data == null) {
            KLogger.d(TAG) { "submitEdit blocked: no changes" }
            return setEditError(str.ProfileEditErrorNoChanges)
        }

        KLogger.d(TAG) { "submitEdit start" }
        _editDialogState.value = current.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = updateProfileUseCase.update(request)) {
                UpdateCommonProfileUseCase.Result.Success -> {
                    KLogger.i(TAG) { "submitEdit success" }
                    _editDialogState.value = EditDialogState()
                    load()
                }
                is UpdateCommonProfileUseCase.Result.Fail -> {
                    val message = result.message?.takeIf { it.isNotBlank() } ?: str.ProfileEditErrorSubmit
                    KLogger.w(TAG) { "submitEdit failed: status=${result.status} message=$message" }
                    _editDialogState.value = _editDialogState.value.copy(
                        isSubmitting = false,
                        errorMessage = message
                    )
                }
            }
        }
    }

    fun submitJoin() {
        val current = _joinDialogState.value
        if (current.isSubmitting) {
            KLogger.d(TAG) { "submitJoin skipped: already submitting" }
            return
        }
        val invite = current.inviteCode.trim()
        if (invite.isBlank()) {
            KLogger.w(TAG) { "submitJoin failed: invite empty" }
            _joinDialogState.value = current.copy(errorMessage = str.JoinProjectInviteEmpty)
            return
        }
        KLogger.d(TAG) { "submitJoin start" }
        _joinDialogState.value = current.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = joinProjectUseCase.join(invite)) {
                is JoinProjectUseCase.Result.Success -> {
                    KLogger.i(TAG) { "submitJoin success: projectId=${result.projectId}" }
                    _joinDialogState.value = JoinDialogState()
                    loadProjects()
                }
                is JoinProjectUseCase.Result.Fail -> {
                    val message = joinErrorMessage(result)
                    KLogger.w(TAG) { "submitJoin failed: status=${result.status} message=$message" }
                    _joinDialogState.value = _joinDialogState.value.copy(isSubmitting = false, errorMessage = message)
                }
            }
        }
    }

    private fun joinErrorMessage(result: JoinProjectUseCase.Result.Fail): String {
        return when (result.status) {
            HttpStatusCode.NotFound -> result.message?.takeIf { it.isNotBlank() } ?: str.JoinProjectInviteInvalid
            else -> result.message?.takeIf { it.isNotBlank() } ?: str.LoadError
        }
    }

    private fun setEditError(message: String) {
        KLogger.w(TAG) { "submitEdit validation failed: $message" }
        _editDialogState.value = _editDialogState.value.copy(errorMessage = message)
    }

    suspend fun buildGithubOauthStartUrl(): StartGithubOauthUseCase.Result {
        return startGithubOauthUseCase.buildStartUrl()
    }

    private companion object {
        private const val TAG = "CommonProfileViewModel"
        private const val MAX_NAME_LENGTH = 24
        private const val MAX_GROUP_LENGTH = 24
    }
}
