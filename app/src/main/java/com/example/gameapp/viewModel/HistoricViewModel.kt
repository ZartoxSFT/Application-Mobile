package com.example.gameapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameapp.model.Game
import com.example.gameapp.model.GameDetails
import com.example.gameapp.repository.AuthRepository
import com.example.gameapp.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoricViewModel(
    private val gameRepository: GameRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedGameDetails = MutableStateFlow<GameDetails?>(null)
    val selectedGameDetails: StateFlow<GameDetails?> = _selectedGameDetails.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Reload (or clear) the history whenever the logged-in user changes,
        // so a new user never sees the previous user's games.
        viewModelScope.launch {
            authRepository.currentUserEmail.collect { email ->
                _selectedGameDetails.value = null
                if (email == null) {
                    _games.value = emptyList()
                } else {
                    loadUserGames()
                }
            }
        }
    }

    fun loadUserGames() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            gameRepository.getUserGames()
                .onSuccess { list -> _games.value = list.reversed() } // newest first
                .onFailure { ex ->
                    _errorMessage.value = ex.localizedMessage ?: "Failed to load game history"
                }
            _isLoading.value = false
        }
    }

    fun selectGame(idGame: String) {
        viewModelScope.launch {
            _isLoading.value = true
            gameRepository.getGameDetails(idGame)
                .onSuccess { details -> _selectedGameDetails.value = details }
                .onFailure { ex ->
                    _errorMessage.value = ex.localizedMessage ?: "Failed to load game details"
                }
            _isLoading.value = false
        }
    }

    fun clearSelectedGame() {
        _selectedGameDetails.value = null
    }
}
