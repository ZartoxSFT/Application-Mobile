package com.example.gameapp.controller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

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

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    // UUIDs standard "Nordic UART" pour la communication série BLE (très utilisé sur ESP32)
    private val UART_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val UART_RX_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // Pour écrire vers l'ESP32

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (bluetoothAdapter == null) return false
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        _scannedDevices.value = emptyList()
        val started =
            bluetoothAdapter.startDiscovery() // Note : startDiscovery trouve le BLE et le Classique
        _isScanning.value = started
        return started
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun addScannedDevice(device: BluetoothDevice) {
        val currentList = _scannedDevices.value
        if (currentList.none { it.address == device.address }) {
            _scannedDevices.value = currentList + device
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        _isConnecting.value = true
        val deviceName = device.name ?: device.address
        _connectionStatus.value = "Connecting to $deviceName..."

        stopDiscovery()

        // Connexion au serveur GATT (BLE)
        bluetoothGatt = device.connectGatt(null, false, gattCallback)
    }

    // Le callback qui gère les événements BLE (Asynchrone)
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionStatus.value = "Connected. Discovering services..."
                // Une fois connecté, on doit demander à l'ESP32 quels services il propose
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                closeConnection()
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(UART_SERVICE_UUID)
                if (service != null) {
                    writeCharacteristic = service.getCharacteristic(UART_RX_CHARACTERISTIC_UUID)

                    _isConnected.value = true
                    _isConnecting.value = false
                    _connectedDeviceName.value = gatt.device.name ?: gatt.device.address
                    _connectionStatus.value = "Connected and Ready"
                    Log.d("BluetoothController", "UART Service trouvé. Prêt à envoyer des données.")
                } else {
                    _connectionStatus.value = "UART Service introuvable sur cet appareil"
                    disconnect()
                }
            } else {
                _connectionStatus.value = "Échec de la découverte des services"
                disconnect()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendMessage(message: String): Boolean {
        val gatt = bluetoothGatt
        val char = writeCharacteristic

        if (gatt == null || char == null || !_isConnected.value) {
            Log.e("BluetoothController", "Non connecté")
            return false
        }

        char.value = message.toByteArray()

        // La méthode d'écriture dépend de la version d'Android
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(
                char,
                message.toByteArray(),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
            result == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    @SuppressLint("MissingPermission")
    private fun closeConnection() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
        _isConnected.value = false
        _isConnecting.value = false
        _connectedDeviceName.value = null
        _connectionStatus.value = "Disconnected"
    }
}