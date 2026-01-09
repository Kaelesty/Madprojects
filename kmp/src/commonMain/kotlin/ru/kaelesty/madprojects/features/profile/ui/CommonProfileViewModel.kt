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
import ru.kaelesty.madprojects.utils.KLogger

class CommonProfileViewModel(
    private val useCase: GetCommonProfileUseCase,
    private val projectsUseCase: GetUserProjectsUseCase,
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

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    private val _projectsState = MutableStateFlow<ProjectsState>(ProjectsState.Loading)
    val projectsState = _projectsState.asStateFlow()

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

    private companion object {
        private const val TAG = "CommonProfileViewModel"
    }
}
