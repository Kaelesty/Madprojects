package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.sprints.ProfileSprint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.api.activity.ActivityResponse
import ru.kaelesty.madprojects.features.project.domain.GetProjectActivitiesUseCase
import ru.kaelesty.madprojects.features.project.domain.GetProjectSprintsUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class ProjectSprintsViewModel(
    private val projectId: String,
    private val getProjectSprintsUseCase: GetProjectSprintsUseCase,
    private val getProjectActivitiesUseCase: GetProjectActivitiesUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(val sprints: List<ProfileSprint>) : State
        data class Error(val message: String?) : State
    }

    sealed interface ActivityState {
        data object Loading : ActivityState
        data class Loaded(val response: ActivityResponse) : ActivityState
        data class Error(val message: String?) : ActivityState
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()
    private val _activityState = MutableStateFlow<ActivityState>(ActivityState.Loading)
    val activityState = _activityState.asStateFlow()

    init {
        load()
    }

    fun load() {
        KLogger.d(TAG) { "load start: sprints + activities" }
        _state.value = State.Loading
        _activityState.value = ActivityState.Loading
        loadSprints()
        loadActivities()
    }

    private fun loadSprints() {
        KLogger.d(TAG) { "load sprints start" }
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

    private fun loadActivities() {
        KLogger.d(TAG) { "load activities start" }
        viewModelScope.launch {
            when (val result = getProjectActivitiesUseCase.load(projectId, ACTIVITY_PREVIEW_COUNT)) {
                is GetProjectActivitiesUseCase.Result.Success -> {
                    _activityState.value = ActivityState.Loaded(result.response)
                }
                is GetProjectActivitiesUseCase.Result.Fail -> {
                    val message = result.message ?: str.ProjectActivityFeedError
                    _activityState.value = ActivityState.Error(message)
                }
            }
        }
    }

    private companion object {
        private const val TAG = "ProjectSprintsViewModel"
        private const val ACTIVITY_PREVIEW_COUNT = 7
    }
}
