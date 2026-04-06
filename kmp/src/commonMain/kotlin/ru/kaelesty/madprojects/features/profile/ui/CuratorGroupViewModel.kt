package ru.kaelesty.madprojects.features.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.projectgroups.ProjectInGroupView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
            val ratedProjects: List<ProjectInGroupView>,
        ) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()
    private var isLoadInProgress = false

    init {
        KLogger.d(TAG) { "init: load groupId=$groupId" }
        load()
    }

    fun load() {
        request(showLoading = true)
    }

    fun refreshSilently() {
        request(showLoading = false)
    }

    private fun request(showLoading: Boolean) {
        if (isLoadInProgress) {
            KLogger.d(TAG) { "request skipped: already in progress, showLoading=$showLoading" }
            return
        }
        val hasLoadedContent = _state.value is State.Loaded
        val shouldShowLoading = showLoading || !hasLoadedContent
        KLogger.d(TAG) {
            "request start: groupId=$groupId showLoading=$showLoading hasLoadedContent=$hasLoadedContent -> shouldShowLoading=$shouldShowLoading"
        }
        if (shouldShowLoading) {
            _state.value = State.Loading
        }
        isLoadInProgress = true
        viewModelScope.launch {
            when (val result = useCase.load(groupId)) {
                is GetCuratorGroupProjectsUseCase.Result.Success -> {
                    _state.value = State.Loaded(
                        groupTitle = result.groupTitle,
                        pendingProjects = result.pendingProjects,
                        approvedProjects = result.approvedProjects,
                        ratedProjects = result.ratedProjects,
                    )
                    KLogger.i(TAG) {
                        "request success: pending=${result.pendingProjects.size} approved=${result.approvedProjects.size} rated=${result.ratedProjects.size}"
                    }
                }
                is GetCuratorGroupProjectsUseCase.Result.Fail -> {
                    val message = result.message?.takeIf { it.isNotBlank() } ?: str.LoadError
                    if (shouldShowLoading) {
                        KLogger.w(TAG) { "request failed (visible): $message" }
                        _state.value = State.Error(message)
                    } else {
                        KLogger.w(TAG) { "request failed (silent): $message" }
                    }
                }
            }
            isLoadInProgress = false
        }
    }

    private companion object {
        private const val TAG = "CuratorGroupViewModel"
    }
}
