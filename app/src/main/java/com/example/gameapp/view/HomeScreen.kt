package com.example.gameapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.gameapp.bluetooth.BluetoothController

@SuppressLint("MissingPermission")
@Composable
fun HomeScreen() {
    val context = LocalContext.current

    val bluetoothManager: BluetoothManager? =
        getSystemService(context, BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    // Controller handling connection & messaging with ESP32 (BLE)
    val bluetoothController = remember(bluetoothAdapter) {
        BluetoothController(bluetoothAdapter)
    }

    val isConnected by bluetoothController.isConnected.collectAsState()
    val isConnecting by bluetoothController.isConnecting.collectAsState()
    val connectedDeviceName by bluetoothController.connectedDeviceName.collectAsState()
    val connectionStatus by bluetoothController.connectionStatus.collectAsState()
    val scannedDevices by bluetoothController.scannedDevices.collectAsState()
    val isScanning by bluetoothController.isScanning.collectAsState()

    // Required permissions depending on Android version
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all { perm ->
                ContextCompat.checkSelfPermission(
                    context,
                    perm
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    var isBluetoothEnabled by remember {
        mutableStateOf(
            bluetoothAdapter != null &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || hasPermissions) &&
                    bluetoothAdapter.isEnabled
        )
    }

    var showDevicePicker by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var messageText by remember { mutableStateOf("Hello ESP32") }

    // Register BroadcastReceiver for Bluetooth device discovery
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
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
                        device?.let { bluetoothController.addScannedDevice(it) }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        // Le callback onDiscoveryFinished n'est plus appelé explicitement car
                        // l'état isScanning est mis à jour dans stopDiscovery() et startDiscovery()
                        bluetoothController.stopDiscovery()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
            bluetoothController.stopDiscovery()
        }
    }

    // Launcher to handle the enable-bluetooth prompt result
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isBluetoothEnabled = true
            Log.d("Bluetooth", "Bluetooth enabled successfully!")
        } else {
            isBluetoothEnabled = false
            Log.d("Bluetooth", "Bluetooth authorization denied or cancelled.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        hasPermissions = allGranted
        if (allGranted) {
            val enabled = bluetoothAdapter?.isEnabled == true
            isBluetoothEnabled = enabled
            if (!enabled && bluetoothAdapter != null) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            } else {
                pairedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
                showDevicePicker = true
                bluetoothController.startDiscovery()
            }
        } else {
            Toast.makeText(context, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestEnableBluetooth() {
        if (bluetoothAdapter == null) return
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            isBluetoothEnabled = true
        }
    }

    fun onConnectClicked() {
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
        } else if (bluetoothAdapter?.isEnabled == false) {
            requestEnableBluetooth()
        } else {
            pairedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            showDevicePicker = true
            bluetoothController.startDiscovery()
        }
    }

    LaunchedEffect(bluetoothAdapter, hasPermissions) {
        if (bluetoothAdapter == null) {
            Log.d("Bluetooth", "Bluetooth isn't supported by the device")
        } else if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
        } else if (!bluetoothAdapter.isEnabled) {
            requestEnableBluetooth()
        } else {
            isBluetoothEnabled = true
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bluetooth Controller"
            )
            if (isBluetoothEnabled) {
                Text(
                    text = "Bluetooth Active",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Status: $connectionStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isConnecting) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Connecting...")
                } else if (isConnected) {
                    Text(
                        text = "Connected to: ${connectedDeviceName ?: "ESP-32-S3 Feather"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Message to send") },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val success = bluetoothController.sendMessage(messageText)
                            val toastMsg =
                                if (success) "Message sent!" else "Failed to send message"
                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Send Message")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(onClick = { bluetoothController.disconnect() }) {
                        Text("Disconnect")
                    }
                } else {
                    Button(onClick = { onConnectClicked() }) {
                        Text("Connect to Device")
                    }
                }
            } else {
                Text(
                    text = "Bluetooth is required to use this screen.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { requestEnableBluetooth() }) {
                    Text("Enable Bluetooth")
                }
            }
        }
    }

    // Dialog for picking from paired and discovered nearby devices
    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = {
                bluetoothController.stopDiscovery()
                showDevicePicker = false
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Device",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(onClick = { bluetoothController.startDiscovery() }) {
                            Text("Scan")
                        }
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    item {
                        Text(
                            text = "PAIRED DEVICES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    if (pairedDevices.isEmpty()) {
                        item {
                            Text(
                                text = "No paired devices found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    } else {
                        items(pairedDevices) { device ->
                            DeviceItem(device = device) {
                                bluetoothController.stopDiscovery()
                                showDevicePicker = false
                                bluetoothController.connectToDevice(device) // Plus de coroutine
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AVAILABLE NEARBY DEVICES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    val unbondedDiscoveredDevices = scannedDevices.filter { scanned ->
                        pairedDevices.none { paired -> paired.address == scanned.address }
                    }

                    if (unbondedDiscoveredDevices.isEmpty()) {
                        item {
                            Text(
                                text = if (isScanning) "Scanning for nearby devices..." else "No nearby devices found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    } else {
                        items(unbondedDiscoveredDevices) { device ->
                            if (device.name != null) {
                                DeviceItem(device = device) {
                                    bluetoothController.stopDiscovery()
                                    showDevicePicker = false
                                    bluetoothController.connectToDevice(device) // Plus de coroutine
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    bluetoothController.stopDiscovery()
                    showDevicePicker = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    val name = device.name ?: "Unknown Device"
    val address = device.address
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = address,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}