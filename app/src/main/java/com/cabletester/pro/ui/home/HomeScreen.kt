
package com.cabletester.pro.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cabletester.pro.components.ProfileEditor
import com.cabletester.pro.model.CableProfile
import com.cabletester.pro.model.TestRecord
import com.cabletester.pro.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    var pendingRestoreJson by remember {
        mutableStateOf<String?>(null)
    }

    // IZVOZ CSV
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = buildCsv(viewModel.testRecords)

                context.contentResolver
                    .openOutputStream(uri)
                    ?.use { output ->
                        // UTF-8 BOM za šumnike v Excelu
                        output.write(
                            byteArrayOf(
                                0xEF.toByte(),
                                0xBB.toByte(),
                                0xBF.toByte()
                            )
                        )

                        output.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    ?: throw Exception("Datoteke ni bilo mogoče odpreti.")

                Toast.makeText(
                    context,
                    "CSV je uspešno izvožen.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Napaka pri izvozu CSV: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // IZVOZ JSON VARNOSTNE KOPIJE
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/json"
        )
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.createBackupJson()

                context.contentResolver
                    .openOutputStream(uri)
                    ?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    }
                    ?: throw Exception("Datoteke ni bilo mogoče odpreti.")

                Toast.makeText(
                    context,
                    "Varnostna kopija je uspešno izvožena.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Napaka pri izvozu: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // IZBIRA JSON DATOTEKE ZA OBNOVITEV
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: throw Exception("Datoteke ni bilo mogoče prebrati.")

                pendingRestoreJson = json

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Napaka pri branju: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // POTRDITEV OBNOVE VARNOSTNE KOPIJE
    pendingRestoreJson?.let { json ->
        AlertDialog(
            onDismissRequest = {
                pendingRestoreJson = null
            },
            title = {
                Text("Obnovi varnostno kopijo?")
            },
            text = {
                Text(
                    "Obstoječi profili in zgodovina testiranj " +
                            "bodo zamenjani s podatki iz varnostne kopije. " +
                            "Pred nadaljevanjem se prepričaj, da imaš " +
                            "shranjeno kopijo trenutnih podatkov. " +
                            "Želiš nadaljevati?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = try {
                            viewModel.restoreBackupJson(json)
                        } catch (e: Exception) {
                            false
                        }

                        pendingRestoreJson = null

                        Toast.makeText(
                            context,
                            if (success) {
                                "Varnostna kopija je obnovljena."
                            } else {
                                "Obnova ni uspela. Datoteka morda ni veljavna."
                            },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                ) {
                    Text("OBNOVI")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingRestoreJson = null
                    }
                ) {
                    Text("PREKLIČI")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {

        // LEVA STRAN
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .padding(6.dp)
        ) {

            // NASLOV + USB INDIKATOR
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Shranjeni kabli",
                    style = MaterialTheme.typography.titleMedium
                )

                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = if (viewModel.isArduinoConnected) {
                        Color(0xFF2E7D32)
                    } else {
                        Color.Gray
                    }
                ) {}

                Text(
                    text = "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (viewModel.isArduinoConnected) {
                        Color(0xFF2E7D32)
                    } else {
                        Color.Gray
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // DODAJ NOV KABEL
            Button(
                onClick = {
                    viewModel.createProfile()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF78909C), // Isti modro-sivi odtenek
                    contentColor = Color.White          // Bela barva za ikono in besedilo
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Text("+ DODAJ NOV KABEL")
            }

            Spacer(modifier = Modifier.height(5.dp))

            var searchText by remember {
                mutableStateOf("")
            }

            // ISKANJE PROFILOV
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                },
                label = {
                    Text("Išči")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(5.dp))

            // DRSNI SEZNAM:
            // PROFILI + VSI TRI GUMBI
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {

                // SEZNAM PROFILOV
                viewModel.profiles
                    .filter {
                        searchText.isBlank() ||
                                it.articleCode.contains(searchText, true) ||
                                it.name.contains(searchText, true)
                    }
                    .sortedBy {
                        it.articleCode.lowercase()
                    }
                    .forEach {
                        ProfileItem(
                            profile = it,
                            selected = it.id ==
                                    viewModel.selectedProfile?.id,
                            onClick = {
                                viewModel.selectProfile(it)
                            }
                        )
                    }

                Spacer(modifier = Modifier.height(8.dp))

                // IZVOZ CSV
                OutlinedButton(
                    onClick = {
                        csvLauncher.launch(
                            "CableTesterPro_testiranja.csv"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("IZVOZ CSV")
                }

                Spacer(modifier = Modifier.height(5.dp))

                // IZVOZ VARNOSTNE KOPIJE
                OutlinedButton(
                    onClick = {
                        backupLauncher.launch(
                            "CableTesterPro_backup.json"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("IZVOZ VARNOSTNE KOPIJE")
                }

                Spacer(modifier = Modifier.height(5.dp))

                // OBNOVA VARNOSTNE KOPIJE
                OutlinedButton(
                    onClick = {
                        restoreLauncher.launch(
                            arrayOf(
                                "application/json",
                                "text/json",
                                "application/octet-stream"
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("OBNOVI VARNOSTNO KOPIJO")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        VerticalDivider()

        // DESNA STRAN
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            ProfileEditor(
                profile = viewModel.selectedProfile,

                onSave = {
                    viewModel.updateProfile(it)
                },

                onDelete = {
                    viewModel.selectedProfile?.let {
                        viewModel.deleteProfile(it)
                    }
                },

                onLearnSave = { connections ->
                    viewModel.selectedProfile?.let {
                        viewModel.saveLearnedConnections(
                            it,
                            connections
                        )
                    }
                },

                onTest = { measuredConnections ->
                    viewModel.selectedProfile?.let {
                        viewModel.compareAndSaveTest(
                            it,
                            measuredConnections
                        )
                    }
                },

                isArduinoConnected = viewModel.isArduinoConnected,

                onArduinoConnectionChanged = {
                    viewModel.updateArduinoConnection(it)
                }
            )
        }
    }
}

// PRIPRAVA CSV VSEBINE
private fun buildCsv(
    records: List<TestRecord>
): String {

    fun escapeCsv(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    val header = listOf(
        "Datum",
        "Šifra artikla",
        "Naziv artikla",
        "Rezultat",
        "Napake"
    )

    val rows = records.map { record ->
        listOf(
            record.date,
            record.articleCode,
            record.articleName,
            if (record.passed) "PASS" else "FAIL",
            record.errors.joinToString("; ")
        )
    }

    return (listOf(header) + rows)
        .joinToString("\r\n") { row ->
            row.joinToString(",") { value ->
                escapeCsv(value)
            }
        }
}

// POSAMEZEN PROFIL V LEVEM MENIJU
@Composable
fun ProfileItem(
    profile: CableProfile,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable {
                onClick()
            }
    ) {
        Column(
            modifier = Modifier.padding(6.dp)
        ) {
            Text(
                text = profile.articleCode,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyMedium
            )

            if (selected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}