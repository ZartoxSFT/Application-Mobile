package com.example.gameapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameapp.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectionUiState(
    val email: String = "",
    val password: String = "",
    val isCreatingAccount: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/** One-shot events the screen reacts to (toast / navigation). */
sealed interface ConnectionEvent {
    data object LoggedIn : ConnectionEvent
    data object AccountCreated : ConnectionEvent
}

class ConnectionViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    private val _events = Channel<ConnectionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun setCreatingAccount(creating: Boolean) =
        _uiState.update { it.copy(isCreatingAccount = creating, errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in both fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = if (state.isCreatingAccount) {
                authRepository.register(state.email, state.password)
            } else {
                authRepository.login(state.email, state.password)
            }

            result.onSuccess {
                if (state.isCreatingAccount) {
                    // Keep the email, switch back to login mode.
                    _uiState.update {
                        it.copy(isLoading = false, isCreatingAccount = false, password = "")
                    }
                    _events.send(ConnectionEvent.AccountCreated)
                } else {
                    _uiState.update { ConnectionUiState() }
                    _events.send(ConnectionEvent.LoggedIn)
                }
            }.onFailure { e ->
                val fallback = if (state.isCreatingAccount) "Error creating user" else "Failed authentication"
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: fallback)
                }
            }
        }
    }
}
