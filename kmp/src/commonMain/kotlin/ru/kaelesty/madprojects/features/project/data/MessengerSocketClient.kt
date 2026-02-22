package ru.kaelesty.madprojects.features.project.data

import entities.Action
import entities.ChatType
import entities.Intent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.project.domain.MessengerSocket
import ru.kaelesty.madprojects.ktor.KtorConfig
import ru.kaelesty.madprojects.utils.KLogger

class MessengerSocketClient(
    private val client: HttpClient,
    private val authContext: AuthContext,
    private val baseUrl: String = KtorConfig.BaseUrl,
) : MessengerSocket {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val outgoing = MutableSharedFlow<Intent>(extraBufferCapacity = OUTGOING_BUFFER)
    private val _actions = MutableSharedFlow<Action.Messenger>(extraBufferCapacity = ACTIONS_BUFFER)
    private val _state = MutableStateFlow<MessengerSocket.ConnectionState>(MessengerSocket.ConnectionState.Idle)

    override val actions: SharedFlow<Action.Messenger> = _actions.asSharedFlow()
    override val state: StateFlow<MessengerSocket.ConnectionState> = _state.asStateFlow()

    private var connectJob: Job? = null
    private var activeProjectId: Int? = null
    private var session: DefaultClientWebSocketSession? = null
    private var sentIntentsCount = 0
    private var receivedFramesCount = 0
    private var receivedMessengerActionsCount = 0

    override fun connect(projectId: Int) {
        KLogger.d(TAG) {
            "connect requested: projectId=$projectId activeProjectId=$activeProjectId jobActive=${connectJob?.isActive == true}"
        }
        if (activeProjectId == projectId && connectJob?.isActive == true) {
            KLogger.d(TAG) { "connect skipped: already active projectId=$projectId" }
            return
        }
        activeProjectId = projectId
        connectJob?.cancel()
        connectJob = scope.launch {
            connectionLoop(projectId)
        }
    }

    override fun disconnect() {
        KLogger.d(TAG) { "disconnect requested: activeProjectId=$activeProjectId" }
        connectJob?.cancel()
        connectJob = null
        activeProjectId = null
        scope.launch {
            closeSession("client disconnect")
        }
        setState(MessengerSocket.ConnectionState.Idle, "disconnect()")
    }

    override suspend fun requestChatsList() {
        val projectId = activeProjectId ?: return logMissingProject("requestChatsList")
        sendIntent(Intent.Messenger.RequestChatsList(projectId))
    }

    override suspend fun requestChatMessages(chatId: Int) {
        val projectId = activeProjectId ?: return logMissingProject("requestChatMessages")
        sendIntent(Intent.Messenger.RequestChatMessages(chatId = chatId, projectId = projectId))
    }

    override suspend fun sendMessage(chatId: Int, message: String) {
        val projectId = activeProjectId ?: return logMissingProject("sendMessage")
        sendIntent(Intent.Messenger.SendMessage(chatId = chatId, message = message, projectId = projectId))
    }

    override suspend fun createChat(title: String, chatType: ChatType) {
        val projectId = activeProjectId ?: return logMissingProject("createChat")
        sendIntent(Intent.Messenger.CreateChat(projectId = projectId, chatTitle = title, chatType = chatType))
    }

    override suspend fun readMessage(messageId: Int, chatId: Int) {
        val projectId = activeProjectId ?: return logMissingProject("readMessage")
        sendIntent(Intent.Messenger.ReadMessage(messageId = messageId, chatId = chatId, projectId = projectId))
    }

    override suspend fun readMessagesBefore(messageId: Int, chatId: Int) {
        val projectId = activeProjectId ?: return logMissingProject("readMessagesBefore")
        sendIntent(Intent.Messenger.ReadMessagesBefore(messageId = messageId, chatId = chatId, projectId = projectId))
    }

    private suspend fun connectionLoop(projectId: Int) {
        KLogger.d(TAG) { "connectionLoop start: projectId=$projectId baseUrl=$baseUrl" }
        var attempt = 0
        var delayMs = RECONNECT_DELAY_MS
        while (currentCoroutineContext().isActive) {
            attempt += 1
            setState(MessengerSocket.ConnectionState.Connecting(attempt), "attempt=$attempt")
            val token = authContext.getAccessToken()
            if (token.isNullOrBlank()) {
                setState(MessengerSocket.ConnectionState.AuthRequired, "no access token")
                KLogger.w(TAG) { "connect skipped: no access token" }
                delay(AUTH_RETRY_DELAY_MS)
                continue
            }
            var wsSession: DefaultClientWebSocketSession? = null
            try {
                val url = "${toWebSocketBaseUrl(baseUrl)}$PROJECT_PATH"
                KLogger.d(TAG) { "connecting: url=$url attempt=$attempt tokenLength=${token.length} projectId=$projectId" }
                wsSession = client.webSocketSession(urlString = url)
                session = wsSession
                KLogger.i(TAG) { "websocket session opened: attempt=$attempt projectId=$projectId" }
                setState(MessengerSocket.ConnectionState.Connected, "session opened")

                sendDirect(wsSession, Intent.Authorize(token))
                sendDirect(wsSession, Intent.Messenger.Start(projectId))
                setState(MessengerSocket.ConnectionState.Authorized(projectId), "authorize+messenger.start sent")
                delayMs = RECONNECT_DELAY_MS

                coroutineScope {
                    KLogger.d(TAG) { "connection coroutines started: projectId=$projectId attempt=$attempt" }
                    val sender = launch { sendLoop(wsSession) }
                    val receiver = launch { receiveLoop(wsSession) }
                    receiver.join()
                    KLogger.w(TAG) { "receiveLoop finished: projectId=$projectId attempt=$attempt; cancelling sender" }
                    sender.cancelAndJoin()
                    KLogger.d(TAG) { "connection coroutines finished: projectId=$projectId attempt=$attempt" }
                }
            } catch (e: CancellationException) {
                KLogger.d(TAG) { "connectionLoop cancelled: projectId=$projectId attempt=$attempt" }
                throw e
            } catch (e: Exception) {
                KLogger.e(TAG, e) { "socket failure" }
                setState(MessengerSocket.ConnectionState.Failed(e.message ?: "unknown"), "exception")
            } finally {
                closeSession("connection closed", wsSession)
                if (currentCoroutineContext().isActive) {
                    setState(MessengerSocket.ConnectionState.Disconnected, "finally")
                }
            }
            if (!currentCoroutineContext().isActive) break
            KLogger.d(TAG) { "reconnect scheduled in ${delayMs}ms" }
            delay(delayMs)
            delayMs = (delayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
        }
        KLogger.d(TAG) { "connectionLoop end: projectId=$projectId" }
    }

    private suspend fun sendLoop(wsSession: DefaultClientWebSocketSession) {
        KLogger.d(TAG) { "sendLoop started" }
        outgoing.collect { intent ->
            runCatching {
                sendDirect(wsSession, intent)
            }.onFailure { error ->
                KLogger.w(TAG) { "send failed: ${error.message}" }
                closeSession("send failed")
            }
        }
    }

    private suspend fun receiveLoop(wsSession: DefaultClientWebSocketSession) {
        KLogger.d(TAG) { "receiveLoop started" }
        for (frame in wsSession.incoming) {
            if (frame !is Frame.Text) {
                KLogger.d(TAG) { "incoming non-text frame ignored: ${frame::class.simpleName}" }
                continue
            }
            val text = frame.readText()
            receivedFramesCount += 1
            KLogger.d(TAG) {
                "incoming text frame #$receivedFramesCount: type=${extractType(text)} length=${text.length}"
            }
            val actionResult = runCatching { socketJson.decodeFromString<Action>(text) }
            if (actionResult.isFailure) {
                val error = actionResult.exceptionOrNull()
                KLogger.w(TAG) {
                    "failed to decode action: ${error?.message}; rawType=${extractType(text)} rawSnippet=${text.take(200)}"
                }
                continue
            }
            val action = actionResult.getOrThrow()
            KLogger.d(TAG) { "action decoded: ${describeAction(action)}" }
            when (action) {
                is Action.Unauthorized -> {
                    KLogger.w(TAG) { "received unauthorized action" }
                    authContext.onUnauthorizedResponse()
                    closeSession("unauthorized")
                }
                is Action.Messenger -> {
                    receivedMessengerActionsCount += 1
                    KLogger.i(TAG) { "messenger action #$receivedMessengerActionsCount: ${describeMessengerAction(action)}" }
                    _actions.emit(action)
                }
                else -> Unit
            }
        }
        KLogger.w(TAG) { "receiveLoop ended: incoming channel completed" }
    }

    private suspend fun sendIntent(intent: Intent) {
        if (_state.value !is MessengerSocket.ConnectionState.Authorized) {
            KLogger.w(TAG) { "send dropped: not connected state=${describeState(_state.value)} intent=${describeIntent(intent)}" }
            return
        }
        if (outgoing.tryEmit(intent)) {
            KLogger.d(TAG) { "intent queued: ${describeIntent(intent)}" }
        } else {
            KLogger.w(TAG) { "send dropped: buffer full intent=${describeIntent(intent)}" }
        }
    }

    private suspend fun sendDirect(wsSession: DefaultClientWebSocketSession, intent: Intent) {
        val payload = socketJson.encodeToString(intent)
        sentIntentsCount += 1
        KLogger.d(TAG) {
            "sendDirect #$sentIntentsCount: ${describeIntent(intent)} type=${extractType(payload)} payloadLength=${payload.length}"
        }
        wsSession.send(Frame.Text(payload))
    }

    private suspend fun closeSession(reason: String, target: DefaultClientWebSocketSession? = session) {
        if (target == null) {
            KLogger.d(TAG) { "closeSession skipped: no session ($reason)" }
            return
        }
        target?.let { current ->
            KLogger.d(TAG) { "closeSession: reason=$reason isCurrent=${session === current}" }
            runCatching {
                current.close()
            }.onFailure {
                KLogger.w(TAG) { "close failed: $reason (${it.message})" }
            }
            if (session === current) {
                session = null
            }
        }
    }

    private fun logMissingProject(operation: String) {
        KLogger.w(TAG) { "$operation skipped: projectId not set" }
    }

    private fun setState(newState: MessengerSocket.ConnectionState, reason: String) {
        val oldState = _state.value
        _state.value = newState
        KLogger.d(TAG) { "state: ${describeState(oldState)} -> ${describeState(newState)} ($reason)" }
    }

    private fun describeAction(action: Action): String = when (action) {
        is Action.Unauthorized -> "Unauthorized"
        is Action.Messenger -> "Messenger.${describeMessengerAction(action)}"
        else -> action::class.simpleName ?: "UnknownAction"
    }

    private fun describeMessengerAction(action: Action.Messenger): String = when (action) {
        is Action.Messenger.SendChatsList -> "SendChatsList(chats=${action.chats.size}, senders=${action.senders.size})"
        is Action.Messenger.NewChat -> "NewChat(id=${action.chat.id})"
        is Action.Messenger.NewMessage -> "NewMessage(chatId=${action.chatId}, messageId=${action.message.id})"
        is Action.Messenger.UpdateChatUnreadCount -> "UpdateChatUnreadCount(chatId=${action.chatId}, count=${action.count})"
        is Action.Messenger.SendChatMessages -> {
            "SendChatMessages(chatId=${action.chatId}, read=${action.readMessages.size}, unread=${action.unreadMessages.size})"
        }
        is Action.Messenger.MessageReadRecorded -> {
            "MessageReadRecorded(chatId=${action.chatId}, messageId=${action.messageId})"
        }
    }

    private fun describeIntent(intent: Intent): String = when (intent) {
        is Intent.Authorize -> "Authorize(tokenLength=${intent.jwt.length})"
        is Intent.Messenger.Start -> "Messenger.Start(projectId=${intent.projectId})"
        is Intent.Messenger.RequestChatsList -> "Messenger.RequestChatsList(projectId=${intent.projectId})"
        is Intent.Messenger.RequestChatMessages -> "Messenger.RequestChatMessages(projectId=${intent.projectId}, chatId=${intent.chatId})"
        is Intent.Messenger.SendMessage -> "Messenger.SendMessage(projectId=${intent.projectId}, chatId=${intent.chatId}, textLength=${intent.message.length})"
        is Intent.Messenger.CreateChat -> "Messenger.CreateChat(projectId=${intent.projectId}, type=${intent.chatType}, title=${intent.chatTitle})"
        is Intent.Messenger.ReadMessage -> "Messenger.ReadMessage(projectId=${intent.projectId}, chatId=${intent.chatId}, messageId=${intent.messageId})"
        is Intent.Messenger.ReadMessagesBefore -> {
            "Messenger.ReadMessagesBefore(projectId=${intent.projectId}, chatId=${intent.chatId}, messageId=${intent.messageId})"
        }
        else -> intent::class.simpleName ?: "UnknownIntent"
    }

    private fun describeState(state: MessengerSocket.ConnectionState): String = when (state) {
        is MessengerSocket.ConnectionState.Authorized -> "Authorized(projectId=${state.projectId})"
        is MessengerSocket.ConnectionState.Connecting -> "Connecting(attempt=${state.attempt})"
        is MessengerSocket.ConnectionState.Failed -> "Failed(reason=${state.reason})"
        MessengerSocket.ConnectionState.AuthRequired -> "AuthRequired"
        MessengerSocket.ConnectionState.Connected -> "Connected"
        MessengerSocket.ConnectionState.Disconnected -> "Disconnected"
        MessengerSocket.ConnectionState.Idle -> "Idle"
    }

    private fun extractType(raw: String): String {
        val key = "\"type\":\""
        val start = raw.indexOf(key)
        if (start == -1) return "unknown"
        val valueStart = start + key.length
        val end = raw.indexOf('"', valueStart)
        return if (end == -1) "unknown" else raw.substring(valueStart, end)
    }

    private fun toWebSocketBaseUrl(url: String): String {
        val trimmed = url.trimEnd('/')
        return when {
            trimmed.startsWith("wss://") || trimmed.startsWith("ws://") -> trimmed
            trimmed.startsWith("https://") -> "wss://${trimmed.removePrefix("https://")}"
            trimmed.startsWith("http://") -> "ws://${trimmed.removePrefix("http://")}"
            else -> "ws://$trimmed"
        }
    }

    private companion object {
        private const val TAG = "MessengerSocketClient"
        private const val PROJECT_PATH = "/project"
        private const val OUTGOING_BUFFER = 32
        private const val ACTIONS_BUFFER = 32
        private const val RECONNECT_DELAY_MS = 1_000L
        private const val AUTH_RETRY_DELAY_MS = 2_000L
        private const val MAX_RECONNECT_DELAY_MS = 10_000L

        private val socketJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            classDiscriminator = "type"
        }
    }
}
