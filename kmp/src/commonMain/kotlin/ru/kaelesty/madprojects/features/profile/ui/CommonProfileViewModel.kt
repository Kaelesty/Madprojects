package ru.kaelesty.madprojects.features.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import domain.profile.CommonProfileResponse
import ru.kaelesty.madprojects.features.profile.domain.GetCommonProfileUseCase

class CommonProfileViewModel(
    private val useCase: GetCommonProfileUseCase,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(val profile: CommonProfileResponse) : State
        data object Error : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = State.Loading
        viewModelScope.launch {
            when (val result = useCase.load()) {
                is GetCommonProfileUseCase.Result.Success -> _state.value = State.Loaded(result.profile)
                GetCommonProfileUseCase.Result.Fail -> _state.value = State.Error
            }
        }
    }
}
