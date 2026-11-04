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
import com.example.gameapp.view.HistoricScreen
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

                // Avoid pushing the same screen twice on a double tap
                fun navigate(route: Routes) {
                    if (backStack.lastOrNull() != route) backStack.add(route)
                }

                fun goBack() {
                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                }

                NavDisplay(
                    backStack = backStack,
                    onBack = { goBack() },
                    entryProvider = { key ->
                        when (key) {
                            Routes.HomeScreen -> NavEntry(key) {
                                HomeScreen(
                                    navigateToGame = { navigate(Routes.GameScreen) },
                                    navigateToHistoric = { navigate(Routes.HistoricScreen) },
                                    navigateToConnection = { navigate(Routes.ConnectionScreen) }
                                )
                            }

                            Routes.GameScreen -> NavEntry(key) {
                                GameScreen(onBack = { goBack() })
                            }

                            Routes.ConnectionScreen -> NavEntry(key) {
                                ConnectionScreen(
                                    onLoginSuccess = { goBack() },
                                    onBack = { goBack() }
                                )
                            }

                            Routes.HistoricScreen -> NavEntry(key) {
                                HistoricScreen(onBack = { goBack() })
                            }
                        }
                    }
                )
            }
        }
    }
}
