package com.example.gameapp.model

data class Game(
    val idGame: String = "",
    val type: String = "",
    val colourOwner: String = "",
    val score: Int = 0,
    val winner: String? = null,
    val owner: String = ""
)
