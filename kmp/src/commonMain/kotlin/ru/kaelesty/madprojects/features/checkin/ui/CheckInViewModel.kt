package ru.kaelesty.madprojects.features.checkin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import ru.kaelesty.madprojects.features.checkin.domain.CheckInUseCase

class CheckInViewModel(
    private val useCase: CheckInUseCase,
) : ViewModel() {

    sealed interface State {
        data object Checking : State
        data object Failed : State
    }

    sealed interface Event {
        data object Connected : Event
    }

    private val _state = MutableStateFlow<State>(State.Checking)
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private var checkJob: Job? = null

    init {
        check()
    }

    fun check() {
        if (checkJob?.isActive == true) return
        _state.value = State.Checking
        checkJob = viewModelScope.launch {
            val ok = useCase.check()
            if (ok) {
                _events.emit(Event.Connected)
            } else {
                _state.value = State.Failed
            }
        }
    }
}
