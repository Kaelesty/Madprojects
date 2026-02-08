package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.sprints.SprintView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.project.domain.FinishSprintUseCase
import ru.kaelesty.madprojects.features.project.domain.GetSprintUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class ProjectSprintDetailsViewModel(
    private val sprintId: String,
    private val getSprintUseCase: GetSprintUseCase,
    private val finishSprintUseCase: FinishSprintUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val sprint: SprintView? = null,
        val errorMessage: String? = null,
        val isFinishing: Boolean = false,
        val finishError: String? = null,
        val finishSuccess: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        KLogger.d(TAG) { "load sprint details start" }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = getSprintUseCase.load(sprintId)) {
                is GetSprintUseCase.Result.Success -> {
                    _state.update { it.copy(isLoading = false, sprint = result.sprint, errorMessage = null) }
                }
                is GetSprintUseCase.Result.Fail -> {
                    val message = result.message ?: str.ProjectSprintsError
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                }
            }
        }
    }

    fun finishSprint() {
        val current = _state.value
        val sprint = current.sprint ?: return
        if (current.isFinishing || sprint.meta.actualEndDate != null) {
            return
        }
        _state.update { it.copy(isFinishing = true, finishError = null, finishSuccess = false) }
        viewModelScope.launch {
            when (val result = finishSprintUseCase.finish(sprintId)) {
                is FinishSprintUseCase.Result.Success -> {
                    _state.update { it.copy(isFinishing = false, finishSuccess = true) }
                    load()
                }
                is FinishSprintUseCase.Result.Fail -> {
                    val message = result.message ?: str.ProjectSprintsError
                    _state.update { it.copy(isFinishing = false, finishError = message) }
                }
            }
        }
    }

    fun consumeFinishSuccess() {
        _state.update { it.copy(finishSuccess = false) }
    }

    private companion object {
        private const val TAG = "ProjectSprintDetailsViewModel"
    }
}
