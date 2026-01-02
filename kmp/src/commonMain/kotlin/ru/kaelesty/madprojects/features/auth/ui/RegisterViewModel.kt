package ru.kaelesty.madprojects.features.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.auth.domain.RegisterUseCase
import ru.kaelesty.madprojects.features.auth.domain.UserType

class RegisterViewModel(
    private val useCase: RegisterUseCase,
) : ViewModel() {

    data class State(
        val username: String = "",
        val lastName: String = "",
        val firstName: String = "",
        val secondName: String = "",
        val data: String = "",
        val email: String = "",
        val password: String = "",
        val userType: UserType = UserType.Common,
        val isFirstPage: Boolean = true,
        val error: ValidationError? = null,
    )

    sealed interface ValidationError {
        data object UsernameTooShort : ValidationError
        data object InvalidLastName : ValidationError
        data object InvalidFirstName : ValidationError
        data object InvalidSecondName : ValidationError
        data object InvalidGroup : ValidationError
        data object InvalidPosition : ValidationError
        data object EmptyEmail : ValidationError
        data object InvalidEmail : ValidationError
        data object EmptyPassword : ValidationError
        data object InvalidPassword : ValidationError
        data object EmailTaken : ValidationError
        data object UsernameTaken : ValidationError
        data object WeakPassword : ValidationError
        data object Unavailable : ValidationError
    }

    sealed interface Event {
        data object Successful : Event
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\\\$%^&*()_+\\-\\[\\]{};':\"\\\\|,.<>/?]).{10,}$")
    private val nameRegex = Regex("^[А-ЯЁа-яё]{2,}$")
    private val groupRegex = Regex("^(?:[А-ЯЁа-яё]{0,3}\\d{2,4}[А-ЯЁа-яё]{0,3})$")

    fun setUsername(value: String) = update { it.copy(username = value) }

    fun setLastName(value: String) = update { it.copy(lastName = value) }

    fun setFirstName(value: String) = update { it.copy(firstName = value) }

    fun setSecondName(value: String) = update { it.copy(secondName = value) }

    fun setData(value: String) = update { it.copy(data = value) }

    fun setEmail(value: String) = update { it.copy(email = value) }

    fun setPassword(value: String) = update { it.copy(password = value) }

    fun setUserType(value: UserType) = update { it.copy(userType = value) }

    fun nextPage() {
        val error = validateFirstPage(state.value)
        if (error != null) {
            updateError(error)
            return
        }
        update { it.copy(isFirstPage = false) }
    }

    fun previousPage() {
        update { it.copy(isFirstPage = true) }
    }

    fun submit() {
        val current = state.value
        val firstError = validateFirstPage(current)
        if (firstError != null) {
            _state.update { it.copy(isFirstPage = true, error = firstError) }
            return
        }

        val secondError = validateSecondPage(current)
        if (secondError != null) {
            updateError(secondError)
            return
        }

        viewModelScope.launch {
            when (
                useCase.register(
                    username = current.username.trim(),
                    lastName = current.lastName.trim(),
                    firstName = current.firstName.trim(),
                    secondName = current.secondName.trim(),
                    data = current.data.trim(),
                    email = current.email.trim(),
                    password = current.password,
                    userType = current.userType,
                )
            ) {
                RegisterUseCase.Result.Success -> _events.emit(Event.Successful)
                RegisterUseCase.Result.EmailTaken -> updateError(ValidationError.EmailTaken)
                RegisterUseCase.Result.UsernameTaken -> updateError(ValidationError.UsernameTaken)
                RegisterUseCase.Result.WeakPassword -> updateError(ValidationError.WeakPassword)
                RegisterUseCase.Result.Unavailable -> updateError(ValidationError.Unavailable)
            }
        }
    }

    private fun validateFirstPage(current: State): ValidationError? {
        val username = current.username.trim()
        if (username.length < 3) return ValidationError.UsernameTooShort

        val lastName = current.lastName.trim()
        if (!nameRegex.matches(lastName)) return ValidationError.InvalidLastName

        val firstName = current.firstName.trim()
        if (!nameRegex.matches(firstName)) return ValidationError.InvalidFirstName

        val secondName = current.secondName.trim()
        if (!nameRegex.matches(secondName)) return ValidationError.InvalidSecondName

        return null
    }

    private fun validateSecondPage(current: State): ValidationError? {
        val data = current.data.trim()
        if (current.userType == UserType.Common) {
            if (!groupRegex.matches(data)) return ValidationError.InvalidGroup
        } else if (!nameRegex.matches(data)) {
            return ValidationError.InvalidPosition
        }

        val email = current.email.trim()
        if (email.isBlank()) return ValidationError.EmptyEmail
        if (!emailRegex.matches(email)) return ValidationError.InvalidEmail

        val password = current.password
        if (password.isBlank()) return ValidationError.EmptyPassword
        if (!passwordRegex.matches(password)) return ValidationError.InvalidPassword

        return null
    }

    private fun update(update: (State) -> State) {
        _state.update { update(it).copy(error = null) }
    }

    private fun updateError(error: ValidationError) {
        _state.update { it.copy(error = error) }
    }
}

