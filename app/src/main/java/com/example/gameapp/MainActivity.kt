package com.example.gameapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.gameapp.navigation.Routes
import com.example.gameapp.ui.theme.GameAppTheme
import com.example.gameapp.view.ConnectionScreen
import com.example.gameapp.view.GameScreen
import com.example.gameapp.view.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameAppTheme {
                val backStack = remember {
                    mutableStateListOf<Routes>(
                        Routes.HomeScreen
                    )
                }

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = { key ->
                        when (key) {
                            Routes.HomeScreen -> NavEntry(key) {
                                HomeScreen(
                                    navigateToGame = { backStack.add(Routes.GameScreen) },
                                    navigateToHistoric = { backStack.add(Routes.HistoricScreen(null)) },
                                    navigateToConnection = { backStack.add(Routes.ConnectionScreen) }
                                )
                            }

                            Routes.GameScreen -> NavEntry(key) {
                                GameScreen()
                            }

                            Routes.ConnectionScreen -> NavEntry(key) {
                                ConnectionScreen(
                                    onLoginSuccess = {
                                        backStack.removeLastOrNull()
                                    }
                                )
                            }

                            is Routes.HistoricScreen -> NavEntry(key) {
                                GameScreen()
                            }
                        }
                    }
                )
            }
        }
    }
}