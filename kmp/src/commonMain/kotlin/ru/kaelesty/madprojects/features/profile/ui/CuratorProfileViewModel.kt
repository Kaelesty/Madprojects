package ru.kaelesty.madprojects.features.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.api.profile.CuratorProfileResponse
import ru.kaelesty.madprojects.api.profile.UpdateProfileRequest
import ru.kaelesty.madprojects.features.profile.domain.CreateProjectGroupUseCase
import ru.kaelesty.madprojects.features.profile.domain.DeleteProjectGroupUseCase
import ru.kaelesty.madprojects.features.profile.domain.GetCuratorProfileUseCase
import ru.kaelesty.madprojects.features.profile.domain.UpdateCuratorProfileUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class CuratorProfileViewModel(
    private val getCuratorProfileUseCase: GetCuratorProfileUseCase,
    private val updateCuratorProfileUseCase: UpdateCuratorProfileUseCase,
    private val createProjectGroupUseCase: CreateProjectGroupUseCase,
    private val deleteProjectGroupUseCase: DeleteProjectGroupUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(val profile: CuratorProfileResponse) : State
        data class Error(val message: String?) : State
    }

    data class EditDialogState(
        val isOpen: Boolean = false,
        val firstName: String = "",
        val secondName: String = "",
        val lastName: String = "",
        val grade: String = "",
        val errorMessage: String? = null,
        val isSubmitting: Boolean = false,
    )

    data class DeleteGroupDialogState(
        val isOpen: Boolean = false,
        val groupId: String? = null,
        val groupTitle: String = "",
        val errorMessage: String? = null,
        val isSubmitting: Boolean = false,
    )

    data class CreateGroupDialogState(
        val isOpen: Boolean = false,
        val title: String = "",
        val errorMessage: String? = null,
        val isSubmitting: Boolean = false,
    )

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    private val _editDialogState = MutableStateFlow(EditDialogState())
    val editDialogState = _editDialogState.asStateFlow()

    private val _deleteGroupDialogState = MutableStateFlow(DeleteGroupDialogState())
    val deleteGroupDialogState = _deleteGroupDialogState.asStateFlow()

    private val _createGroupDialogState = MutableStateFlow(CreateGroupDialogState())
    val createGroupDialogState = _createGroupDialogState.asStateFlow()

    private var isLoadInProgress = false

    init {
        KLogger.d(TAG) { "init: loading curator profile" }
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
            "request start: showLoading=$showLoading hasLoadedContent=$hasLoadedContent -> shouldShowLoading=$shouldShowLoading"
        }
        if (shouldShowLoading) {
            _state.value = State.Loading
        }
        isLoadInProgress = true
        viewModelScope.launch {
            when (val result = getCuratorProfileUseCase.load()) {
                is GetCuratorProfileUseCase.Result.Success -> {
                    KLogger.i(TAG) { "request success: groups=${result.profile.projectGroups.size}" }
                    _state.value = State.Loaded(result.profile)
                }
                is GetCuratorProfileUseCase.Result.Fail -> {
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

    fun openEditDialog() {
        val profile = (_state.value as? State.Loaded)?.profile ?: run {
            KLogger.w(TAG) { "openEditDialog skipped: profile not loaded" }
            return
        }
        KLogger.d(TAG) { "openEditDialog" }
        _editDialogState.value = EditDialogState(
            isOpen = true,
            firstName = profile.firstName,
            secondName = profile.secondName,
            lastName = profile.lastName,
            grade = profile.grade,
        )
    }

    fun closeEditDialog() {
        if (_editDialogState.value.isSubmitting) {
            KLogger.d(TAG) { "closeEditDialog skipped: submitting" }
            return
        }
        KLogger.d(TAG) { "closeEditDialog" }
        _editDialogState.value = EditDialogState()
    }

    fun openDeleteGroupDialog(groupId: String, groupTitle: String) {
        KLogger.d(TAG) { "openDeleteGroupDialog: groupId=$groupId" }
        _deleteGroupDialogState.value = DeleteGroupDialogState(
            isOpen = true,
            groupId = groupId,
            groupTitle = groupTitle,
        )
    }

    fun closeDeleteGroupDialog() {
        if (_deleteGroupDialogState.value.isSubmitting) {
            KLogger.d(TAG) { "closeDeleteGroupDialog skipped: submitting" }
            return
        }
        KLogger.d(TAG) { "closeDeleteGroupDialog" }
        _deleteGroupDialogState.value = DeleteGroupDialogState()
    }

    fun openCreateGroupDialog() {
        KLogger.d(TAG) { "openCreateGroupDialog" }
        _createGroupDialogState.value = CreateGroupDialogState(isOpen = true)
    }

    fun closeCreateGroupDialog() {
        if (_createGroupDialogState.value.isSubmitting) {
            KLogger.d(TAG) { "closeCreateGroupDialog skipped: submitting" }
            return
        }
        KLogger.d(TAG) { "closeCreateGroupDialog" }
        _createGroupDialogState.value = CreateGroupDialogState()
    }

    fun setEditFirstName(value: String) {
        _editDialogState.value = _editDialogState.value.copy(
            firstName = value.take(MAX_NAME_LENGTH),
            errorMessage = null,
        )
    }

    fun setEditSecondName(value: String) {
        _editDialogState.value = _editDialogState.value.copy(
            secondName = value.take(MAX_NAME_LENGTH),
            errorMessage = null,
        )
    }

    fun setEditLastName(value: String) {
        _editDialogState.value = _editDialogState.value.copy(
            lastName = value.take(MAX_NAME_LENGTH),
            errorMessage = null,
        )
    }

    fun setEditGrade(value: String) {
        _editDialogState.value = _editDialogState.value.copy(
            grade = value.take(MAX_GRADE_LENGTH),
            errorMessage = null,
        )
    }

    fun setCreateGroupTitle(value: String) {
        _createGroupDialogState.value = _createGroupDialogState.value.copy(
            title = value.take(MAX_GROUP_TITLE_LENGTH),
            errorMessage = null,
        )
    }

    fun submitEdit() {
        val loaded = _state.value as? State.Loaded ?: run {
            KLogger.w(TAG) { "submitEdit skipped: profile not loaded" }
            return
        }
        val current = _editDialogState.value
        if (current.isSubmitting) {
            KLogger.d(TAG) { "submitEdit skipped: submitting" }
            return
        }
        val firstName = current.firstName.trim()
        val secondName = current.secondName.trim()
        val lastName = current.lastName.trim()
        val grade = current.grade.trim()
        when {
            firstName.isBlank() -> return setEditError(str.ProfileEditErrorFirstNameEmpty)
            lastName.isBlank() -> return setEditError(str.ProfileEditErrorLastNameEmpty)
            secondName.isBlank() -> return setEditError(str.ProfileEditErrorSecondNameEmpty)
            grade.isBlank() -> return setEditError(str.ProfileEditErrorGradeEmpty)
        }
        val profile = loaded.profile
        val request = UpdateProfileRequest(
            firstName = firstName.takeIf { it != profile.firstName },
            secondName = secondName.takeIf { it != profile.secondName },
            lastName = lastName.takeIf { it != profile.lastName },
            data = grade.takeIf { it != profile.grade },
        )
        if (request.firstName == null && request.secondName == null && request.lastName == null && request.data == null) {
            KLogger.d(TAG) { "submitEdit blocked: no changes" }
            return setEditError(str.ProfileEditErrorNoChanges)
        }

        KLogger.d(TAG) { "submitEdit start" }
        _editDialogState.value = current.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = updateCuratorProfileUseCase.update(request)) {
                UpdateCuratorProfileUseCase.Result.Success -> {
                    KLogger.i(TAG) { "submitEdit success" }
                    _editDialogState.value = EditDialogState()
                    load()
                }
                is UpdateCuratorProfileUseCase.Result.Fail -> {
                    val message = result.message?.takeIf { it.isNotBlank() } ?: str.ProfileEditErrorSubmit
                    KLogger.w(TAG) { "submitEdit failed: status=${result.status} message=$message" }
                    _editDialogState.value = _editDialogState.value.copy(
                        isSubmitting = false,
                        errorMessage = message,
                    )
                }
            }
        }
    }

    fun submitDeleteGroup() {
        val current = _deleteGroupDialogState.value
        val groupId = current.groupId
        if (!current.isOpen || groupId.isNullOrBlank()) {
            KLogger.w(TAG) { "submitDeleteGroup skipped: dialog is closed or groupId missing" }
            return
        }
        if (current.isSubmitting) {
            KLogger.d(TAG) { "submitDeleteGroup skipped: submitting" }
            return
        }
        KLogger.d(TAG) { "submitDeleteGroup start: groupId=$groupId" }
        _deleteGroupDialogState.value = current.copy(
            isSubmitting = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            when (val result = deleteProjectGroupUseCase.delete(groupId)) {
                DeleteProjectGroupUseCase.Result.Success -> {
                    KLogger.i(TAG) { "submitDeleteGroup success: groupId=$groupId" }
                    _deleteGroupDialogState.value = DeleteGroupDialogState()
                    load()
                }
                is DeleteProjectGroupUseCase.Result.Fail -> {
                    val message = result.message?.takeIf { it.isNotBlank() } ?: str.CuratorProfileDeleteGroupError
                    KLogger.w(TAG) { "submitDeleteGroup failed: status=${result.status} message=$message" }
                    _deleteGroupDialogState.value = _deleteGroupDialogState.value.copy(
                        isSubmitting = false,
                        errorMessage = message,
                    )
                }
            }
        }
    }

    fun submitCreateGroup() {
        val current = _createGroupDialogState.value
        if (!current.isOpen) {
            KLogger.w(TAG) { "submitCreateGroup skipped: dialog is closed" }
            return
        }
        if (current.isSubmitting) {
            KLogger.d(TAG) { "submitCreateGroup skipped: submitting" }
            return
        }
        val title = current.title.trim()
        if (title.isBlank()) {
            KLogger.w(TAG) { "submitCreateGroup validation failed: title is blank" }
            _createGroupDialogState.value = current.copy(errorMessage = str.CuratorProfileCreateGroupErrorTitleEmpty)
            return
        }

        KLogger.d(TAG) { "submitCreateGroup start: titleLength=${title.length}" }
        _createGroupDialogState.value = current.copy(
            title = title,
            isSubmitting = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            when (val result = createProjectGroupUseCase.create(title)) {
                is CreateProjectGroupUseCase.Result.Success -> {
                    KLogger.i(TAG) { "submitCreateGroup success: groupId=${result.group.id}" }
                    _createGroupDialogState.value = CreateGroupDialogState()
                    load()
                }
                is CreateProjectGroupUseCase.Result.Fail -> {
                    val message = result.message?.takeIf { it.isNotBlank() } ?: str.CuratorProfileCreateGroupErrorSubmit
                    KLogger.w(TAG) { "submitCreateGroup failed: status=${result.status} message=$message" }
                    _createGroupDialogState.value = _createGroupDialogState.value.copy(
                        isSubmitting = false,
                        errorMessage = message,
                    )
                }
            }
        }
    }

    private fun setEditError(message: String) {
        KLogger.w(TAG) { "submitEdit validation failed: $message" }
        _editDialogState.value = _editDialogState.value.copy(errorMessage = message)
    }

    private companion object {
        private const val TAG = "CuratorProfileViewModel"
        private const val MAX_NAME_LENGTH = 24
        private const val MAX_GRADE_LENGTH = 64
        private const val MAX_GROUP_TITLE_LENGTH = 64
    }
}
