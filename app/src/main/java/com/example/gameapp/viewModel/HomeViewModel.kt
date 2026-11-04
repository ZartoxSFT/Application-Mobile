package com.example.gameapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameapp.controller.BluetoothController
import com.example.gameapp.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoggedIn: Boolean = false,
    val username: String? = null
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val bluetoothController: BluetoothController
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = authRepository.currentUserEmail
        .map { email -> HomeUiState(isLoggedIn = email != null, username = email) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = authRepository.currentUserEmail.value.let {
                HomeUiState(isLoggedIn = it != null, username = it)
            }
        )

    fun logout() {
        // Results can't be saved without a user, so drop the controller connection as well.
        bluetoothController.disconnect()
        authRepository.logout()
    }
}
