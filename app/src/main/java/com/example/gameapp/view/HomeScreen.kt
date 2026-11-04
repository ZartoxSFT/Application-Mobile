package com.example.gameapp.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gameapp.component.SectionCard
import com.example.gameapp.viewModel.HomeUiState
import com.example.gameapp.viewModel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    homeVM: HomeViewModel = koinViewModel(),
    navigateToGame: () -> Unit,
    navigateToHistoric: () -> Unit,
    navigateToConnection: () -> Unit,
) {
    val uiState by homeVM.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onPlay = navigateToGame,
        onHistory = navigateToHistoric,
        onLogin = navigateToConnection,
        onLogout = homeVM::logout
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onPlay: () -> Unit,
    onHistory: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🎮", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "GameApp",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Bluetooth controller & game companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            if (uiState.isLoggedIn) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (uiState.username?.firstOrNull() ?: 'U').uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Signed in as",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.username ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                HomeButton(text = "Play / Controller", emoji = "🕹️", onClick = onPlay)
                Spacer(modifier = Modifier.height(12.dp))
                HomeButton(text = "Game history", emoji = "📜", onClick = onHistory, filled = false)
                Spacer(modifier = Modifier.height(20.dp))

                TextButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Log out")
                }
            } else {
                SectionCard {
                    Text(
                        text = "Sign in to control your games over Bluetooth and keep track of your match history.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                HomeButton(text = "Log in / Register", emoji = "🔑", onClick = onLogin)
            }
        }
    }
}

@Composable
private fun HomeButton(
    text: String,
    emoji: String,
    onClick: () -> Unit,
    filled: Boolean = true
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
    val content: @Composable () -> Unit = {
        Text("$emoji  $text", style = MaterialTheme.typography.labelLarge)
    }
    if (filled) {
        Button(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium) {
            content()
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium
        ) {
            content()
        }
    }
}
