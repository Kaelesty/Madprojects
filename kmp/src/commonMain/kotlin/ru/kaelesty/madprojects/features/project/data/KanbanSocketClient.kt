package ru.kaelesty.madprojects.features.project.data

import entities.Action
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
import ru.kaelesty.madprojects.features.project.domain.KanbanSocket
import ru.kaelesty.madprojects.ktor.KtorConfig
import ru.kaelesty.madprojects.utils.KLogger

class KanbanSocketClient(
    private val client: HttpClient,
    private val authContext: AuthContext,
    private val baseUrl: String = KtorConfig.BaseUrl,
) : KanbanSocket {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val outgoing = MutableSharedFlow<Intent>(extraBufferCapacity = OUTGOING_BUFFER)
    private val _actions = MutableSharedFlow<Action.Kanban>(extraBufferCapacity = ACTIONS_BUFFER)
    private val _state = MutableStateFlow<KanbanSocket.ConnectionState>(KanbanSocket.ConnectionState.Idle)

    override val actions: SharedFlow<Action.Kanban> = _actions.asSharedFlow()
    override val state: StateFlow<KanbanSocket.ConnectionState> = _state.asStateFlow()

    private var connectJob: Job? = null
    private var activeProjectId: Int? = null
    private var session: DefaultClientWebSocketSession? = null

    override fun connect(projectId: Int) {
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
        KLogger.d(TAG) { "disconnect requested" }
        connectJob?.cancel()
        connectJob = null
        activeProjectId = null
        scope.launch {
            closeSession("client disconnect")
        }
        _state.value = KanbanSocket.ConnectionState.Idle
    }

    override suspend fun requestKanban() {
        val projectId = activeProjectId ?: return logMissingProject("requestKanban")
        sendIntent(Intent.Kanban.GetKanban(projectId))
    }

    override suspend fun createColumn(name: String, color: String) {
        val projectId = activeProjectId ?: return logMissingProject("createColumn")
        sendIntent(Intent.Kanban.CreateColumn(name = name, projectId = projectId, color = color))
    }

    override suspend fun updateColumn(id: Int, name: String?, color: String?) {
        val projectId = activeProjectId ?: return logMissingProject("updateColumn")
        sendIntent(Intent.Kanban.UpdateColumn(id = id, name = name, color = color, projectId = projectId))
    }

    override suspend fun moveColumn(id: Int, newPosition: Int) {
        val projectId = activeProjectId ?: return logMissingProject("moveColumn")
        sendIntent(Intent.Kanban.MoveColumn(id = id, newPosition = newPosition, projectId = projectId))
    }

    override suspend fun deleteColumn(id: Int) {
        val projectId = activeProjectId ?: return logMissingProject("deleteColumn")
        sendIntent(Intent.Kanban.DeleteColumn(id = id, projectId = projectId))
    }

    override suspend fun createKard(name: String, desc: String, columnId: Int) {
        val projectId = activeProjectId ?: return logMissingProject("createKard")
        sendIntent(Intent.Kanban.CreateKard(name = name, desc = desc, columnId = columnId, projectId = projectId))
    }

    override suspend fun updateKard(id: Int, name: String?, desc: String?) {
        val projectId = activeProjectId ?: return logMissingProject("updateKard")
        sendIntent(Intent.Kanban.UpdateKard(id = id, name = name, desc = desc, projectId = projectId))
    }

    override suspend fun moveKard(id: Int, columnId: Int, newColumnId: Int, newPosition: Int) {
        val projectId = activeProjectId ?: return logMissingProject("moveKard")
        sendIntent(
            Intent.Kanban.MoveKard(
                id = id,
                columnId = columnId,
                newColumnId = newColumnId,
                newPosition = newPosition,
                projectId = projectId
            )
        )
    }

    override suspend fun deleteKard(id: Int) {
        val projectId = activeProjectId ?: return logMissingProject("deleteKard")
        sendIntent(Intent.Kanban.DeleteKard(id = id, projectId = projectId))
    }

    private suspend fun connectionLoop(projectId: Int) {
        var attempt = 0
        var delayMs = RECONNECT_DELAY_MS
        while (currentCoroutineContext().isActive) {
            attempt += 1
            _state.value = KanbanSocket.ConnectionState.Connecting(attempt)
            val token = authContext.getAccessToken()
            if (token.isNullOrBlank()) {
                _state.value = KanbanSocket.ConnectionState.AuthRequired
                KLogger.w(TAG) { "connect skipped: no access token" }
                delay(AUTH_RETRY_DELAY_MS)
                continue
            }
            var wsSession: DefaultClientWebSocketSession? = null
            try {
                val url = "${toWebSocketBaseUrl(baseUrl)}$PROJECT_PATH"
                KLogger.d(TAG) { "connecting: url=$url attempt=$attempt" }
                wsSession = client.webSocketSession(urlString = url)
                session = wsSession
                _state.value = KanbanSocket.ConnectionState.Connected

                sendDirect(wsSession, Intent.Authorize(token))
                sendDirect(wsSession, Intent.Kanban.Start(projectId))
                _state.value = KanbanSocket.ConnectionState.Authorized(projectId)
                delayMs = RECONNECT_DELAY_MS

                coroutineScope {
                    val sender = launch { sendLoop(wsSession) }
                    val receiver = launch { receiveLoop(wsSession) }
                    receiver.join()
                    sender.cancelAndJoin()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                KLogger.e(TAG, e) { "socket failure" }
                _state.value = KanbanSocket.ConnectionState.Failed(e.message ?: "unknown")
            } finally {
                closeSession("connection closed", wsSession)
                if (currentCoroutineContext().isActive) {
                    _state.value = KanbanSocket.ConnectionState.Disconnected
                }
            }
            if (!currentCoroutineContext().isActive) break
            KLogger.d(TAG) { "reconnect scheduled in ${delayMs}ms" }
            delay(delayMs)
            delayMs = (delayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
        }
    }

    private suspend fun sendLoop(wsSession: DefaultClientWebSocketSession) {
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
        for (frame in wsSession.incoming) {
            if (frame !is Frame.Text) continue
            val text = frame.readText()
            val actionResult = runCatching { socketJson.decodeFromString<Action>(text) }
            if (actionResult.isFailure) {
                val error = actionResult.exceptionOrNull()
                KLogger.w(TAG) { "failed to decode action: ${error?.message}" }
                continue
            }
            val action = actionResult.getOrThrow()
            when (action) {
                is Action.Unauthorized -> {
                    KLogger.w(TAG) { "received unauthorized action" }
                    authContext.onUnauthorizedResponse()
                    closeSession("unauthorized")
                }
                is Action.Kanban -> _actions.emit(action)
                else -> Unit
            }
        }
    }

    private suspend fun sendIntent(intent: Intent) {
        if (_state.value !is KanbanSocket.ConnectionState.Authorized) {
            KLogger.w(TAG) { "send dropped: not connected (${intent::class.simpleName})" }
            return
        }
        if (!outgoing.tryEmit(intent)) {
            KLogger.w(TAG) { "send dropped: buffer full (${intent::class.simpleName})" }
        }
    }

    private suspend fun sendDirect(wsSession: DefaultClientWebSocketSession, intent: Intent) {
        val payload = socketJson.encodeToString(intent)
        wsSession.send(Frame.Text(payload))
    }

    private suspend fun closeSession(reason: String, target: DefaultClientWebSocketSession? = session) {
        target?.let { current ->
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
        private const val TAG = "KanbanSocketClient"
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
