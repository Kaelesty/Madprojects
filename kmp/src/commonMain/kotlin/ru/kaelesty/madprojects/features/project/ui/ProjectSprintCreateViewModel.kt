package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.kaelesty.madprojects.api.sprints.CreateSprintRequest
import ru.kaelesty.madprojects.features.project.domain.CreateSprintUseCase
import ru.kaelesty.madprojects.features.project.domain.GetProjectKardsUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.nowMillis
import shared_domain.entities.KanbanState

class ProjectSprintCreateViewModel(
    private val projectId: String,
    private val getProjectKardsUseCase: GetProjectKardsUseCase,
    private val createSprintUseCase: CreateSprintUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class UiState(
        val title: String = "",
        val desc: String = "",
        val endDate: String = "",
        val kards: List<KanbanState.Kard> = emptyList(),
        val selectedKardIds: Set<Int> = emptySet(),
        val isLoadingKards: Boolean = true,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
        val success: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
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

    fun setEndDate(value: String) {
        _state.update { it.copy(endDate = value.take(MAX_DATE_LENGTH), errorMessage = null) }
    }

    fun toggleKard(id: Int) {
        _state.update { current ->
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
        val endDateText = current.endDate.trim()
        if (endDateText.isBlank()) {
            _state.update { it.copy(errorMessage = str.ProjectSprintValidationEndDateEmpty) }
            return
        }
        val endDate = parseDate(endDateText) ?: run {
            _state.update { it.copy(errorMessage = str.ProjectSprintValidationEndDateInvalid) }
            return
        }
        val today = today()
        if (endDate.toEpochDays() < today.toEpochDays()) {
            _state.update { it.copy(errorMessage = str.ProjectSprintValidationEndDatePast) }
            return
        }
        if (current.selectedKardIds.isEmpty()) {
            _state.update { it.copy(errorMessage = str.ProjectSprintValidationTasksEmpty) }
            return
        }

        _state.update { it.copy(isSubmitting = true, errorMessage = null, success = false) }
        viewModelScope.launch {
            val request = CreateSprintRequest(
                projectId = projectId,
                title = title,
                desc = desc,
                endDate = endDateText,
                kardIds = current.selectedKardIds.map { it.toString() }
            )
            when (val result = createSprintUseCase.create(request)) {
                is CreateSprintUseCase.Result.Success -> {
                    _state.update { it.copy(isSubmitting = false, success = true) }
                }
                is CreateSprintUseCase.Result.Fail -> {
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

    private fun parseDate(value: String): LocalDate? {
        val parts = value.split(".")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        return runCatching { LocalDate(year, month, day) }.getOrNull()
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun today(): LocalDate {
        return Instant.fromEpochMilliseconds(nowMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }

    private companion object {
        private const val MAX_TITLE_LENGTH = 25
        private const val MAX_DESC_LENGTH = 1000
        private const val MAX_DATE_LENGTH = 10
    }
}
