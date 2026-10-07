package com.example.gameapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ConnectionViewModel(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun login(email: String, password: String, onLoginSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "At least one field is empty"
            return
        }

        viewModelScope.launch {
            _errorMessage.value = null

            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                onLoginSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed authentication"
            }
        }
    }

    fun create(email: String, password: String, onCreateSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "At least one field is empty"
            return
        }

        viewModelScope.launch {
            _errorMessage.value = null

            try {
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                onCreateSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error creating user"
            }
        }
    }
}