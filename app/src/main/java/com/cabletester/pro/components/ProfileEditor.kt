package com.cabletester.pro.components

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cabletester.pro.model.CableProfile
import com.cabletester.pro.model.PinConnection
import com.cabletester.pro.service.BluetoothService
import com.cabletester.pro.service.USBService
import com.cabletester.pro.viewmodel.TestComparison

private enum class ConnectionType {
    USB,
    BLUETOOTH
}

@Composable
fun ProfileEditor(
    profile: CableProfile?,
    onSave: (CableProfile) -> Unit,
    onDelete: () -> Unit,
    onLearnSave: (List<PinConnection>) -> Unit,
    onTest: (List<PinConnection>) -> TestComparison?,
    isArduinoConnected: Boolean,
    onArduinoConnectionChanged: (Boolean) -> Unit
) {
    if (profile == null) {
        Text(
            "Izberi profil",
            modifier = Modifier.padding(20.dp)
        )
        return
    }

    val context = LocalContext.current
    val mainHandler = remember {
        Handler(Looper.getMainLooper())
    }

    val toneGenerator = remember {
        ToneGenerator(
            AudioManager.STREAM_MUSIC,
            100
        )
    }

    DisposableEffect(toneGenerator) {
        onDispose {
            toneGenerator.release()
        }
    }

    val usbService = remember {
        USBService(
            context.applicationContext
        )
    }

    val bluetoothService = remember {
        BluetoothService(
            context.applicationContext
        )
    }

    var connectionType by remember {
        mutableStateOf(ConnectionType.USB)
    }

    var showSelfLearnScreen by remember {
        mutableStateOf(false)
    }

    var connecting by remember {
        mutableStateOf(false)
    }

    var testing by remember {
        mutableStateOf(false)
    }

    var connectionStatus by remember {
        mutableStateOf("")
    }

    var testStatus by remember {
        mutableStateOf("")
    }

    var rawResponse by remember {
        mutableStateOf("")
    }

    var testComparison by remember {
        mutableStateOf<TestComparison?>(null)
    }

    var comparisonMessage by remember {
        mutableStateOf("")
    }

    var showBluetoothDevices by remember {
        mutableStateOf(false)
    }

    var pairedDevices by remember {
        mutableStateOf<List<BluetoothDevice>>(emptyList())
    }

    /*
     * Android 12+ zahteva BLUETOOTH_CONNECT
     * kot runtime permission.
     */
    val bluetoothPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                pairedDevices =
                    bluetoothService.getPairedDevices()

                if (pairedDevices.isEmpty()) {
                    connectionStatus =
                        "Ni najdenih seznanjenih Bluetooth naprav."
                } else {
                    showBluetoothDevices = true
                }
            } else {
                connecting = false

                connectionStatus =
                    "Bluetooth dovoljenje ni odobreno."
            }
        }

    /*
     * USB odklop.
     */
    DisposableEffect(usbService) {

        usbService.setOnDisconnectedListener {

            mainHandler.post {

                connecting = false

                onArduinoConnectionChanged(false)

                connectionStatus =
                    "Arduino je odklopljen."
            }
        }

        onDispose {
            usbService.setOnDisconnectedListener(null)
        }
    }

    /*
     * Bluetooth odklop.
     */
    DisposableEffect(bluetoothService) {

        bluetoothService.setOnDisconnectedListener {

            mainHandler.post {

                connecting = false

                onArduinoConnectionChanged(false)

                connectionStatus =
                    "Bluetooth Arduino je odklopljen."
            }
        }

        onDispose {
            bluetoothService.setOnDisconnectedListener(null)
        }
    }

    /*
     * Ob izhodu iz ProfileEditorja zapremo povezave.
     */
    DisposableEffect(Unit) {

        onDispose {
            bluetoothService.shutdown()

            try {
                usbService.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    /*
     * SELF LEARN.
     *
     * Trenutni SelfLearnScreen še uporablja USBService.
     * Bluetooth za SELF LEARN dodava v naslednjem koraku.
     */
    if (showSelfLearnScreen) {

        SelfLearnScreen(
            articleCode = profile.articleCode,
            articleName = profile.name,
            usbService = usbService,
            isArduinoConnected =
                isArduinoConnected &&
                        connectionType == ConnectionType.USB,
            onSave = onLearnSave,
            onBack = {
                showSelfLearnScreen = false
            }
        )

        return
    }

    var editMode by remember {
        mutableStateOf(false)
    }

    var articleCode by remember {
        mutableStateOf(profile.articleCode)
    }

    var articleName by remember {
        mutableStateOf(profile.articleName)
    }

    var notes by remember {
        mutableStateOf(profile.notes)
    }

    LaunchedEffect(profile) {

        articleCode =
            profile.articleCode

        articleName =
            profile.articleName

        notes =
            profile.notes
    }

    /*
     * Bluetooth naprave.
     */
    if (showBluetoothDevices) {

        AlertDialog(
            onDismissRequest = {
                if (!connecting) {
                    showBluetoothDevices = false
                }
            },

            title = {
                Text("Izberi Bluetooth napravo")
            },

            text = {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    if (pairedDevices.isEmpty()) {

                        Text(
                            "Ni seznanjenih naprav."
                        )

                    } else {

                        pairedDevices.forEach { device ->

                            val deviceName =
                                try {
                                    device.name
                                        ?: "Neimenovana naprava"
                                } catch (_: SecurityException) {
                                    "Bluetooth naprava"
                                }

                            val deviceAddress =
                                try {
                                    device.address
                                } catch (_: SecurityException) {
                                    ""
                                }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 4.dp
                                    )
                                    .clickable(
                                        enabled = !connecting
                                    ) {

                                        showBluetoothDevices =
                                            false

                                        connecting = true

                                        connectionStatus =
                                            "Povezujem $deviceName..."

                                        /*
                                         * Če smo bili prej na USB,
                                         * ga odklopimo.
                                         */
                                        if (
                                            connectionType ==
                                            ConnectionType.USB
                                        ) {
                                            try {
                                                usbService.disconnect()
                                            } catch (_: Exception) {
                                            }
                                        }

                                        bluetoothService.connect(
                                            device = device,

                                            onConnected = {

                                                mainHandler.post {

                                                    connecting = false

                                                    connectionType =
                                                        ConnectionType.BLUETOOTH

                                                    connectionStatus =
                                                        ""

                                                    onArduinoConnectionChanged(
                                                        true
                                                    )
                                                }
                                            },
                                            onError = { error ->
                                                mainHandler.post {
                                                    connecting = false
                                                    onArduinoConnectionChanged(
                                                        false
                                                    )
                                                    connectionStatus =
                                                        error
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(
                                        12.dp
                                    )
                                ) {
                                    Text(
                                        deviceName,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium
                                    )
                                    if (
                                        deviceAddress
                                            .isNotBlank()
                                    ) {
                                        Text(
                                            deviceAddress,
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBluetoothDevices = false
                    }
                ) {
                    Text("PREKLIČI")
                }
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(10.dp),
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        Card(
            Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(6.dp)
            ) {
                Text(
                    profile.articleCode,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected =
                    connectionType ==
                            ConnectionType.USB,
                onClick = {
                    if (!isArduinoConnected) {
                        connectionType =
                            ConnectionType.USB
                        connectionStatus = ""
                    }
                },
                label = {
                    Text("USB")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = "USB",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                modifier =
                    Modifier.weight(1f)
            )
            FilterChip(
                selected =
                    connectionType ==
                            ConnectionType.BLUETOOTH,
                onClick = {
                    if (!isArduinoConnected) {
                        connectionType =
                            ConnectionType.BLUETOOTH
                        connectionStatus = ""
                    }
                },
                label = {
                    Text("Bluetooth")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                modifier =
                    Modifier.weight(1f)
            )
        }
        /*
        * POVEZAVA Z ARDUINOM
        */
        if (!isArduinoConnected) {
            Button(
                onClick = {
                    connecting = true
                    connectionStatus = ""
                    if (
                        connectionType ==
                        ConnectionType.USB
                    ) {
                        connectionStatus =
                            "Iščem Arduino CH340..."
                        val device =
                            usbService.findArduino()
                        if (device == null) {
                            connecting = false
                            connectionStatus =
                                "Arduino CH340 ni najden " +
                                        "ali naprava ni podprta."
                        } else {
                            usbService.requestUsbPermission(
                                device
                            ) { granted ->
                                if (granted) {
                                    usbService.connect(
                                        device = device,
                                        onConnected = {
                                            mainHandler.post {
                                                connecting =
                                                    false
                                                connectionStatus =
                                                    ""
                                                onArduinoConnectionChanged(
                                                    true
                                                )
                                            }
                                        },
                                        onError = { error ->
                                            mainHandler.post {
                                                connecting =
                                                    false
                                                connectionStatus =
                                                    error
                                                onArduinoConnectionChanged(
                                                    false
                                                )
                                            }
                                        }
                                    )
                                } else {
                                    mainHandler.post {
                                        connecting =
                                            false
                                        connectionStatus =
                                            "USB dovoljenje ni odobreno."
                                        onArduinoConnectionChanged(
                                            false
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        /*
                        * BLUETOOTH
                        */
                        if (
                            !bluetoothService
                                .isBluetoothSupported()
                        ) {
                            connecting = false
                            connectionStatus =
                                "Naprava ne podpira Bluetootha."
                        } else if (
                            !bluetoothService
                                .isBluetoothEnabled()
                        ) {
                            connecting = false
                            connectionStatus =
                                "Bluetooth ni vklopljen."
                        } else {
                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.S
                            ) {
                                val permission =
                                    Manifest.permission
                                        .BLUETOOTH_CONNECT
                                val granted =
                                    androidx.core.content
                                        .ContextCompat
                                        .checkSelfPermission(
                                            context,
                                            permission
                                        ) ==
                                            android.content.pm
                                                .PackageManager
                                                .PERMISSION_GRANTED
                                if (!granted) {
                                    bluetoothPermissionLauncher
                                        .launch(permission)
                                    connecting = false
                                } else {
                                    pairedDevices =
                                        bluetoothService
                                            .getPairedDevices()
                                    if (
                                        pairedDevices.isEmpty()
                                    ) {
                                        connecting = false
                                        connectionStatus =
                                            "Najprej seznani HC-05 " +
                                                    "v nastavitvah Bluetootha."
                                    } else {
                                        connecting = false
                                        showBluetoothDevices =
                                            true
                                    }
                                }
                            } else {
                                pairedDevices =
                                    bluetoothService
                                        .getPairedDevices()
                                if (
                                    pairedDevices.isEmpty()
                                ) {
                                    connecting = false
                                    connectionStatus =
                                        "Ni najdenih seznanjenih " +
                                                "Bluetooth naprav."
                                } else {
                                    connecting = false
                                    showBluetoothDevices =
                                        true
                                }
                            }
                        }
                    }
                },
                enabled = !connecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                val (currentIcon, buttonLabel) = when {
                    connecting -> {
                        Icons.Default.HourglassEmpty to "POVEZOVANJE..."
                    }
                    connectionType == ConnectionType.BLUETOOTH -> {
                        Icons.Default.Bluetooth to "POVEŽI TESTER preko BT"
                    }
                    else -> {
                        Icons.Default.Usb to "POVEŽI TESTER preko USB-C"
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = currentIcon,
                        contentDescription = buttonLabel,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
        if (connectionStatus.isNotBlank()) {
            Text(connectionStatus)
        }
        /*
        * INFORMACIJA O AKTIVNI POVEZAVI
        */
        if (isArduinoConnected) {
            Card(
                Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(10.dp)
                ) {
                    val statusIcon = if (connectionType == ConnectionType.BLUETOOTH) {
                        Icons.Default.Bluetooth
                    } else {
                        Icons.Default.Usb
                    }
                    val statusText = if (connectionType == ConnectionType.BLUETOOTH) {
                        "POVEZAN: Bluetooth"
                    } else {
                        "POVEZAN: USB"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = statusText,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
        /*
        * TEST
        */
        Button(
            onClick = {
                testing = true
                testStatus =
                    "Testiranje kabla..."
                rawResponse = ""
                testComparison = null
                comparisonMessage = ""
                /*
                * Skupna funkcija za obdelavo odgovora.
                */
                /*
 * Skupna funkcija za obdelavo odgovora.
 */
                fun handleResponse(response: String) {
                    rawResponse = response
                    val trimmedResponse = response.trim()

                    val measuredConnections: List<PinConnection> = if (trimmedResponse == "NO_CONNECTIONS") {
                        emptyList()
                    } else {
                        val parts = trimmedResponse
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        val channel = """(?:[AB](?:[1-9]|_SHIELD))"""
                        val regex = Regex("""^($channel)-($channel)$""")

                        val parsed = parts.mapNotNull { part ->
                            val match = regex.matchEntire(part)
                            if (match != null) {
                                PinConnection(
                                    fromChannel = match.groupValues[1],
                                    toChannel = match.groupValues[2]
                                )
                            } else {
                                null
                            }
                        }.distinct()

                        if (parts.isEmpty() || parsed.size != parts.size) {
                            emptyList() // Če je odgovor neveljaven, vrnemo prazen seznam
                        } else {
                            parsed
                        }
                    }

                    // Ker measuredConnections zdaj ni več null, lahko varno pokličemo onTest
                    val result = onTest(measuredConnections)

                    if (result == null) {
                        testComparison = null
                        comparisonMessage = "Referenčni vzorec še ni naučen. Najprej izvedi SELF LEARN."
                        testStatus = "Test ni primerjan."
                    } else {
                        testComparison = result
                        comparisonMessage = ""

                        if (result.passed) {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                            testStatus = "Test uspešno zaključen."
                        } else {
                            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2500)
                            testStatus = "Test ni uspešen."
                        }
                    }

                    testing = false
                }

                /*
                * TEST PREK USB
                */
                if (
                    connectionType ==
                    ConnectionType.USB
                ) {
                    usbService.sendTest(
                        onResult = { response ->
                            mainHandler.post {
                                handleResponse(
                                    response
                                )
                            }
                        },
                        onError = { error ->
                            mainHandler.post {
                                testStatus =
                                    error
                                comparisonMessage = ""
                                testing = false
                                if (
                                    error.contains(
                                        "Arduino",
                                        ignoreCase = true
                                    ) ||
                                    error.contains(
                                        "USB",
                                        ignoreCase = true
                                    ) ||
                                    error.contains(
                                        "port",
                                        ignoreCase = true
                                    ) ||
                                    error.contains(
                                        "device",
                                        ignoreCase = true
                                    )
                                ) {
                                    usbService.disconnect()
                                    connecting = false
                                    onArduinoConnectionChanged(
                                        false
                                    )
                                    connectionStatus =
                                        "Arduino je odklopljen."
                                }
                            }
                        }
                    )
                } else {
                    /*
                    * TEST PREK BLUETOOTH
                    */
                    bluetoothService.sendTest(
                        onResult = { response ->
                            mainHandler.post {
                                handleResponse(
                                    response
                                )
                            }
                        },
                        onError = { error ->
                            mainHandler.post {
                                testStatus =
                                    error
                                comparisonMessage = ""
                                testing = false
                                bluetoothService.disconnect()
                                connecting = false
                                onArduinoConnectionChanged(
                                    false
                                )
                                connectionStatus =
                                    "Bluetooth Arduino je odklopljen."
                            }
                        }
                    )
                }
            },
            enabled =
                isArduinoConnected &&
                        !testing,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val testLabel = if (testing) "TESTIRANJE..." else "TEST"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!testing) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Test",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                }
                Text(
                    text = testLabel,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        if (!isArduinoConnected) {
            Text(
                "Za testiranje najprej poveži Arduino."
            )
        }
        /*
        * REZULTAT TESTA
        */
        if (testComparison != null) {
            val result =
                testComparison!!
            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (result.passed) {
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .errorContainer
                            }
                    )
            ) {
                Column(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (result.passed) {
                            "PASS – KABEL JE USTREZEN"
                        } else {
                            "FAIL – KABEL NI USTREZEN"
                        },
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )
                    Text(testStatus)
                    if (
                        result.errors.isNotEmpty()
                    ) {
                        HorizontalDivider()
                        Text(
                            "Ugotovljene napake:",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )
                        result.errors.forEach { error ->
                            Text(
                                "• $error"
                            )
                        }
                    }
                }
            }
        } else if (
            comparisonMessage.isNotBlank()
        ) {
            Card(
                Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(12.dp)
                ) {
                    Text(testStatus)
                    Text(comparisonMessage)
                }
            }
        } else if (
            testStatus.isNotBlank()
        ) {
            Text(testStatus)
        }
        /*
        * IZMERJENE POVEZAVE / STATISTIKA
        */
        if (rawResponse.isNotBlank()) {
            Card(
                Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(12.dp)
                ) {
                    Text(
                        "Izmerjene povezave:"
                    )
                    Text(rawResponse)
                }
            }
        } else if (
            testComparison == null &&
            testStatus.isBlank()
        ) {
            Card(
                Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(12.dp)
                ) {
                    Text(
                        if (
                            profile.referenceLearned
                        ) {
                            "Referenčni vzorec: NAUČEN"
                        } else {
                            "Referenčni vzorec: NI NAUČEN"
                        }
                    )
                    Text(
                        "Naučene povezave: " +
                                "${profile.connections.size}"
                    )
                    if (
                        profile.referenceLearned
                    ) {
                        Text(
                            "Zadnje učenje: " +
                                    profile.learnedDate
                        )
                    }
                    Text(
                        "Zadnji test: " +
                                profile.lastTest
                    )
                    Text(
                        "Uspešni testi: " +
                                "${profile.passedTests}"
                    )
                    Text(
                        "Neuspešni testi: " +
                                "${profile.failedTests}"
                    )
                }
            }
        }
        /*
        * UREJANJE PROFILA
        */
        Button(
            onClick = {
                editMode = !editMode
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                if (editMode) {
                    "SKRIJ UREJANJE"
                } else {
                    "UREDI PROFIL"
                }
            )
        }
        if (editMode) {
            HorizontalDivider()
            Text(
                "Urejanje profila"
            )
            OutlinedTextField(
                value = articleCode,
                onValueChange = {
                    articleCode = it
                },
                label = {
                    Text("Koda artikla")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = articleName,
                onValueChange = {
                    articleName = it
                },
                label = {
                    Text("Naziv artikla")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                },
                label = {
                    Text("Opombe")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            articleCode =
                                articleCode,
                            articleName =
                                articleName,
                            notes =
                                notes
                        )
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("SHRANI")
            }
            Button(
                onClick = {
                    showSelfLearnScreen = true
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("SELF LEARN")
            }
            Button(
                onClick = onDelete,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .error
                    ),
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("IZBRIŠI PROFIL")
            }
        }
    }
}