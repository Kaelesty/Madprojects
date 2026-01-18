package ru.kaelesty.madprojects.features.project.domain

import entities.Action
import entities.ChatType
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MessengerSocket {

    val state: StateFlow<ConnectionState>

    val actions: SharedFlow<Action.Messenger>

    fun connect(projectId: Int)

    fun disconnect()

    suspend fun requestChatsList()

    suspend fun requestChatMessages(chatId: Int)

    suspend fun sendMessage(chatId: Int, message: String)

    suspend fun createChat(title: String, chatType: ChatType)

    suspend fun readMessage(messageId: Int, chatId: Int)

    suspend fun readMessagesBefore(messageId: Int, chatId: Int)

    sealed interface ConnectionState {
        data object Idle : ConnectionState
        data class Connecting(val attempt: Int) : ConnectionState
        data object Connected : ConnectionState
        data class Authorized(val projectId: Int) : ConnectionState
        data object AuthRequired : ConnectionState
        data object Disconnected : ConnectionState
        data class Failed(val reason: String) : ConnectionState
    }
}
