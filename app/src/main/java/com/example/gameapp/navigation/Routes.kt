package com.example.gameapp.navigation

sealed interface Routes {
    data object HomeScreen : Routes
    data class HistoricScreen(val user: String?) : Routes
    data object GameScreen : Routes
    data object ConnectionScreen : Routes
}