package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import entities.Action
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.project.domain.CreateKardChatUseCase
import ru.kaelesty.madprojects.features.project.domain.KanbanSocket
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger
import shared_domain.entities.KanbanState

class ProjectKanbanViewModel(
    private val projectId: String,
    private val kanbanSocket: KanbanSocket,
    private val createKardChatUseCase: CreateKardChatUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val kanban: KanbanState? = null,
        val isCreatingChat: Boolean = false,
        val creatingChatForId: Int? = null,
    )

    sealed interface Event {
        data class OpenChat(val chatId: Int) : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val parsedProjectId = projectId.toIntOrNull()
    private var requestKanbanWatchdogJob: Job? = null
    private var lastKanbanRequestSeq = 0L

    init {
        if (parsedProjectId == null) {
            _state.value = UiState(
                isLoading = false,
                errorMessage = str.LoadError,
            )
        } else {
            KLogger.d(TAG) { "init: connect projectId=$parsedProjectId" }
            kanbanSocket.connect(parsedProjectId)
            observeSocketState()
            observeSocketActions()
        }
    }

    override fun onCleared() {
        KLogger.d(TAG) { "onCleared: disconnect" }
        requestKanbanWatchdogJob?.cancel()
        kanbanSocket.disconnect()
    }

    private fun observeSocketState() {
        viewModelScope.launch {
            kanbanSocket.state.collectLatest { state ->
                KLogger.d(TAG) { "socket state event: ${describeSocketState(state)}" }
                when (state) {
                    is KanbanSocket.ConnectionState.Authorized -> {
                        KLogger.d(TAG) { "socket authorized, requesting kanban" }
                        _state.update { it.copy(isLoading = true, errorMessage = null) }
                        scheduleRequestKanbanWatchdog("authorized")
                        kanbanSocket.requestKanban()
                    }
                    is KanbanSocket.ConnectionState.Connecting,
                    is KanbanSocket.ConnectionState.Connected,
                    KanbanSocket.ConnectionState.Disconnected -> {
                        if (_state.value.kanban == null) {
                            _state.update { it.copy(isLoading = true) }
                        }
                    }
                    is KanbanSocket.ConnectionState.Failed -> {
                        requestKanbanWatchdogJob?.cancel()
                        KLogger.w(TAG) { "socket failed: ${state.reason}" }
                        _state.update {
                            if (it.kanban == null) {
                                it.copy(isLoading = false, errorMessage = state.reason)
                            } else {
                                it
                            }
                        }
                    }
                    KanbanSocket.ConnectionState.AuthRequired -> {
                        requestKanbanWatchdogJob?.cancel()
                        KLogger.w(TAG) { "socket requires auth" }
                        _state.update {
                            if (it.kanban == null) {
                                it.copy(isLoading = false, errorMessage = str.LoadError)
                            } else {
                                it
                            }
                        }
                    }
                    KanbanSocket.ConnectionState.Idle -> {
                        requestKanbanWatchdogJob?.cancel()
                    }
                }
            }
        }
    }

    private fun observeSocketActions() {
        viewModelScope.launch {
            kanbanSocket.actions.collectLatest { action ->
                KLogger.d(TAG) { "socket action event: ${describeKanbanAction(action)}" }
                when (action) {
                    is Action.Kanban.SetState -> {
                        requestKanbanWatchdogJob?.cancel()
                        KLogger.i(TAG) { "kanban state received: columns=${action.kanban.columns.size}" }
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = null,
                                kanban = action.kanban
                            )
                        }
                    }
                }
            }
        }
    }

    fun createColumn(name: String, color: String) {
        val projectId = parsedProjectId ?: return
        viewModelScope.launch {
            KLogger.d(TAG) { "createColumn: name=$name color=$color" }
            kanbanSocket.createColumn(name, color.trimStart('#'))
        }
    }

    fun updateColumn(id: Int, name: String?, color: String?) {
        viewModelScope.launch {
            KLogger.d(TAG) { "updateColumn: id=$id name=$name color=$color" }
            kanbanSocket.updateColumn(id, name, color?.trimStart('#'))
        }
    }

    fun moveColumn(id: Int, newPosition: Int) {
        updateKanban { kanban ->
            val columns = kanban.columns.toMutableList()
            val fromIndex = columns.indexOfFirst { it.id == id }
            if (fromIndex == -1) return@updateKanban kanban
            val column = columns.removeAt(fromIndex)
            val target = newPosition.coerceIn(0, columns.size)
            columns.add(target, column)
            kanban.copy(columns = columns)
        }
        viewModelScope.launch {
            KLogger.d(TAG) { "moveColumn: id=$id newPosition=$newPosition" }
            kanbanSocket.moveColumn(id, newPosition)
        }
    }

    fun deleteColumn(id: Int) {
        viewModelScope.launch {
            KLogger.d(TAG) { "deleteColumn: id=$id" }
            kanbanSocket.deleteColumn(id)
        }
    }

    fun createKard(name: String, columnId: Int) {
        viewModelScope.launch {
            KLogger.d(TAG) { "createKard: columnId=$columnId name=$name" }
            kanbanSocket.createKard(name = name, desc = "", columnId = columnId)
        }
    }

    fun updateKard(id: Int, name: String?) {
        viewModelScope.launch {
            KLogger.d(TAG) { "updateKard: id=$id name=$name" }
            kanbanSocket.updateKard(id = id, name = name, desc = null)
        }
    }

    fun moveKard(id: Int, columnId: Int, newColumnId: Int, newPosition: Int) {
        updateKanban { kanban ->
            val columns = kanban.columns.toMutableList()
            val fromColumnIndex = columns.indexOfFirst { it.id == columnId }
            val toColumnIndex = columns.indexOfFirst { it.id == newColumnId }
            if (fromColumnIndex == -1 || toColumnIndex == -1) return@updateKanban kanban
            val fromColumn = columns[fromColumnIndex]
            val toColumn = columns[toColumnIndex]
            val fromCards = fromColumn.kards.toMutableList()
            val fromIndex = fromCards.indexOfFirst { it.id == id }
            if (fromIndex == -1) return@updateKanban kanban
            val card = fromCards.removeAt(fromIndex)
            val toCards = if (columnId == newColumnId) fromCards else toColumn.kards.toMutableList()
            val target = newPosition.coerceIn(0, toCards.size)
            toCards.add(target, card)
            columns[fromColumnIndex] = fromColumn.copy(kards = if (columnId == newColumnId) toCards else fromCards)
            if (columnId != newColumnId) {
                columns[toColumnIndex] = toColumn.copy(kards = toCards)
            }
            kanban.copy(columns = columns)
        }
        viewModelScope.launch {
            KLogger.d(TAG) { "moveKard: id=$id columnId=$columnId newColumnId=$newColumnId newPosition=$newPosition" }
            kanbanSocket.moveKard(
                id = id,
                columnId = columnId,
                newColumnId = newColumnId,
                newPosition = newPosition
            )
        }
    }

    fun deleteKard(id: Int) {
        viewModelScope.launch {
            KLogger.d(TAG) { "deleteKard: id=$id" }
            kanbanSocket.deleteKard(id)
        }
    }

    fun createChatForKard(kardId: Int) {
        if (parsedProjectId == null) return
        val current = _state.value
        if (current.isCreatingChat) return
        _state.update { it.copy(isCreatingChat = true, creatingChatForId = kardId, errorMessage = null) }
        viewModelScope.launch {
            when (val result = createKardChatUseCase.create(projectId, kardId)) {
                is CreateKardChatUseCase.Result.Success -> {
                    val chatId = result.chatId.toIntOrNull()
                    if (chatId != null) {
                        updateKanban { kanban ->
                            val columns = kanban.columns.map { column ->
                                column.copy(
                                    kards = column.kards.map { kard ->
                                        if (kard.id == kardId) {
                                            kard.copy(chatId = result.chatId, unreadMessage = 0)
                                        } else {
                                            kard
                                        }
                                    }
                                )
                            }
                            kanban.copy(columns = columns)
                        }
                        _events.tryEmit(Event.OpenChat(chatId))
                    }
                    _state.update { it.copy(isCreatingChat = false, creatingChatForId = null) }
                }
                is CreateKardChatUseCase.Result.Fail -> {
                    val message = result.message ?: str.LoadError
                    _state.update {
                        it.copy(
                            isCreatingChat = false,
                            creatingChatForId = null,
                            errorMessage = message
                        )
                    }
                }
            }
        }
    }

    private fun updateKanban(update: (KanbanState) -> KanbanState) {
        _state.update { current ->
            val kanban = current.kanban ?: return@update current
            current.copy(kanban = update(kanban))
        }
    }

    private fun scheduleRequestKanbanWatchdog(source: String) {
        val seq = ++lastKanbanRequestSeq
        requestKanbanWatchdogJob?.cancel()
        requestKanbanWatchdogJob = viewModelScope.launch {
            delay(KANBAN_REQUEST_WATCHDOG_DELAY_MS)
            val ui = _state.value
            val socketState = kanbanSocket.state.value
            if (ui.kanban == null && ui.isLoading) {
                KLogger.w(TAG) {
                    "kanban request watchdog fired: seq=$seq source=$source projectId=$parsedProjectId " +
                        "uiLoading=${ui.isLoading} error=${ui.errorMessage} socketState=${describeSocketState(socketState)}"
                }
            } else {
                KLogger.d(TAG) {
                    "kanban request watchdog resolved: seq=$seq hasKanban=${ui.kanban != null} " +
                        "uiLoading=${ui.isLoading} socketState=${describeSocketState(socketState)}"
                }
            }
        }
    }

    private fun describeSocketState(state: KanbanSocket.ConnectionState): String = when (state) {
        is KanbanSocket.ConnectionState.Authorized -> "Authorized(projectId=${state.projectId})"
        is KanbanSocket.ConnectionState.Connecting -> "Connecting(attempt=${state.attempt})"
        is KanbanSocket.ConnectionState.Failed -> "Failed(reason=${state.reason})"
        KanbanSocket.ConnectionState.AuthRequired -> "AuthRequired"
        KanbanSocket.ConnectionState.Connected -> "Connected"
        KanbanSocket.ConnectionState.Disconnected -> "Disconnected"
        KanbanSocket.ConnectionState.Idle -> "Idle"
    }

    private fun describeKanbanAction(action: Action.Kanban): String = when (action) {
        is Action.Kanban.SetState -> {
            var cards = 0
            action.kanban.columns.forEach { cards += it.kards.size }
            "SetState(columns=${action.kanban.columns.size}, cards=$cards)"
        }
    }

    private companion object {
        private const val TAG = "ProjectKanbanViewModel"
        private const val KANBAN_REQUEST_WATCHDOG_DELAY_MS = 6_000L
    }
}
