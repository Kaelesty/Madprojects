package ru.kaelesty.madprojects.features.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.profile.ProfileProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import domain.profile.CommonProfileResponse
import ru.kaelesty.madprojects.features.profile.domain.GetCommonProfileUseCase
import ru.kaelesty.madprojects.features.profile.domain.GetUserProjectsUseCase
import ru.kaelesty.madprojects.features.profile.domain.JoinProjectUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger
import io.ktor.http.HttpStatusCode

class CommonProfileViewModel(
    private val useCase: GetCommonProfileUseCase,
    private val projectsUseCase: GetUserProjectsUseCase,
    private val joinProjectUseCase: JoinProjectUseCase,
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

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    private val _projectsState = MutableStateFlow<ProjectsState>(ProjectsState.Loading)
    val projectsState = _projectsState.asStateFlow()

    private val _joinDialogState = MutableStateFlow(JoinDialogState())
    val joinDialogState = _joinDialogState.asStateFlow()

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

    private companion object {
        private const val TAG = "CommonProfileViewModel"
    }
}
