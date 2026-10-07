package com.example.gameapp.dependencyInjection

import com.example.gameapp.viewModel.ConnectionViewModel
import com.example.gameapp.viewModel.GameViewModel
import com.example.gameapp.viewModel.HomeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val AppModule = module {
    single<FirebaseAuth> { Firebase.auth }
    viewModelOf(::HomeViewModel)
    viewModelOf(::GameViewModel)
    viewModelOf(::ConnectionViewModel)
}