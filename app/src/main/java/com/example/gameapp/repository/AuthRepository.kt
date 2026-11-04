package com.example.gameapp.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Single source of truth for authentication.
 * The [currentUserEmail] flow is updated by Firebase itself (AuthStateListener), so every
 * screen observing it refreshes automatically after a login or a logout.
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth
) {
    private val _currentUserEmail = MutableStateFlow(firebaseAuth.currentUser.displayLabel())
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUserEmail.value = auth.currentUser.displayLabel()
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        refresh()
    }

    suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        // Firebase signs the new user in automatically: sign out so they log in explicitly.
        firebaseAuth.signOut()
        refresh()
    }

    fun logout() {
        firebaseAuth.signOut()
        refresh()
    }

    private fun refresh() {
        _currentUserEmail.value = firebaseAuth.currentUser.displayLabel()
    }

    private fun com.google.firebase.auth.FirebaseUser?.displayLabel(): String? =
        this?.let { it.email ?: it.displayName ?: "User" }
}
