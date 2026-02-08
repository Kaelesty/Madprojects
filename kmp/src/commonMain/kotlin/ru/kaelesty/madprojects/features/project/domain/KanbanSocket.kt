package ru.kaelesty.madprojects.features.project.domain

import entities.Action
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface KanbanSocket {

    val state: StateFlow<ConnectionState>

    val actions: SharedFlow<Action.Kanban>

    fun connect(projectId: Int)

    fun disconnect()

    suspend fun requestKanban()

    suspend fun createColumn(name: String, color: String)

    suspend fun updateColumn(id: Int, name: String?, color: String?)

    suspend fun moveColumn(id: Int, newPosition: Int)

    suspend fun deleteColumn(id: Int)

    suspend fun createKard(name: String, desc: String, columnId: Int)

    suspend fun updateKard(id: Int, name: String?, desc: String?)

    suspend fun moveKard(id: Int, columnId: Int, newColumnId: Int, newPosition: Int)

    suspend fun deleteKard(id: Int)

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
