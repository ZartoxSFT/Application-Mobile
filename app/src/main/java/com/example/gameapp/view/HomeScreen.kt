package com.example.gameapp.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gameapp.viewModel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    homeVM: HomeViewModel = koinViewModel(),
    navigateToGame: () -> Unit,
    navigateToHistoric: () -> Unit,
    navigateToConnection: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (homeVM.isConnected()) {
                Button(onClick = {
                    navigateToGame()
                }) {
                    Text("Game")
                }
                Button(onClick = {
                    navigateToHistoric()
                }) {
                    Text("Historic")
                }
            } else {
                Button(onClick = {
                    navigateToConnection()
                }) {
                    Text("Connection")
                }
            }
        }
    }
}