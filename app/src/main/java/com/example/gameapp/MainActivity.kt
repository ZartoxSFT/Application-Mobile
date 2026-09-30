package com.example.gameapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.gameapp.navigation.Routes
import com.example.gameapp.ui.theme.GameAppTheme
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
                                HomeScreen()
                            }

                            is Routes.HistoricScreen -> NavEntry(key) {
                                HomeScreen()
                            }

                            Routes.PlayScreen -> NavEntry(key) {
                                HomeScreen()
                            }
                        }
                    }
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Log.d("GameApp", innerPadding.toString())
                }
            }
        }
    }
}