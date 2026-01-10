package ru.kaelesty.madprojects.features.projectcreate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.project.AvailableCurator
import domain.project.CreateProjectRequest
import domain.projectgroups.ProjectGroup
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.projectcreate.domain.CreateProjectUseCase
import ru.kaelesty.madprojects.features.projectcreate.domain.GetCuratorGroupsUseCase
import ru.kaelesty.madprojects.features.projectcreate.domain.GetCuratorsUseCase
import ru.kaelesty.madprojects.features.projectcreate.domain.VerifyRepoLinkUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class CreateProjectViewModel(
    private val useCase: CreateProjectUseCase,
    private val getCuratorsUseCase: GetCuratorsUseCase,
    private val getCuratorGroupsUseCase: GetCuratorGroupsUseCase,
    private val verifyRepoLinkUseCase: VerifyRepoLinkUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class State(
        val title: String = "",
        val desc: String = "",
        val maxMembersCount: String = "",
        val selectedCurator: AvailableCurator? = null,
        val selectedGroup: ProjectGroup? = null,
        val repoLinks: List<String> = emptyList(),
        val isRepoDialogOpen: Boolean = false,
        val repoInput: String = "",
        val repoInputError: String? = null,
        val isRepoValidating: Boolean = false,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
    )

    sealed interface ListState<out T> {
        data object Loading : ListState<Nothing>
        data object Disabled : ListState<Nothing>
        data class Loaded<T>(val items: List<T>) : ListState<T>
        data class Error(val message: String?) : ListState<Nothing>
    }

    sealed interface Event {
        data object Successful : Event
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _curatorsState = MutableStateFlow<ListState<AvailableCurator>>(ListState.Loading)
    val curatorsState = _curatorsState.asStateFlow()

    private val _groupsState = MutableStateFlow<ListState<ProjectGroup>>(ListState.Disabled)
    val groupsState = _groupsState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    init {
        loadCurators()
    }

    fun setTitle(value: String) = update { it.copy(title = value.take(MAX_TITLE_LENGTH)) }

    fun setDesc(value: String) = update { it.copy(desc = value.take(MAX_DESC_LENGTH)) }

    fun setMaxMembersCount(value: String) = update { it.copy(maxMembersCount = value) }

    fun setCurator(value: AvailableCurator) {
        KLogger.d(TAG) { "setCurator: id=${value.id}" }
        update { it.copy(selectedCurator = value, selectedGroup = null) }
        loadGroups()
    }

    fun setGroup(value: ProjectGroup) {
        KLogger.d(TAG) { "setGroup: id=${value.id}" }
        update { it.copy(selectedGroup = value) }
    }

    fun openRepoDialog() {
        KLogger.d(TAG) { "openRepoDialog" }
        update {
            it.copy(
                isRepoDialogOpen = true,
                repoInput = "",
                repoInputError = null,
                isRepoValidating = false,
            )
        }
    }

    fun closeRepoDialog() {
        if (state.value.isRepoValidating) {
            KLogger.d(TAG) { "closeRepoDialog skipped: validating" }
            return
        }
        KLogger.d(TAG) { "closeRepoDialog" }
        update {
            it.copy(
                isRepoDialogOpen = false,
                repoInput = "",
                repoInputError = null,
                isRepoValidating = false,
            )
        }
    }

    fun setRepoInput(value: String) {
        update { it.copy(repoInput = value, repoInputError = null) }
    }

    fun confirmRepoInput() {
        val current = state.value
        if (current.isRepoValidating) {
            KLogger.d(TAG) { "confirmRepoInput skipped: already validating" }
            return
        }
        val normalized = normalizeRepoLink(current.repoInput)
        if (normalized.isBlank()) {
            KLogger.w(TAG) { "confirmRepoInput failed: empty link" }
            update { it.copy(repoInputError = str.CreateProjectRepoLinkEmpty) }
            return
        }
        if (current.repoLinks.any { it.equals(normalized, ignoreCase = true) }) {
            KLogger.w(TAG) { "confirmRepoInput failed: duplicate link" }
            update { it.copy(repoInputError = str.CreateProjectRepoLinkDuplicate) }
            return
        }
        KLogger.d(TAG) { "confirmRepoInput validating: $normalized" }
        update { it.copy(isRepoValidating = true, repoInputError = null) }
        viewModelScope.launch {
            when (val result = verifyRepoLinkUseCase.verify(normalized)) {
                VerifyRepoLinkUseCase.Result.Success -> {
                    KLogger.i(TAG) { "confirmRepoInput success: link added" }
                    update {
                        it.copy(
                            repoLinks = it.repoLinks + normalized,
                            isRepoDialogOpen = false,
                            repoInput = "",
                            repoInputError = null,
                            isRepoValidating = false,
                        )
                    }
                }
                is VerifyRepoLinkUseCase.Result.Fail -> {
                    val message = repoLinkErrorMessage(result)
                    KLogger.w(TAG) { "confirmRepoInput failed: status=${result.status} message=$message" }
                    update { it.copy(repoInputError = message, isRepoValidating = false) }
                }
            }
        }
    }

    fun removeRepoLink(link: String) {
        KLogger.d(TAG) { "removeRepoLink: $link" }
        update { it.copy(repoLinks = it.repoLinks.filterNot { existing -> existing == link }) }
    }

    fun loadCurators() {
        KLogger.d(TAG) { "loadCurators start" }
        _curatorsState.value = ListState.Loading
        _groupsState.value = ListState.Disabled
        update { it.copy(selectedCurator = null, selectedGroup = null) }
        viewModelScope.launch {
            when (val result = getCuratorsUseCase.load()) {
                is GetCuratorsUseCase.Result.Success -> {
                    KLogger.i(TAG) { "loadCurators success: count=${result.curators.size}" }
                    _curatorsState.value = ListState.Loaded(result.curators)
                }
                is GetCuratorsUseCase.Result.Fail -> {
                    KLogger.w(TAG) { "loadCurators failed: message=${result.message}" }
                    _curatorsState.value = ListState.Error(result.message)
                }
            }
        }
    }

    fun loadGroups() {
        val curatorId = state.value.selectedCurator?.id
        if (curatorId == null) {
            KLogger.w(TAG) { "loadGroups skipped: curator is null" }
            _groupsState.value = ListState.Disabled
            return
        }
        KLogger.d(TAG) { "loadGroups start: curatorId=$curatorId" }
        _groupsState.value = ListState.Loading
        viewModelScope.launch {
            when (val result = getCuratorGroupsUseCase.load(curatorId)) {
                is GetCuratorGroupsUseCase.Result.Success -> {
                    KLogger.i(TAG) { "loadGroups success: count=${result.groups.size}" }
                    _groupsState.value = ListState.Loaded(result.groups)
                }
                is GetCuratorGroupsUseCase.Result.Fail -> {
                    KLogger.w(TAG) { "loadGroups failed: message=${result.message}" }
                    _groupsState.value = ListState.Error(result.message)
                }
            }
        }
    }

    fun submit() {
        val current = state.value
        val validationError = validate(current)
        if (validationError != null) {
            KLogger.w(TAG) { "validation failed: $validationError" }
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        val maxMembers = current.maxMembersCount.trim().toInt()
        val request = CreateProjectRequest(
            title = current.title.trim(),
            desc = current.desc.trim(),
            maxMembersCount = maxMembers,
            curatorId = current.selectedCurator?.id.orEmpty(),
            projectGroupId = current.selectedGroup?.id.orEmpty(),
            repoLinks = current.repoLinks,
        )

        _state.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = useCase.create(request)) {
                is CreateProjectUseCase.Result.Success -> {
                    KLogger.i(TAG) { "submit success: projectId=${result.projectId}" }
                    _state.update { it.copy(isSubmitting = false) }
                    _events.emit(Event.Successful)
                }
                is CreateProjectUseCase.Result.Fail -> {
                    val message = result.message?.takeIf { it.isNotBlank() } ?: str.CreateProjectErrorUnavailable
                    KLogger.w(TAG) { "submit failed: message=$message" }
                    _state.update { it.copy(isSubmitting = false, errorMessage = message) }
                }
            }
        }
    }

    private fun validate(state: State): String? {
        if (state.title.isBlank()) return str.CreateProjectErrorTitleEmpty
        if (state.title.length > MAX_TITLE_LENGTH) return str.CreateProjectErrorTitleTooLong
        if (state.desc.isBlank()) return str.CreateProjectErrorDescriptionEmpty
        if (state.desc.length > MAX_DESC_LENGTH) return str.CreateProjectErrorDescriptionTooLong
        val maxMembersRaw = state.maxMembersCount.trim()
        if (maxMembersRaw.isBlank()) return str.CreateProjectErrorMaxMembersEmpty
        val maxMembers = maxMembersRaw.toIntOrNull()
        if (maxMembers == null || maxMembers <= 0) return str.CreateProjectErrorMaxMembersInvalid
        if (maxMembers > MAX_MEMBERS) return str.CreateProjectErrorMaxMembersLimit
        if (state.selectedCurator == null) return str.CreateProjectErrorCuratorEmpty
        if (state.selectedGroup == null) return str.CreateProjectErrorGroupEmpty
        return null
    }

    private fun normalizeRepoLink(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.endsWith("/")) trimmed.dropLast(1) else trimmed
    }

    private fun repoLinkErrorMessage(result: VerifyRepoLinkUseCase.Result.Fail): String {
        return when (result.status) {
            HttpStatusCode.TooEarly -> str.CreateProjectRepoLinkNoGithub
            HttpStatusCode.MethodNotAllowed -> str.CreateProjectRepoLinkPrivate
            HttpStatusCode.NotFound, HttpStatusCode.BadRequest -> str.CreateProjectRepoLinkInvalid
            else -> result.message?.takeIf { it.isNotBlank() } ?: str.LoadError
        }
    }

    private fun update(update: (State) -> State) {
        _state.update { update(it).copy(errorMessage = null) }
    }

    private companion object {
        private const val TAG = "CreateProjectViewModel"
        private const val MAX_MEMBERS = 20
        private const val MAX_TITLE_LENGTH = 25
        private const val MAX_DESC_LENGTH = 300
    }
}
