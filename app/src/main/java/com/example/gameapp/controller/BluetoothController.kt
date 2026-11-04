package com.example.gameapp.controller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BluetoothController(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    val isSupported: Boolean
        get() = bluetoothAdapter != null

    @SuppressLint("MissingPermission")
    fun isEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> =
        bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

    private var isReceiverRegistered = false

    // Receives classic discovery results while a scan is running
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    device?.let { addScannedDevice(it) }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> stopDiscovery()
            }
        }
    }

    private fun registerDiscoveryReceiver() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, filter)
        isReceiverRegistered = true
    }

    private fun unregisterDiscoveryReceiver() {
        if (!isReceiverRegistered) return
        context.unregisterReceiver(discoveryReceiver)
        isReceiverRegistered = false
    }

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

    private val _receivedMessage = MutableStateFlow<String?>(null)
    val receivedMessage: StateFlow<String?> = _receivedMessage.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    // UUIDs standard "Nordic UART" pour la communication série BLE (ESP32)
    private val UART_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val UART_RX_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // Pour écrire vers l'ESP32
    private val UART_TX_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Pour recevoir depuis l'ESP32
    private val CCCD_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (bluetoothAdapter == null) return false
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        _scannedDevices.value = emptyList()
        registerDiscoveryReceiver()
        val started = bluetoothAdapter.startDiscovery()
        _isScanning.value = started
        if (!started) unregisterDiscoveryReceiver()
        return started
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
        unregisterDiscoveryReceiver()
    }

    private fun addScannedDevice(device: BluetoothDevice) {
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
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    // Callback handling BLE events (async)
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionStatus.value = "Connected. Discovering services..."
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
                    val readCharacteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID)

                    if (readCharacteristic != null) {
                        gatt.setCharacteristicNotification(readCharacteristic, true)
                        val descriptor = readCharacteristic.getDescriptor(CCCD_UUID)
                        if (descriptor != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(
                                    descriptor,
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }

                    _isConnected.value = true
                    _isConnecting.value = false
                    _connectedDeviceName.value = gatt.device.name ?: gatt.device.address
                    _connectionStatus.value = "Connected and Ready"
                    Log.d("BluetoothController", "UART Service trouvé. Prêt.")
                } else {
                    _connectionStatus.value = "UART Service introuvable sur cet appareil"
                    disconnect()
                }
            } else {
                _connectionStatus.value = "Échec de la découverte des services"
                disconnect()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val data = characteristic.value
            if (data != null) {
                val text = String(data)
                _receivedMessage.value = text
                Log.d("BluetoothController", "Received message: $text")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val text = String(value)
            _receivedMessage.value = text
            Log.d("BluetoothController", "Received message: $text")
        }
    }

    fun clearReceivedMessage() {
        _receivedMessage.value = null
    }

    @SuppressLint("MissingPermission")
    fun sendMessage(message: String): Boolean {
        val gatt = bluetoothGatt
        val char = writeCharacteristic

        if (gatt == null || char == null || !_isConnected.value) {
            Log.e("BluetoothController", "Non connecté")
            return false
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(
                char,
                message.toByteArray(),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
            result == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            char.value = message.toByteArray()
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val gatt = bluetoothGatt
        if (gatt == null) {
            // Nothing connected (or still connecting): make sure the state is reset anyway.
            closeConnection()
        } else {
            gatt.disconnect()
        }
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
