package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.sprints.ProfileSprint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.project.domain.GetProjectSprintsUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class ProjectSprintsViewModel(
    private val projectId: String,
    private val getProjectSprintsUseCase: GetProjectSprintsUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(val sprints: List<ProfileSprint>) : State
        data class Error(val message: String?) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        KLogger.d(TAG) { "load sprints start" }
        _state.value = State.Loading
        viewModelScope.launch {
            when (val result = getProjectSprintsUseCase.load(projectId)) {
                is GetProjectSprintsUseCase.Result.Success -> {
                    _state.value = State.Loaded(result.sprints)
                }
                is GetProjectSprintsUseCase.Result.Fail -> {
                    val message = result.message ?: str.ProjectSprintsError
                    _state.value = State.Error(message)
                }
            }
        }
    }

    private companion object {
        private const val TAG = "ProjectSprintsViewModel"
    }
}
