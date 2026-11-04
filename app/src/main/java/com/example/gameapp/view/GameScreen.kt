package com.example.gameapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.gameapp.component.AppTopBar
import com.example.gameapp.component.DeviceItem
import com.example.gameapp.component.DirectionalCross
import com.example.gameapp.component.SectionCard
import com.example.gameapp.viewModel.GameViewModel
import com.example.gameapp.viewModel.ResultFormState
import org.koin.androidx.compose.koinViewModel

@Composable
fun GameScreen(
    gameVM: GameViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val isConnected by gameVM.isConnected.collectAsState()
    val isConnecting by gameVM.isConnecting.collectAsState()
    val connectedDeviceName by gameVM.connectedDeviceName.collectAsState()
    val connectionStatus by gameVM.connectionStatus.collectAsState()
    val receivedMessage by gameVM.receivedMessage.collectAsState()
    val messageText by gameVM.messageText.collectAsState()
    val isSaving by gameVM.isSaving.collectAsState()
    val showDevicePicker by gameVM.showDevicePicker.collectAsState()
    val showResultDialog by gameVM.showResultDialog.collectAsState()

    // ---- Android permission / system dialogs (must live in the UI layer) ----
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var hasPermissions by remember {
        mutableStateOf(requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    var isBluetoothEnabled by remember {
        mutableStateOf(hasPermissions && gameVM.isBluetoothEnabled())
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isBluetoothEnabled = result.resultCode == Activity.RESULT_OK
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        hasPermissions = permissionsMap.values.all { it }
        if (hasPermissions) {
            isBluetoothEnabled = gameVM.isBluetoothEnabled()
            if (!isBluetoothEnabled && gameVM.isBluetoothSupported) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        } else {
            Toast.makeText(context, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestBluetooth() {
        when {
            !gameVM.isBluetoothSupported -> Unit
            !hasPermissions -> permissionLauncher.launch(requiredPermissions)
            !gameVM.isBluetoothEnabled() ->
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> isBluetoothEnabled = true
        }
    }

    LaunchedEffect(Unit) {
        requestBluetooth()
        gameVM.userMessages.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose { gameVM.stopDiscovery() }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Controller", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                !gameVM.isBluetoothSupported -> InfoCard(
                    emoji = "🚫",
                    text = "Bluetooth isn't supported on this device."
                )

                !isBluetoothEnabled -> {
                    InfoCard(
                        emoji = "📶",
                        text = "Bluetooth is required to connect to your controller."
                    )
                    Button(
                        onClick = { requestBluetooth() },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) { Text("Enable Bluetooth") }
                }

                else -> {
                    ConnectionStatusCard(
                        isConnected = isConnected,
                        isConnecting = isConnecting,
                        deviceName = connectedDeviceName,
                        status = connectionStatus,
                        onConnect = {
                            if (hasPermissions) gameVM.openDevicePicker() else requestBluetooth()
                        },
                        onDisconnect = gameVM::disconnect
                    )

                    if (isConnected) {
                        DirectionalCross(onDirectionClick = gameVM::sendDirection)

                        SectionCard(title = "Send a message") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = gameVM::onMessageTextChange,
                                    singleLine = true,
                                    placeholder = { Text("Message") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = gameVM::sendMessage) { Text("Send") }
                            }
                            receivedMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Last message from device",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(text = msg, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        OutlinedButton(
                            onClick = gameVM::openResultDialog,
                            enabled = !isSaving,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("💾  Save game result")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResultDialog) {
        val form by gameVM.resultForm.collectAsState()
        SaveResultDialog(
            form = form,
            onFormChange = gameVM::updateResultForm,
            onConfirm = gameVM::submitResultForm,
            onDismiss = gameVM::closeResultDialog
        )
    }

    if (showDevicePicker) {
        val pairedDevices by gameVM.pairedDevices.collectAsState()
        val scannedDevices by gameVM.scannedDevices.collectAsState()
        val isScanning by gameVM.isScanning.collectAsState()
        DevicePickerDialog(
            pairedDevices = pairedDevices,
            scannedDevices = scannedDevices,
            isScanning = isScanning,
            onRescan = gameVM::rescan,
            onDeviceSelected = gameVM::connectToDevice,
            onDismiss = gameVM::closeDevicePicker
        )
    }
}

@Composable
private fun InfoCard(emoji: String, text: String) {
    SectionCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    deviceName: String?,
    status: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val dotColor = when {
        isConnected -> Color(0xFF2EBD59)
        isConnecting -> Color(0xFFFFB300)
        else -> MaterialTheme.colorScheme.outline
    }

    SectionCard(title = "Device") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isConnected -> deviceName ?: "ESP-32-S3 Feather"
                        isConnecting -> "Connecting…"
                        else -> "Not connected"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            when {
                isConnecting -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )

                isConnected -> OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
                else -> Button(onClick = onConnect) { Text("Connect") }
            }
        }
    }
}

@Composable
private fun SaveResultDialog(
    form: ResultFormState,
    onFormChange: ((ResultFormState) -> ResultFormState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save game result") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormField("Game (e.g. Snake, Pong)", form.gameType) { v -> onFormChange { it.copy(gameType = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Your colour", form.colourOwner, Modifier.weight(1f)) { v ->
                        onFormChange { it.copy(colourOwner = v) }
                    }
                    FormField("Your score", form.ownerScore, Modifier.weight(1f), number = true) { v ->
                        onFormChange { it.copy(ownerScore = v) }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Multiplayer", modifier = Modifier.weight(1f))
                    Switch(
                        checked = form.isMultiplayer,
                        onCheckedChange = { checked -> onFormChange { it.copy(isMultiplayer = checked) } }
                    )
                }

                if (form.isMultiplayer) {
                    HorizontalDivider()
                    Text(
                        text = "Invited player",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FormField("Username", form.invitedUsername) { v -> onFormChange { it.copy(invitedUsername = v) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormField("Colour", form.invitedColour, Modifier.weight(1f)) { v ->
                            onFormChange { it.copy(invitedColour = v) }
                        }
                        FormField("Score", form.invitedScore, Modifier.weight(1f), number = true) { v ->
                            onFormChange { it.copy(invitedScore = v) }
                        }
                    }
                    FormField("Winner (empty if draw)", form.winner) { v -> onFormChange { it.copy(winner = v) } }
                } else {
                    Text(
                        text = "Single player: no winner will be recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    number: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = modifier
    )
}

@SuppressLint("MissingPermission")
@Composable
private fun DevicePickerDialog(
    pairedDevices: List<BluetoothDevice>,
    scannedDevices: List<BluetoothDevice>,
    isScanning: Boolean,
    onRescan: () -> Unit,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    val nearbyDevices = scannedDevices.filter { scanned ->
        scanned.name != null && pairedDevices.none { it.address == scanned.address }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select a device")
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onRescan) { Text("Scan") }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                item { PickerHeader("Paired devices") }
                if (pairedDevices.isEmpty()) {
                    item { PickerEmpty("No paired devices") }
                } else {
                    items(pairedDevices, key = { it.address }) { device ->
                        DeviceItem(device = device) { onDeviceSelected(device) }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    PickerHeader("Nearby devices")
                }
                if (nearbyDevices.isEmpty()) {
                    item { PickerEmpty(if (isScanning) "Scanning…" else "No nearby devices found") }
                } else {
                    items(nearbyDevices, key = { it.address }) { device ->
                        DeviceItem(device = device) { onDeviceSelected(device) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PickerHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun PickerEmpty(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
