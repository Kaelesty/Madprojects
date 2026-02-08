package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.sprints.SprintView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.api.sprints.UpdateSprintRequest
import ru.kaelesty.madprojects.features.project.domain.GetProjectKardsUseCase
import ru.kaelesty.madprojects.features.project.domain.UpdateSprintUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import shared_domain.entities.KanbanState

class ProjectSprintEditViewModel(
    private val projectId: String,
    private val sprintId: String,
    private val sprint: SprintView,
    private val getProjectKardsUseCase: GetProjectKardsUseCase,
    private val updateSprintUseCase: UpdateSprintUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class UiState(
        val title: String = "",
        val desc: String = "",
        val kards: List<KanbanState.Kard> = emptyList(),
        val selectedKardIds: Set<Int> = emptySet(),
        val lockedKardIds: Set<Int> = emptySet(),
        val isLoadingKards: Boolean = true,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
        val success: Boolean = false,
    )

    private val _state = MutableStateFlow(
        UiState(
            title = sprint.meta.title,
            desc = sprint.meta.desc,
            selectedKardIds = sprint.kanban.columns.flatMap { column -> column.kards }.map { it.id }.toSet(),
            lockedKardIds = sprint.kanban.columns.flatMap { column -> column.kards }.map { it.id }.toSet(),
        )
    )
    val state = _state.asStateFlow()

    init {
        loadKards()
    }

    fun setTitle(value: String) {
        _state.update { it.copy(title = value.take(MAX_TITLE_LENGTH), errorMessage = null) }
    }

    fun setDesc(value: String) {
        _state.update { it.copy(desc = value.take(MAX_DESC_LENGTH), errorMessage = null) }
    }

    fun toggleKard(id: Int) {
        _state.update { current ->
            if (current.lockedKardIds.contains(id)) return@update current
            val selected = current.selectedKardIds.toMutableSet()
            if (selected.contains(id)) {
                selected.remove(id)
            } else {
                selected.add(id)
            }
            current.copy(selectedKardIds = selected, errorMessage = null)
        }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        val title = current.title.trim()
        if (title.isBlank()) {
            _state.update { it.copy(errorMessage = str.ProjectSprintValidationTitleEmpty) }
            return
        }
        val desc = current.desc.trim()
        if (desc.isBlank()) {
            _state.update { it.copy(errorMessage = str.ProjectSprintValidationDescEmpty) }
            return
        }
        val newKards = current.selectedKardIds.filterNot { current.lockedKardIds.contains(it) }
        _state.update { it.copy(isSubmitting = true, errorMessage = null, success = false) }
        viewModelScope.launch {
            val request = UpdateSprintRequest(
                sprintId = sprintId,
                title = title,
                desc = desc,
                kardIds = newKards.map { it.toString() }
            )
            when (val result = updateSprintUseCase.update(request)) {
                is UpdateSprintUseCase.Result.Success -> {
                    _state.update { it.copy(isSubmitting = false, success = true) }
                }
                is UpdateSprintUseCase.Result.Fail -> {
                    val message = result.message ?: str.ProjectSprintsError
                    _state.update { it.copy(isSubmitting = false, errorMessage = message) }
                }
            }
        }
    }

    private fun loadKards() {
        _state.update { it.copy(isLoadingKards = true) }
        viewModelScope.launch {
            when (val result = getProjectKardsUseCase.load(projectId)) {
                is GetProjectKardsUseCase.Result.Success -> {
                    _state.update { it.copy(isLoadingKards = false, kards = result.kards) }
                }
                is GetProjectKardsUseCase.Result.Fail -> {
                    val message = result.message ?: str.ProjectSprintsError
                    _state.update { it.copy(isLoadingKards = false, errorMessage = message) }
                }
            }
        }
    }

    private companion object {
        private const val MAX_TITLE_LENGTH = 25
        private const val MAX_DESC_LENGTH = 1000
    }
}
