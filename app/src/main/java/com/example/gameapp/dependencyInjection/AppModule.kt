package com.example.gameapp.dependencyInjection

import android.bluetooth.BluetoothManager
import androidx.core.content.ContextCompat
import com.example.gameapp.controller.BluetoothController
import com.example.gameapp.repository.AuthRepository
import com.example.gameapp.repository.GameRepository
import com.example.gameapp.viewModel.ConnectionViewModel
import com.example.gameapp.viewModel.GameViewModel
import com.example.gameapp.viewModel.HistoricViewModel
import com.example.gameapp.viewModel.HomeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val AppModule = module {
    single<FirebaseAuth> { Firebase.auth }
    single<FirebaseFirestore> { Firebase.firestore }
    single<FirebaseDatabase> {
        FirebaseDatabase.getInstance("https://gameapp-d11b8-default-rtdb.europe-west1.firebasedatabase.app")
    }
    single {
        val context = androidContext()
        val bluetoothManager = ContextCompat.getSystemService(context, BluetoothManager::class.java)
        BluetoothController(context, bluetoothManager?.adapter)
    }
    singleOf(::AuthRepository)
    singleOf(::GameRepository)
    viewModelOf(::HomeViewModel)
    viewModelOf(::GameViewModel)
    viewModelOf(::ConnectionViewModel)
    viewModelOf(::HistoricViewModel)
}
