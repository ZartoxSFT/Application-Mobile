package com.example.gameapp.view

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.gameapp.viewModel.ConnectionViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ConnectionScreen(
    connectionVM: ConnectionViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isCreating by remember { mutableStateOf(false) }

    val errorMessage by connectionVM.errorMessage.collectAsState()

    fun onCreateSuccess(): () -> Unit = {
        Toast.makeText(context, "User created, please connect", Toast.LENGTH_SHORT)
            .show()
    }

    fun onLogin(): () -> Unit = {
        Toast.makeText(context, "Successfully connected", Toast.LENGTH_SHORT)
            .show()
        onLoginSuccess()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCreating,
                    onCheckedChange = { isCreating = it }
                )
                Text("Create an account")
            }
            Button(
                onClick = {
                    if (isCreating)
                        connectionVM.create(
                            email,
                            password,
                            onCreateSuccess()
                        )
                    else
                        connectionVM.login(
                            email,
                            password,
                            onLogin()
                        )
                }
            ) {
                Text(if (isCreating) "Create" else "Connect")
            }
        }
    }
}