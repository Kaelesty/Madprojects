package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import entities.Action
import entities.Chat
import entities.ChatType
import entities.ChatSender
import entities.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.project.domain.MessengerSocket
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger

class ProjectMessengerViewModel(
    private val projectId: String,
    private val messengerSocket: MessengerSocket,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class ChatItem(
        val id: Int,
        val title: String,
        val lastMessage: String,
        val unreadCount: Int,
    )

    sealed interface ChatsState {
        data object Loading : ChatsState
        data class Loaded(val chats: List<ChatItem>) : ChatsState
        data class Error(val message: String?) : ChatsState
    }

    data class CreateChatDialogState(
        val isOpen: Boolean = false,
        val title: String = "",
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
    )

    private val _chatsState = MutableStateFlow<ChatsState>(ChatsState.Loading)
    val chatsState = _chatsState.asStateFlow()

    private val _createChatDialogState = MutableStateFlow(CreateChatDialogState())
    val createChatDialogState = _createChatDialogState.asStateFlow()

    private var sendersById: Map<String, ChatSender> = emptyMap()

    init {
        KLogger.d(TAG) { "init: subscribe socket for projectId=$projectId" }
        observeSocketState()
        observeSocketActions()
    }

    private fun observeSocketState() {
        viewModelScope.launch {
            messengerSocket.state.collectLatest { state ->
                when (state) {
                    is MessengerSocket.ConnectionState.Authorized -> {
                        KLogger.d(TAG) { "socket authorized, requesting chats" }
                        _chatsState.value = ChatsState.Loading
                        messengerSocket.requestChatsList()
                    }
                    is MessengerSocket.ConnectionState.Connecting,
                    is MessengerSocket.ConnectionState.Connected -> {
                        if (_chatsState.value !is ChatsState.Loaded) {
                            _chatsState.value = ChatsState.Loading
                        }
                    }
                    is MessengerSocket.ConnectionState.Failed -> {
                        KLogger.w(TAG) { "socket failed: ${state.reason}" }
                        if (_chatsState.value !is ChatsState.Loaded) {
                            _chatsState.value = ChatsState.Error(state.reason)
                        }
                    }
                    MessengerSocket.ConnectionState.AuthRequired -> {
                        KLogger.w(TAG) { "socket requires auth" }
                        if (_chatsState.value !is ChatsState.Loaded) {
                            _chatsState.value = ChatsState.Error(str.LoadError)
                        }
                    }
                    MessengerSocket.ConnectionState.Disconnected -> {
                        if (_chatsState.value !is ChatsState.Loaded) {
                            _chatsState.value = ChatsState.Loading
                        }
                    }
                    MessengerSocket.ConnectionState.Idle -> Unit
                }
            }
        }
    }

    private fun observeSocketActions() {
        viewModelScope.launch {
            messengerSocket.actions.collectLatest { action ->
                when (action) {
                    is Action.Messenger.SendChatsList -> {
                        KLogger.i(TAG) { "chats list received: count=${action.chats.size}" }
                        sendersById = action.senders.associateBy { it.id }
                        _chatsState.value = ChatsState.Loaded(
                            action.chats.map { it.toChatItem() }
                        )
                    }
                    is Action.Messenger.NewChat -> {
                        KLogger.i(TAG) { "new chat received: id=${action.chat.id}" }
                        addChat(action.chat)
                    }
                    is Action.Messenger.NewMessage -> {
                        updateLastMessage(action.chatId, action.message)
                    }
                    is Action.Messenger.UpdateChatUnreadCount -> {
                        updateUnreadCount(action.chatId, action.count)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun addChat(chat: Chat) {
        _chatsState.update { current ->
            val existing = (current as? ChatsState.Loaded)?.chats.orEmpty()
            if (existing.any { it.id == chat.id }) return@update current
            ChatsState.Loaded(listOf(chat.toChatItem()) + existing)
        }
    }

    private fun updateLastMessage(chatId: Int, message: Message) {
        _chatsState.update { current ->
            val loaded = current as? ChatsState.Loaded ?: return@update current
            val updated = loaded.chats.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(lastMessage = formatLastMessage(message))
                } else {
                    chat
                }
            }
            ChatsState.Loaded(updated)
        }
    }

    private fun updateUnreadCount(chatId: Int, count: Int) {
        _chatsState.update { current ->
            val loaded = current as? ChatsState.Loaded ?: return@update current
            val updated = loaded.chats.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(unreadCount = count)
                } else {
                    chat
                }
            }
            ChatsState.Loaded(updated)
        }
    }

    private fun Chat.toChatItem(): ChatItem {
        return ChatItem(
            id = id,
            title = title,
            lastMessage = lastMessage?.let { formatLastMessage(it) } ?: str.ProjectMessengerLastMessagePlaceholder,
            unreadCount = unreadMessagesCount,
        )
    }

    private fun formatLastMessage(message: Message): String {
        val sender = sendersById[message.senderId]
        val senderName = sender?.let { formatSenderName(it) }.orEmpty()
        val text = message.text.trim()
        return when {
            senderName.isNotBlank() && text.isNotBlank() -> "$senderName: $text"
            text.isNotBlank() -> text
            senderName.isNotBlank() -> senderName
            else -> str.ProjectMessengerLastMessagePlaceholder
        }
    }

    private fun formatSenderName(sender: ChatSender): String {
        return listOf(sender.lastName, sender.firstName, sender.secondName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    fun openCreateChatDialog() {
        KLogger.d(TAG) { "openCreateChatDialog" }
        _createChatDialogState.value = CreateChatDialogState(isOpen = true)
    }

    fun closeCreateChatDialog() {
        if (_createChatDialogState.value.isSubmitting) {
            KLogger.d(TAG) { "closeCreateChatDialog skipped: submitting" }
            return
        }
        KLogger.d(TAG) { "closeCreateChatDialog" }
        _createChatDialogState.value = CreateChatDialogState()
    }

    fun setCreateChatTitle(value: String) {
        _createChatDialogState.update { current ->
            current.copy(title = value, errorMessage = null)
        }
    }

    fun submitCreateChat() {
        val current = _createChatDialogState.value
        if (current.isSubmitting) {
            KLogger.d(TAG) { "submitCreateChat skipped: submitting" }
            return
        }
        val title = current.title.trim()
        if (title.isBlank()) {
            KLogger.d(TAG) { "submitCreateChat blocked: empty title" }
            _createChatDialogState.value = current.copy(errorMessage = str.ProjectMessengerCreateTitleEmpty)
            return
        }
        if (messengerSocket.state.value !is MessengerSocket.ConnectionState.Authorized) {
            KLogger.w(TAG) { "submitCreateChat blocked: socket not authorized" }
            _createChatDialogState.value = current.copy(errorMessage = str.ProjectMessengerCreateConnectionError)
            return
        }
        _createChatDialogState.value = current.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            KLogger.d(TAG) { "createChat start: title=$title" }
            runCatching { messengerSocket.createChat(title, ChatType.Public) }
                .onFailure { error ->
                    KLogger.e(TAG, error) { "createChat failed" }
                    _createChatDialogState.value = current.copy(
                        isSubmitting = false,
                        errorMessage = str.LoadError
                    )
                }
                .onSuccess {
                    KLogger.i(TAG) { "createChat request sent" }
                    _createChatDialogState.value = CreateChatDialogState()
                }
        }
    }

    private companion object {
        private const val TAG = "ProjectMessengerViewModel"
    }
}
