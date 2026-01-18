package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import ru.kaelesty.madprojects.features.project.domain.MessengerSocket
import ru.kaelesty.madprojects.utils.KLogger

class ProjectSocketViewModel(
    projectId: String,
    private val messengerSocket: MessengerSocket,
) : ViewModel() {

    init {
        val parsedId = projectId.toIntOrNull()
        if (parsedId == null) {
            KLogger.w(TAG) { "init skipped: invalid projectId=$projectId" }
        } else {
            KLogger.d(TAG) { "init: connect projectId=$parsedId" }
            messengerSocket.connect(parsedId)
        }
    }

    override fun onCleared() {
        KLogger.d(TAG) { "onCleared: disconnect" }
        messengerSocket.disconnect()
    }

    private companion object {
        private const val TAG = "ProjectSocketViewModel"
    }
}
