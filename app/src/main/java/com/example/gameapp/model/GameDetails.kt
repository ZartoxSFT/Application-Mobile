package com.example.gameapp.model

data class PlayerParticipationDetails(
    val player: Player = Player(),
    val participate: Participate = Participate()
)

data class GameDetails(
    val game: Game = Game(),
    val ownerUsername: String = "",
    val invitedPlayersWithDetails: List<PlayerParticipationDetails> = emptyList()
)
