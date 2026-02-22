package app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object LogManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val storage = MutableStateFlow<List<String>>(listOf())
    private val counter = MutableStateFlow<Int>(0)

    fun emitOutput(projectId: Int, message: String, isSuccess: Boolean) {
        scope.launch {
            val newValue = storage.value
                .toMutableList()
                .apply {
                    add("${counter.value}: OUTPUT(${if (isSuccess) "DROPPED" else "SENT"}) \t ProjectSession[$projectId]: \t $message")
                }
                .toList()
            counter.emit(counter.value + 1)
            storage.emit(
                if (newValue.size >= 100) {
                    newValue.takeLast(100)
                }
                else {
                    newValue
                }
            )
        }
    }

    fun emitInput(projectId: Int, message: String) {
        scope.launch {
            val newValue = storage.value
                .toMutableList()
                .apply {
                    add("${counter.value}: INPUT \t ProjectSession[$projectId]: \t $message")
                }
                .toList()
            counter.emit(counter.value + 1)
            storage.emit(
                if (newValue.size >= 100) {
                    newValue.takeLast(100)
                }
                else {
                    newValue
                }
            )
        }
    }

    fun emitError(message: String) {
        System.err.println("LogManager ERROR: $message")
        scope.launch {
            val newValue = storage.value
                .toMutableList()
                .apply {
                    add("${counter.value}: ERROR \t $message")
                }
                .toList()
            counter.emit(counter.value + 1)
            storage.emit(
                if (newValue.size >= 100) {
                    newValue.takeLast(100)
                }
                else {
                    newValue
                }
            )
        }
    }

    fun get(): List<String> {
        return storage.value
    }
}
