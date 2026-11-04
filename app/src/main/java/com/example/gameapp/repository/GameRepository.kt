package com.example.gameapp.repository

import com.example.gameapp.model.Game
import com.example.gameapp.model.GameDetails
import com.example.gameapp.model.GameResult
import com.example.gameapp.model.Participate
import com.example.gameapp.model.Player
import com.example.gameapp.model.PlayerParticipationDetails
import com.example.gameapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GameRepository(
    realtimeDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) {
    private val rootRef = realtimeDatabase.reference
    private val usersRef = rootRef.child("users")
    private val gamesRef = rootRef.child("games")
    private val playersRef = rootRef.child("players")
    private val participatesRef = rootRef.child("participates")

    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    val currentUsername: String
        get() = firebaseAuth.currentUser?.email?.substringBefore("@")
            ?: firebaseAuth.currentUser?.displayName
            ?: "User"

    suspend fun saveGameResult(result: GameResult): Result<String> {
        val ownerId = currentUserId ?: return Result.failure(Exception("User not connected"))

        return try {
            // 1. Ensure current user node exists in Realtime Database 'users'
            val userSnapshot = usersRef.child(ownerId).get().await()
            if (!userSnapshot.exists()) {
                val user = User(
                    idUser = ownerId,
                    username = currentUsername,
                    password = ""
                )
                usersRef.child(ownerId).setValue(user).await()
            }

            // 2. Save Game in 'games' node
            val newGameRef = gamesRef.push()
            val idGame = newGameRef.key ?: UUID.randomUUID().toString()
            val game = Game(
                idGame = idGame,
                type = result.gameType,
                colourOwner = result.colourOwner,
                score = result.score,
                winner = if (result.invitedPlayers.isEmpty()) null else result.winner,
                owner = ownerId
            )
            newGameRef.setValue(game).await()

            // 3. Save invited players in 'players' node & 'participates' node
            for (invited in result.invitedPlayers) {
                if (invited.username.isNotBlank()) {
                    val idPlayer = UUID.randomUUID().toString()
                    val player = Player(
                        idPlayer = idPlayer,
                        username = invited.username
                    )
                    playersRef.child(idPlayer).setValue(player).await()

                    val participate = Participate(
                        game = idGame,
                        player = idPlayer,
                        colourPlayer = invited.colourPlayer,
                        scorePlayer = invited.scorePlayer
                    )
                    participatesRef.push().setValue(participate).await()
                }
            }

            Result.success(idGame)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserGames(): Result<List<Game>> {
        val ownerId = currentUserId ?: return Result.failure(Exception("User not connected"))
        return try {
            val snapshot = gamesRef.orderByChild("owner").equalTo(ownerId).get().await()
            val gamesList = mutableListOf<Game>()
            for (child in snapshot.children) {
                val game = child.getValue(Game::class.java)
                if (game != null) {
                    gamesList.add(game)
                }
            }
            Result.success(gamesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGameDetails(idGame: String): Result<GameDetails> {
        return try {
            val gameSnapshot = gamesRef.child(idGame).get().await()
            val game = gameSnapshot.getValue(Game::class.java)
                ?: return Result.failure(Exception("Game not found"))

            val participatesSnapshot =
                participatesRef.orderByChild("game").equalTo(idGame).get().await()
            val playerDetailsList = mutableListOf<PlayerParticipationDetails>()

            for (child in participatesSnapshot.children) {
                val part = child.getValue(Participate::class.java)
                if (part != null && part.player.isNotBlank()) {
                    val playerSnapshot = playersRef.child(part.player).get().await()
                    val player = playerSnapshot.getValue(Player::class.java)
                    if (player != null) {
                        playerDetailsList.add(
                            PlayerParticipationDetails(
                                player = player,
                                participate = part
                            )
                        )
                    }
                }
            }

            Result.success(
                GameDetails(
                    game = game,
                    ownerUsername = currentUsername,
                    invitedPlayersWithDetails = playerDetailsList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
