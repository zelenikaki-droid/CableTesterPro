package com.cabletester.pro.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cabletester.pro.model.CableProfile
import com.cabletester.pro.model.PinConnection
import com.cabletester.pro.model.TestRecord
import com.cabletester.pro.repository.ProfileRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    var profiles by mutableStateOf(repository.loadProfiles())
        private set

    var selectedProfile by mutableStateOf<CableProfile?>(null)
        private set

    var isArduinoConnected by mutableStateOf(false)
        private set

    // ZGODOVINA VSEH TESTOV
    var testRecords by mutableStateOf(repository.loadTestRecords())
        private set

    init {
        val lastId = repository.getLastProfileId()
        selectedProfile = profiles.find { it.id == lastId }
    }

    fun updateArduinoConnection(connected: Boolean) {
        isArduinoConnected = connected
    }

    fun selectProfile(profile: CableProfile) {
        selectedProfile = profile
        repository.saveLastProfileId(profile.id)
    }

    fun addProfile(profile: CableProfile) {
        profiles = profiles + profile
        repository.saveProfiles(profiles)
    }

    fun updateProfile(profile: CableProfile) {
        val updatedProfile = profile.copy(
            name = profile.articleName
        )

        profiles = profiles.map {
            if (it.id == updatedProfile.id) updatedProfile else it
        }

        selectedProfile = updatedProfile
        repository.saveProfiles(profiles)
    }

    fun deleteProfile(profile: CableProfile) {
        profiles = profiles.filter { it.id != profile.id }

        if (selectedProfile?.id == profile.id) {
            selectedProfile = null
        }

        repository.saveProfiles(profiles)
    }

    fun createProfile() {
        val profile = CableProfile(
            id = System.currentTimeMillis(),
            name = "Nov profil",
            articleCode = "NOV-${profiles.size + 1}",
            articleName = "Nov profil",
            notes = "",
            created = System.currentTimeMillis().toString()
        )

        addProfile(profile)
        selectProfile(profile)
    }

    fun saveLearnedConnections(
        profile: CableProfile,
        connections: List<PinConnection>
    ) {
        val date = SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())

        val updatedProfile = profile.copy(
            connections = connections.distinct(),
            referenceLearned = true,
            learnedDate = date
        )

        updateProfile(updatedProfile)
    }

    // SHRANI REZULTAT TESTA IN CELOTEN ZAPIS
    fun compareAndSaveTest(
        profile: CableProfile,
        measuredConnections: List<PinConnection>
    ): TestComparison? {

        if (!profile.referenceLearned || profile.connections.isEmpty()) {
            return null
        }

        val reference = profile.connections.toSet()
        val measured = measuredConnections.toSet()

        val missing = reference - measured
        val extra = measured - reference

        val errors = buildList {
            missing.forEach { connection ->
                add(
                    "Manjka povezava: " +
                            "${connection.fromChannel}-${connection.toChannel}"
                )
            }

            extra.forEach { connection ->
                add(
                    "Nepričakovana povezava: " +
                            "${connection.fromChannel}-${connection.toChannel}"
                )
            }
        }

        val passed = errors.isEmpty()

        val date = SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())

        // POSODOBI POVZETEK V PROFILU
        val updatedProfile = profile.copy(
            lastTest = date,
            lastResult = passed,
            passedTests = profile.passedTests + if (passed) 1 else 0,
            failedTests = profile.failedTests + if (passed) 0 else 1,
            testErrors = errors
        )

        updateProfile(updatedProfile)

        // USTVARI ZAPIS POSAMEZNEGA TESTA
        val record = TestRecord(
            id = System.currentTimeMillis(),
            profileId = profile.id,
            articleCode = profile.articleCode,
            articleName = profile.articleName,
            date = date,
            passed = passed,
            errors = errors
        )

        // DODA ZAPIS V ZGODOVINO VSEH TESTOV
        testRecords = testRecords + record

        // TRAJNO SHRANI ZGODOVINO
        repository.saveTestRecords(testRecords)

        return TestComparison(
            passed = passed,
            errors = errors
        )
    }

    fun loadDemoProfiles() {
        if (profiles.isNotEmpty()) return

        addProfile(
            CableProfile(
                id = 1,
                name = "Kabel motorja",
                articleCode = "KAB-001",
                articleName = "Kabel motorja",
                notes = "Demo profil",
                created = "2026-07-21"
            )
        )

        addProfile(
            CableProfile(
                id = 2,
                name = "Adapter DB9",
                articleCode = "DB9-001",
                articleName = "Adapter DB9",
                notes = "Demo profil",
                created = "2026-07-21"
            )
        )
    }

    // PRIPRAVA JSON VARNOSTNE KOPIJE
    fun createBackupJson(): String {
        return repository.createBackupJson()
    }

    // OBNOVA JSON VARNOSTNE KOPIJE
    fun restoreBackupJson(json: String): Boolean {
        val success = repository.restoreBackupJson(json)

        if (success) {
            profiles = repository.loadProfiles()
            testRecords = repository.loadTestRecords()

            selectedProfile = profiles.find {
                it.id == repository.getLastProfileId()
            }
        }

        return success
    }
}

// PODATKOVNI RAZRED REZULTATA TESTA
data class TestComparison(
    val passed: Boolean,
    val errors: List<String>
)