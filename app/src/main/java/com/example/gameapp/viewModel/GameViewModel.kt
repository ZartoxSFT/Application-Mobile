package com.example.gameapp.viewModel

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameapp.controller.BluetoothController
import com.example.gameapp.model.GameResult
import com.example.gameapp.model.InvitedPlayerInput
import com.example.gameapp.repository.GameRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Values typed in the "Save game result" dialog. */
data class ResultFormState(
    val gameType: String = "Snake",
    val colourOwner: String = "Red",
    val ownerScore: String = "100",
    val isMultiplayer: Boolean = false,
    val invitedUsername: String = "",
    val invitedColour: String = "Blue",
    val invitedScore: String = "80",
    val winner: String = ""
)

class GameViewModel(
    private val bluetoothController: BluetoothController,
    private val gameRepository: GameRepository
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = bluetoothController.isConnected
    val isConnecting: StateFlow<Boolean> = bluetoothController.isConnecting
    val connectedDeviceName: StateFlow<String?> = bluetoothController.connectedDeviceName
    val connectionStatus: StateFlow<String> = bluetoothController.connectionStatus
    val scannedDevices: StateFlow<List<BluetoothDevice>> = bluetoothController.scannedDevices
    val isScanning: StateFlow<Boolean> = bluetoothController.isScanning
    val receivedMessage: StateFlow<String?> = bluetoothController.receivedMessage

    val isBluetoothSupported: Boolean get() = bluetoothController.isSupported
    fun isBluetoothEnabled(): Boolean = bluetoothController.isEnabled()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _showDevicePicker = MutableStateFlow(false)
    val showDevicePicker: StateFlow<Boolean> = _showDevicePicker.asStateFlow()

    private val _showResultDialog = MutableStateFlow(false)
    val showResultDialog: StateFlow<Boolean> = _showResultDialog.asStateFlow()

    private val _resultForm = MutableStateFlow(ResultFormState())
    val resultForm: StateFlow<ResultFormState> = _resultForm.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _messageText = MutableStateFlow("Hello ESP32")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    /** Live scores (player 1, player 2) sent by the ESP32 as "SCORE:<J1>,<J2>". */
    private val _liveScores = MutableStateFlow<Pair<Int, Int>?>(null)
    val liveScores: StateFlow<Pair<Int, Int>?> = _liveScores.asStateFlow()

    /** Short user-facing messages (displayed as toasts by the screen). */
    private val _userMessages = Channel<String>(Channel.BUFFERED)
    val userMessages = _userMessages.receiveAsFlow()

    init {
        // Automatically listen to Bluetooth messages from ESP-32 Feather
        viewModelScope.launch {
            receivedMessage.collect { message ->
                message?.let {
                    if (!parseLiveScores(it)) parseAndSaveGameResultFromBluetooth(it)
                }
            }
        }
    }

    private fun parseLiveScores(message: String): Boolean {
        val trimmed = message.trim()
        if (!trimmed.startsWith("SCORE:")) return false
        val scores = trimmed.substringAfter("SCORE:").split(",").map { it.trim().toIntOrNull() }
        if (scores.size == 2 && scores[0] != null && scores[1] != null) {
            _liveScores.value = scores[0]!! to scores[1]!!
        }
        return true
    }

    // region Device picker
    fun openDevicePicker() {
        _pairedDevices.value = bluetoothController.getPairedDevices()
        _showDevicePicker.value = true
        bluetoothController.startDiscovery()
    }

    fun closeDevicePicker() {
        bluetoothController.stopDiscovery()
        _showDevicePicker.value = false
    }

    fun rescan() {
        bluetoothController.startDiscovery()
    }

    fun connectToDevice(device: BluetoothDevice) {
        closeDevicePicker()
        bluetoothController.connectToDevice(device)
    }

    fun stopDiscovery() = bluetoothController.stopDiscovery()

    fun disconnect() = bluetoothController.disconnect()
    // endregion

    // region Messages
    fun onMessageTextChange(value: String) {
        _messageText.value = value
    }

    fun sendMessage() {
        val success = bluetoothController.sendMessage(_messageText.value)
        notify(if (success) "Message sent!" else "Failed to send message")
    }

    fun sendDirection(direction: String) {
        if (!bluetoothController.sendMessage(direction)) {
            notify("Failed to send $direction")
        }
    }
    // endregion

    // region Result form
    fun openResultDialog() {
        _showResultDialog.value = true
    }

    fun closeResultDialog() {
        _showResultDialog.value = false
    }

    fun updateResultForm(transform: (ResultFormState) -> ResultFormState) {
        _resultForm.update(transform)
    }

    fun submitResultForm() {
        val form = _resultForm.value
        val invitedPlayers = if (form.isMultiplayer && form.invitedUsername.isNotBlank()) {
            listOf(
                InvitedPlayerInput(
                    username = form.invitedUsername.trim(),
                    colourPlayer = form.invitedColour.trim(),
                    scorePlayer = form.invitedScore.toIntOrNull() ?: 0
                )
            )
        } else {
            emptyList()
        }

        saveGameResult(
            gameType = form.gameType.trim(),
            colourOwner = form.colourOwner.trim(),
            score = form.ownerScore.toIntOrNull() ?: 0,
            winner = if (form.isMultiplayer) form.winner.trim().ifBlank { null } else null,
            invitedPlayers = invitedPlayers
        )
        _showResultDialog.value = false
    }
    // endregion

    private fun notify(message: String) {
        _userMessages.trySend(message)
    }

    private fun saveGameResult(
        gameType: String,
        colourOwner: String,
        score: Int,
        winner: String?,
        invitedPlayers: List<InvitedPlayerInput> = emptyList()
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val gameResult = GameResult(
                gameType = gameType,
                colourOwner = colourOwner,
                score = score,
                winner = if (invitedPlayers.isEmpty()) null else winner,
                invitedPlayers = invitedPlayers
            )
            gameRepository.saveGameResult(gameResult)
                .onSuccess {
                    notify("Game result saved!")
                    bluetoothController.clearReceivedMessage()
                }
                .onFailure { exception ->
                    notify("Error saving result: ${exception.localizedMessage ?: "Save failed"}")
                }
            _isSaving.value = false
        }
    }

    private fun parseAndSaveGameResultFromBluetooth(message: String) {
        val trimmed = message.trim()
        if (trimmed.startsWith("{")) {
            // Attempt JSON parsing from ESP-32
            try {
                val json = JSONObject(trimmed)
                val type = json.optString("type", "Snake")
                val score = json.optInt("score", 0)
                val colourOwner = json.optString("colourOwner", "Red")
                val winner = if (json.has("winner") && !json.isNull("winner")) json.optString("winner") else null

                val invitedList = mutableListOf<InvitedPlayerInput>()
                if (json.has("players")) {
                    val playersArr = json.getJSONArray("players")
                    for (i in 0 until playersArr.length()) {
                        val pObj = playersArr.getJSONObject(i)
                        invitedList.add(
                            InvitedPlayerInput(
                                username = pObj.optString("username", "Guest"),
                                colourPlayer = pObj.optString("colourPlayer", "Blue"),
                                scorePlayer = pObj.optInt("scorePlayer", 0)
                            )
                        )
                    }
                }

                saveGameResult(
                    gameType = type,
                    colourOwner = colourOwner,
                    score = score,
                    winner = winner,
                    invitedPlayers = invitedList
                )
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error parsing JSON game result: ${e.message}")
            }
        } else if (trimmed.startsWith("RESULT:") || trimmed.startsWith("GAME_OVER:")) {
            // Key-value formatted string parsing
            try {
                val content = trimmed.substringAfter(":")
                val parts = content.split(";").associate { part ->
                    val kv = part.split("=")
                    if (kv.size == 2) kv[0].trim() to kv[1].trim() else "" to ""
                }

                val type = parts["type"] ?: "Snake"
                val score = parts["score"]?.toIntOrNull() ?: 0
                val colourOwner = parts["colourOwner"] ?: "Red"
                val winnerRaw = parts["winner"]
                val winner = if (winnerRaw.isNullOrBlankOrNullString()) null else winnerRaw

                saveGameResult(
                    gameType = type,
                    colourOwner = colourOwner,
                    score = score,
                    winner = winner,
                    invitedPlayers = emptyList()
                )
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error parsing string game result: ${e.message}")
            }
        }
    }

    private fun String?.isNullOrBlankOrNullString(): Boolean {
        return this.isNullOrBlank() || this.equals("null", ignoreCase = true)
    }
}
