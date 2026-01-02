package ru.kaelesty.madprojects.features.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.auth.domain.LoginUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources

class LoginViewModel(
    private val useCase: LoginUseCase,
    private val str: StringResources = StringResources,
): ViewModel() {

    data class State(
        val email: String = "",
        val password: String = "",
        val errorMessage: String? = null,
    )

    sealed interface Event {
        data object Successful: Event
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\\\$%^&*()_+\\-\\[\\]{};':\"\\\\|,.<>/?]).{10,}$")

    fun setEmail(value: String) {
        _state.update { it.copy(email = value, errorMessage = null) }
    }

    fun setPassword(value: String) {
        _state.update { it.copy(password = value, errorMessage = null) }
    }

    fun submit() {
        val email = state.value.email.trim()
        val password = state.value.password

        if (email.isBlank()) {
            _state.update { it.copy(errorMessage = str.ErrorEmptyEmail) }
            return
        }
        if (!emailRegex.matches(email)) {
            _state.update { it.copy(errorMessage = str.ErrorInvalidEmail) }
            return
        }
        if (password.isBlank()) {
            _state.update { it.copy(errorMessage = str.ErrorEmptyPassword) }
            return
        }
        if (!passwordRegex.matches(password)) {
            _state.update { it.copy(errorMessage = str.ErrorInvalidPasswordPolicy) }
            return
        }

        viewModelScope.launch {
            when (useCase.login(email, password)) {
                LoginUseCase.Result.Success -> _events.emit(Event.Successful)
                LoginUseCase.Result.Fail -> _state.update { it.copy(errorMessage = str.ErrorLogin) }
                LoginUseCase.Result.Unavailable -> _state.update { it.copy(errorMessage = str.ErrorUnavailable) }
            }
        }
    }
}