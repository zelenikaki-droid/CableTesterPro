
package com.cabletester.pro.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cabletester.pro.model.PinConnection
import com.cabletester.pro.service.USBService
import com.cabletester.pro.viewmodel.TestComparison

@Composable
fun TestScreen(
    articleCode: String,
    articleName: String,
    usbService: USBService,
    onBack: () -> Unit,
    isArduinoConnected: Boolean,
    onArduinoConnectionChanged: (Boolean) -> Unit,
    onTest: (List<PinConnection>) -> TestComparison?
) {
    val mainHandler = remember {
        Handler(Looper.getMainLooper())
    }

    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    var status by remember {
        mutableStateOf(
            if (isArduinoConnected) {
                ""
            } else {
                "Arduino še ni povezan."
            }
        )
    }

    var connected by remember {
        mutableStateOf(isArduinoConnected)
    }

    var result by remember { mutableStateOf("") }

    var comparison by remember {
        mutableStateOf<TestComparison?>(null)
    }

    var comparisonMessage by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            articleCode,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            articleName,
            style = MaterialTheme.typography.titleLarge
        )

        HorizontalDivider()

        Button(
            onClick = {
                status = "Pošiljam ukaz TEST..."
                result = ""
                comparison = null
                comparisonMessage = ""

                usbService.sendTest(
                    onResult = { response ->
                        mainHandler.post {
                            result = response

                            val trimmedResponse = response.trim()

                            val measuredConnections =
                                if (
                                    trimmedResponse == "NO_CONNECTIONS" ||
                                    trimmedResponse.isBlank()
                                ) {
                                    emptyList()
                                } else {
                                    val parts = trimmedResponse
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }

                                    val channel =
                                        """(?:[AB](?:[1-9]|_SHIELD))"""

                                    val regex = Regex(
                                        """^($channel)-($channel)$"""
                                    )

                                    val parsed = parts.mapNotNull { part ->
                                        val match =
                                            regex.matchEntire(part)

                                        if (match != null) {
                                            PinConnection(
                                                fromChannel =
                                                    match.groupValues[1],
                                                toChannel =
                                                    match.groupValues[2]
                                            )
                                        } else {
                                            null
                                        }
                                    }.distinct()

                                    if (parsed.size != parts.size) {
                                        status =
                                            "Neveljaven odgovor Arduina. " +
                                                    "Preveri format odgovora."
                                        return@post
                                    }

                                    parsed
                                }

                            val testResult =
                                onTest(measuredConnections)

                            if (testResult == null) {
                                comparison = null
                                comparisonMessage =
                                    "Referenčni vzorec še ni naučen. " +
                                            "Najprej izvedi SELF LEARN."
                                status = "Test ni primerjan."
                            } else {
                                comparison = testResult
                                comparisonMessage = ""

                                if (testResult.passed) {
                                    toneGenerator.startTone(
                                        ToneGenerator.TONE_PROP_BEEP,
                                        180
                                    )

                                    status = "Test uspešno zaključen."
                                } else {
                                    toneGenerator.startTone(
                                        ToneGenerator.TONE_PROP_BEEP,
                                        800
                                    )

                                    status = "Test ni uspešen."
                                }
                            }
                        }
                    },
                    onError = { error ->
                        mainHandler.post {
                            status = error
                            connected = false
                            onArduinoConnectionChanged(false)
                        }
                    }
                )
            },
            enabled = connected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("POŠLJI TEST")
        }

        if (status.isNotBlank()) {
            Text("Stanje: $status")
        }

        if (result.isNotBlank()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Odgovor Arduina:")
                    Text(result)
                }
            }
        }

        if (comparisonMessage.isNotBlank()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    comparisonMessage,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        comparison?.let { testResult ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (testResult.passed) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (testResult.passed) {
                            "✅ PASS – KABEL JE USTREZEN"
                        } else {
                            "❌ FAIL – KABEL NI USTREZEN"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (testResult.errors.isNotEmpty()) {
                        HorizontalDivider()

                        Text(
                            "Ugotovljene napake:",
                            style = MaterialTheme.typography.titleMedium
                        )

                        testResult.errors.forEach { error ->
                            Text("• $error")
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                usbService.disconnect()
                connected = false
                onArduinoConnectionChanged(false)
                status = "Arduino je odklopljen."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ODKLOPI ARDUINO")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("NAZAJ")
        }
    }
}