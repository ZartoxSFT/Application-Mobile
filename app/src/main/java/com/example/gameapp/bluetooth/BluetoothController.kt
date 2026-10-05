package com.example.gameapp.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class BluetoothController(private val bluetoothAdapter: BluetoothAdapter?) {

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // Standard SPP (Serial Port Profile) UUID for Bluetooth Serial (ESP32)
    private val sppUuid: UUID = UUID.fromString("00001101-0000-0000-0000-00805F9B34FB")

    /**
     * Start discovery for nearby Bluetooth devices.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (bluetoothAdapter == null) return false
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        _scannedDevices.value = emptyList()
        val started = bluetoothAdapter.startDiscovery()
        _isScanning.value = started
        Log.d("BluetoothController", "Start discovery: $started")
        return started
    }

    /**
     * Stop active Bluetooth device discovery.
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        _isScanning.value = false
    }

    fun onDiscoveryFinished() {
        _isScanning.value = false
        Log.d("BluetoothController", "Discovery finished")
    }

    /**
     * Add a newly discovered device to the scanned devices list.
     */
    @SuppressLint("MissingPermission")
    fun addScannedDevice(device: BluetoothDevice) {
        val currentList = _scannedDevices.value
        if (currentList.none { it.address == device.address }) {
            _scannedDevices.value = currentList + device
            Log.d("BluetoothController", "Discovered device: ${device.name ?: device.address}")
        }
    }

    /**
     * Get a list of paired/bonded Bluetooth devices.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Connect to a specific Bluetooth device (e.g. ESP-32-S3 Feather).
     */
    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice): Boolean {
        return withContext(Dispatchers.IO) {
            _isConnecting.value = true
            val deviceName = device.name ?: device.address
            _connectionStatus.value = "Connecting to $deviceName..."

            try {
                // Always cancel discovery prior to connecting; discovery degrades socket performance and causes timeouts
                bluetoothAdapter?.cancelDiscovery()
                delay(500.milliseconds)

                // Fetch SDP UUIDs if null to help populate the Bluetooth stack SDP cache
                if (device.uuids == null) {
                    device.fetchUuidsWithSdp()
                    delay(200.milliseconds)
                }

                var connected = false
                var lastException: Exception? = null

                // Strategy 1: Insecure RFCOMM socket via SDP UUID (standard for SPP / ESP32)
                try {
                    Log.d(
                        "BluetoothController",
                        "Attempting connection via createInsecureRfcommSocketToServiceRecord..."
                    )
                    socket = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
                    socket?.connect()
                    connected = true
                } catch (e: IOException) {
                    Log.w(
                        "BluetoothController",
                        "Insecure RFCOMM connection via SDP failed",
                        e
                    )
                    lastException = e
                    closeSocketSilently()
                    delay(500.milliseconds)
                }

                if (connected && socket != null) {
                    outputStream = socket?.outputStream
                    _isConnected.value = true
                    _isConnecting.value = false
                    _connectedDeviceName.value = deviceName
                    _connectionStatus.value = "Connected to $deviceName"
                    Log.d("BluetoothController", "Successfully connected to $deviceName")
                    true
                } else {
                    throw lastException
                        ?: IOException("Failed to establish Bluetooth socket connection")
                }
            } catch (e: Exception) {
                Log.e("BluetoothController", "Connection failed", e)
                closeConnection()
                _connectionStatus.value = "Connection failed: ${e.localizedMessage}"
                false
            }
        }
    }

    /**
     * Send a text message or command to the connected ESP32 device.
     */
    suspend fun sendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            if ((outputStream == null) || (!_isConnected.value)) {
                Log.e("BluetoothController", "Cannot send message: Not connected")
                return@withContext false
            }

            try {
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
                Log.d("BluetoothController", "Sent message: $message")
                true
            } catch (e: IOException) {
                Log.e("BluetoothController", "Error sending message", e)
                _connectionStatus.value = "Failed to send message"
                false
            }
        }
    }

    /**
     * Disconnect from the Bluetooth device.
     */
    fun disconnect() {
        closeConnection()
    }

    private fun closeSocketSilently() {
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.w("BluetoothController", "Error closing temporary socket", e)
        } finally {
            socket = null
        }
    }

    private fun closeConnection() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothController", "Error closing socket", e)
        } finally {
            outputStream = null
            socket = null
            _isConnected.value = false
            _isConnecting.value = false
            _connectedDeviceName.value = null
            _connectionStatus.value = "Disconnected"
        }
    }
}

