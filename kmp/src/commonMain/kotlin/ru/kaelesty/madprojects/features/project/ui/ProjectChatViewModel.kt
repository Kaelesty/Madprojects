package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import entities.Action
import entities.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.project.domain.MessengerSocket
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class ProjectChatViewModel(
    private val projectId: String,
    private val chatId: Int,
    private val messengerSocket: MessengerSocket,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class MessageItem(
        val id: Int,
        val text: String,
        val senderId: String,
        val time: Long,
        val isMine: Boolean,
        val isRead: Boolean,
    )

    data class UiState(
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val messages: List<MessageItem> = emptyList(),
        val input: String = "",
        val isSending: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var currentUserId: String? = null

    init {
        KLogger.d(TAG) { "init: chatId=$chatId projectId=$projectId" }
        observeSocketState()
        observeSocketActions()
    }

    fun setInput(value: String) {
        _state.update { it.copy(input = value) }
    }

    fun reload() {
        requestMessages(force = true)
    }

    fun sendMessage() {
        val text = state.value.input.trim()
        if (text.isBlank()) {
            KLogger.d(TAG) { "sendMessage skipped: empty" }
            return
        }
        if (messengerSocket.state.value !is MessengerSocket.ConnectionState.Authorized) {
            KLogger.w(TAG) { "sendMessage blocked: socket not authorized" }
            _state.update { it.copy(errorMessage = str.ProjectMessengerChatError) }
            return
        }
        _state.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            KLogger.d(TAG) { "sendMessage start: length=${text.length}" }
            runCatching { messengerSocket.sendMessage(chatId, text) }
                .onFailure { error ->
                    KLogger.e(TAG, error) { "sendMessage failed" }
                    _state.update { it.copy(isSending = false, errorMessage = str.ProjectMessengerChatError) }
                }
                .onSuccess {
                    _state.update { it.copy(isSending = false, input = "") }
                }
        }
    }

    private fun observeSocketState() {
        viewModelScope.launch {
            messengerSocket.state.collectLatest { socketState ->
                when (socketState) {
                    is MessengerSocket.ConnectionState.Authorized -> {
                        KLogger.d(TAG) { "socket authorized, requesting messages" }
                        requestMessages(force = true)
                    }
                    is MessengerSocket.ConnectionState.Failed -> {
                        KLogger.w(TAG) { "socket failed: ${socketState.reason}" }
                        if (state.value.messages.isEmpty()) {
                            _state.update { it.copy(isLoading = false, errorMessage = socketState.reason) }
                        }
                    }
                    MessengerSocket.ConnectionState.AuthRequired -> {
                        if (state.value.messages.isEmpty()) {
                            _state.update { it.copy(isLoading = false, errorMessage = str.ProjectMessengerChatError) }
                        }
                    }
                    MessengerSocket.ConnectionState.Disconnected -> {
                        if (state.value.messages.isEmpty()) {
                            _state.update { it.copy(isLoading = true) }
                        }
                    }
                    MessengerSocket.ConnectionState.Idle -> Unit
                    is MessengerSocket.ConnectionState.Connecting -> Unit
                    MessengerSocket.ConnectionState.Connected -> Unit
                }
            }
        }
    }

    private fun requestMessages(force: Boolean) {
        if (!force && state.value.isLoading) return
        if (messengerSocket.state.value !is MessengerSocket.ConnectionState.Authorized) {
            KLogger.w(TAG) { "requestMessages skipped: socket not authorized" }
            _state.update { it.copy(isLoading = false, errorMessage = str.ProjectMessengerChatError) }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            messengerSocket.requestChatMessages(chatId)
        }
    }

    private fun observeSocketActions() {
        viewModelScope.launch {
            messengerSocket.actions.collectLatest { action ->
                when (action) {
                    is Action.Messenger.SendChatMessages -> {
                        if (action.chatId != chatId) return@collectLatest
                        KLogger.d(TAG) { "messages received: read=${action.readMessages.size} unread=${action.unreadMessages.size}" }
                        currentUserId = action.userId
                        val unreadIds = action.unreadMessages.map { it.id }.toSet()
                        val combined = (action.readMessages + action.unreadMessages)
                            .sortedWith(compareBy<Message> { it.time }.thenBy { it.id })
                        val items = combined.map { message ->
                            MessageItem(
                                id = message.id,
                                text = message.text,
                                senderId = message.senderId,
                                time = message.time,
                                isMine = message.senderId == action.userId,
                                isRead = message.id !in unreadIds
                            )
                        }
                        _state.update { it.copy(isLoading = false, errorMessage = null, messages = items) }
                        markMessagesRead(action.unreadMessages)
                    }
                    is Action.Messenger.NewMessage -> {
                        if (action.chatId != chatId) return@collectLatest
                        addMessage(action.message)
                    }
                    is Action.Messenger.MessageReadRecorded -> {
                        if (action.chatId != chatId) return@collectLatest
                        markMessageRead(action.messageId)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun addMessage(message: Message) {
        val isMine = currentUserId?.let { message.senderId == it } ?: false
        val item = MessageItem(
            id = message.id,
            text = message.text,
            senderId = message.senderId,
            time = message.time,
            isMine = isMine,
            isRead = isMine
        )
        _state.update { current ->
            val updated = (current.messages + item)
                .sortedWith(compareBy<MessageItem> { it.time }.thenBy { it.id })
            current.copy(messages = updated, isLoading = false, errorMessage = null)
        }
        if (!isMine) {
            markMessagesRead(listOf(message))
        }
    }

    private fun markMessageRead(messageId: Int) {
        _state.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id == messageId) message.copy(isRead = true) else message
                }
            )
        }
    }

    private fun markMessagesRead(messages: List<Message>) {
        if (messages.isEmpty()) return
        val latestId = messages.maxOf { it.id }
        _state.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id <= latestId) message.copy(isRead = true) else message
                }
            )
        }
        viewModelScope.launch {
            KLogger.d(TAG) { "readMessagesBefore: messageId=$latestId" }
            messengerSocket.readMessagesBefore(messageId = latestId, chatId = chatId)
        }
    }

    private companion object {
        private const val TAG = "ProjectChatViewModel"
    }
}
