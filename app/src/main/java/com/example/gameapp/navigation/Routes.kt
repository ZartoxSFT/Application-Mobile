package com.example.gameapp.navigation

sealed interface Routes {
    data object HomeScreen : Routes
    data object HistoricScreen : Routes
    data object GameScreen : Routes
    data object ConnectionScreen : Routes
}
