package com.example.gameapp.viewModel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class GameViewModel(
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    fun isConnected(): Boolean {
        val currentUser = firebaseAuth.currentUser
        return currentUser != null
    }
}