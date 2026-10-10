package com.cabletester.pro.components

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

@Composable
fun SelfLearnScreen(
    articleCode: String,
    articleName: String,
    usbService: USBService,
    isArduinoConnected: Boolean,
    onSave: (List<PinConnection>) -> Unit,
    onBack: () -> Unit
) {
    val mainHandler = remember {
        Handler(Looper.getMainLooper())
    }

    var learned by remember { mutableStateOf(false) }
    var learning by remember { mutableStateOf(false) }

    var learnedConnections by remember {
        mutableStateOf(emptyList<PinConnection>())
    }

    var status by remember {
        mutableStateOf(
            if (isArduinoConnected) {
                "Arduino je povezan."
            } else {
                "Najprej poveži Arduino na zaslonu TEST."
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
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

        Text(
            "▶ SELF LEARN",
            style = MaterialTheme.typography.headlineSmall
        )

        if (!learned) {
            Text(
                "Priklopi referenčni kabel in zaženi učenje."
            )

            Text("Stanje: $status")

            Button(
                onClick = {
                    learning = true
                    status = "Merim povezave z Arduinom..."

                    usbService.sendTest(
                        onResult = { response ->
                            mainHandler.post {
                                val cleanResponse = response.trim()

                                if (
                                    cleanResponse == "NO_CONNECTIONS" ||
                                    cleanResponse.isBlank()
                                ) {
                                    status =
                                        "Ni zaznanih povezav. Preveri kabel."
                                    learning = false
                                } else {
                                    val parts = cleanResponse
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }

                                    val channel =
                                        """(?:[AB](?:[1-9]|_SHIELD))"""

                                    val pattern = Regex(
                                        """^($channel)-($channel)$"""
                                    )

                                    val parsed = parts.mapNotNull { item ->
                                        val match =
                                            pattern.matchEntire(item)

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

                                    if (
                                        parsed.isEmpty() ||
                                        parsed.size != parts.size
                                    ) {
                                        status =
                                            "Neveljaven Arduino odgovor: " +
                                                    cleanResponse
                                    } else {
                                        learnedConnections = parsed
                                        onSave(parsed)

                                        learned = true
                                        status =
                                            "Referenčni vzorec uspešno shranjen."
                                    }

                                    learning = false
                                }
                            }
                        },
                        onError = { error ->
                            mainHandler.post {
                                status = error
                                learning = false
                            }
                        }
                    )
                },
                enabled = isArduinoConnected &&
                        !learning &&
                        !learned,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (learning) "UČENJE..." else "ZAČNI UČENJE"
                )
            }
        } else {
            Text(
                "✅ REFERENČNI VZOREC SHRANJEN",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary
            )

            Text("Naučenih povezav: ${learnedConnections.size}")
            Text(status)

            HorizontalDivider()

            Text(
                "Naučene povezave:",
                style = MaterialTheme.typography.titleMedium
            )

            learnedConnections.forEach { connection ->
                Text(
                    "${connection.fromChannel}-${connection.toChannel}"
                )
            }
        }

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("NAZAJ")
        }
    }
}
