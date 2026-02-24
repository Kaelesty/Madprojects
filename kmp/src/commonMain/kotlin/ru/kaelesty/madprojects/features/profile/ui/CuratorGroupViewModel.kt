package ru.kaelesty.madprojects.features.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.projectgroups.ProjectInGroupView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.profile.domain.GetCuratorGroupProjectsUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class CuratorGroupViewModel(
    private val groupId: String,
    private val useCase: GetCuratorGroupProjectsUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(
            val groupTitle: String,
            val pendingProjects: List<ProjectInGroupView>,
            val approvedProjects: List<ProjectInGroupView>,
        ) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    init {
        KLogger.d(TAG) { "init: load groupId=$groupId" }
        load()
    }

    fun load() {
        KLogger.d(TAG) { "load start: groupId=$groupId" }
        _state.value = State.Loading
        viewModelScope.launch {
            when (val result = useCase.load(groupId)) {
                is GetCuratorGroupProjectsUseCase.Result.Success -> {
                    _state.value = State.Loaded(
                        groupTitle = result.groupTitle,
                        pendingProjects = result.pendingProjects,
                        approvedProjects = result.approvedProjects,
                    )
                }
                is GetCuratorGroupProjectsUseCase.Result.Fail -> {
                    val message = result.message?.takeIf { it.isNotBlank() } ?: str.LoadError
                    KLogger.w(TAG) { "load failed: $message" }
                    _state.value = State.Error(message)
                }
            }
        }
    }

    private companion object {
        private const val TAG = "CuratorGroupViewModel"
    }
}

