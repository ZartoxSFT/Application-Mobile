package com.example.gameapp.model

data class InvitedPlayerInput(
    val username: String = "",
    val colourPlayer: String = "",
    val scorePlayer: Int = 0
)

data class GameResult(
    val gameType: String = "Snake",
    val colourOwner: String = "Red",
    val score: Int = 0,
    val winner: String? = null,
    val invitedPlayers: List<InvitedPlayerInput> = emptyList()
)
