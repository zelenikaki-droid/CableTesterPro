
package com.cabletester.pro.repository

import android.content.Context
import com.cabletester.pro.model.CableProfile
import com.cabletester.pro.model.PinConnection
import com.cabletester.pro.model.TestRecord
import org.json.JSONArray
import org.json.JSONObject

class ProfileRepository(context: Context) {

    private val prefs = context.getSharedPreferences(
        "cable_profiles",
        Context.MODE_PRIVATE
    )

    // SHRANJEVANJE PROFILOV
    fun saveProfiles(profiles: List<CableProfile>) {
        val array = JSONArray()

        profiles.forEach { profile ->
            val obj = JSONObject()

            obj.put("id", profile.id)
            obj.put("name", profile.name)
            obj.put("articleCode", profile.articleCode)
            obj.put("articleName", profile.articleName)
            obj.put("notes", profile.notes)
            obj.put("created", profile.created)
            obj.put("lastTest", profile.lastTest)
            obj.put("referenceLearned", profile.referenceLearned)
            obj.put("imagePath", profile.imagePath)

            if (profile.lastResult == null) {
                obj.put("lastResult", JSONObject.NULL)
            } else {
                obj.put("lastResult", profile.lastResult)
            }

            obj.put("passedTests", profile.passedTests)
            obj.put("failedTests", profile.failedTests)
            obj.put("learnedDate", profile.learnedDate)

            // Napake zadnjega testa
            val errorsArray = JSONArray()
            profile.testErrors.forEach { error ->
                errorsArray.put(error)
            }
            obj.put("testErrors", errorsArray)

            // Naučene povezave
            val connectionsArray = JSONArray()
            profile.connections.forEach { connection ->
                val connectionObj = JSONObject()
                connectionObj.put("fromChannel", connection.fromChannel)
                connectionObj.put("toChannel", connection.toChannel)
                connectionsArray.put(connectionObj)
            }
            obj.put("connections", connectionsArray)

            array.put(obj)
        }

        prefs.edit()
            .putString("profiles", array.toString())
            .apply()
    }

    // NALAGANJE PROFILOV
    fun loadProfiles(): List<CableProfile> {
        val json = prefs.getString("profiles", null)
            ?: return emptyList()

        val array = JSONArray(json)
        val list = mutableListOf<CableProfile>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            // Povezave
            val connections = mutableListOf<PinConnection>()
            val connectionsArray =
                obj.optJSONArray("connections") ?: JSONArray()

            for (j in 0 until connectionsArray.length()) {
                val connectionObj =
                    connectionsArray.getJSONObject(j)

                val from = connectionObj.optString("fromChannel", "")
                val to = connectionObj.optString("toChannel", "")

                if (from.isNotBlank() && to.isNotBlank()) {
                    connections.add(
                        PinConnection(
                            fromChannel = from,
                            toChannel = to
                        )
                    )
                }
            }

            // Napake
            val errors = mutableListOf<String>()
            val errorsArray =
                obj.optJSONArray("testErrors") ?: JSONArray()

            for (j in 0 until errorsArray.length()) {
                errors.add(errorsArray.optString(j))
            }

            val lastResult = if (
                obj.has("lastResult") &&
                !obj.isNull("lastResult")
            ) {
                obj.getBoolean("lastResult")
            } else {
                null
            }

            list.add(
                CableProfile(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", ""),
                    articleCode = obj.optString("articleCode", ""),
                    articleName = obj.optString("articleName", ""),
                    notes = obj.optString("notes", ""),
                    created = obj.optString("created", ""),
                    lastTest = obj.optString("lastTest", ""),
                    referenceLearned = obj.optBoolean(
                        "referenceLearned", false
                    ),
                    imagePath = obj.optString("imagePath", ""),
                    lastResult = lastResult,
                    passedTests = obj.optInt("passedTests", 0),
                    failedTests = obj.optInt("failedTests", 0),
                    testErrors = errors,
                    connections = connections,
                    learnedDate = obj.optString("learnedDate", "")
                )
            )
        }

        return list
    }

    // SHRANJEVANJE ZGODOVINE POSAMEZNIH TESTOV
    fun saveTestRecords(records: List<TestRecord>) {
        val array = JSONArray()

        records.forEach { record ->
            val obj = JSONObject()

            obj.put("id", record.id)
            obj.put("profileId", record.profileId)
            obj.put("articleCode", record.articleCode)
            obj.put("articleName", record.articleName)
            obj.put("date", record.date)
            obj.put("passed", record.passed)

            val errorsArray = JSONArray()
            record.errors.forEach { error ->
                errorsArray.put(error)
            }

            obj.put("errors", errorsArray)
            array.put(obj)
        }

        prefs.edit()
            .putString("test_records", array.toString())
            .apply()
    }

    // NALAGANJE ZGODOVINE POSAMEZNIH TESTOV
    fun loadTestRecords(): List<TestRecord> {
        val json = prefs.getString("test_records", null)
            ?: return emptyList()

        val array = JSONArray(json)
        val records = mutableListOf<TestRecord>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            val errors = mutableListOf<String>()
            val errorsArray =
                obj.optJSONArray("errors") ?: JSONArray()

            for (j in 0 until errorsArray.length()) {
                errors.add(errorsArray.optString(j))
            }

            records.add(
                TestRecord(
                    id = obj.optLong("id", 0L),
                    profileId = obj.optLong("profileId", 0L),
                    articleCode = obj.optString("articleCode", ""),
                    articleName = obj.optString("articleName", ""),
                    date = obj.optString("date", ""),
                    passed = obj.optBoolean("passed", false),
                    errors = errors
                )
            )
        }

        return records
    }

    // ZADNJI IZBRANI PROFIL
    fun saveLastProfileId(id: Long) {
        prefs.edit()
            .putLong("last_profile_id", id)
            .apply()
    }

    fun getLastProfileId(): Long {
        return prefs.getLong("last_profile_id", -1)
    }
    // USTVARJANJE VARNOSTNE KOPIJE
    fun createBackupJson(): String {
        val profilesArray = JSONArray()
        val recordsArray = JSONArray()

        // Profili
        loadProfiles().forEach { profile ->
            val obj = JSONObject()

            obj.put("id", profile.id)
            obj.put("name", profile.name)
            obj.put("articleCode", profile.articleCode)
            obj.put("articleName", profile.articleName)
            obj.put("notes", profile.notes)
            obj.put("created", profile.created)
            obj.put("lastTest", profile.lastTest)
            obj.put("referenceLearned", profile.referenceLearned)
            obj.put("imagePath", profile.imagePath)

            obj.put(
                "lastResult",
                profile.lastResult ?: JSONObject.NULL
            )

            obj.put("passedTests", profile.passedTests)
            obj.put("failedTests", profile.failedTests)
            obj.put("learnedDate", profile.learnedDate)

            val errorsArray = JSONArray()
            profile.testErrors.forEach { errorsArray.put(it) }
            obj.put("testErrors", errorsArray)

            val connectionsArray = JSONArray()
            profile.connections.forEach { connection ->
                val connectionObj = JSONObject()
                connectionObj.put("fromChannel", connection.fromChannel)
                connectionObj.put("toChannel", connection.toChannel)
                connectionsArray.put(connectionObj)
            }
            obj.put("connections", connectionsArray)

            profilesArray.put(obj)
        }

        // Zgodovina testiranj
        loadTestRecords().forEach { record ->
            val obj = JSONObject()

            obj.put("id", record.id)
            obj.put("profileId", record.profileId)
            obj.put("articleCode", record.articleCode)
            obj.put("articleName", record.articleName)
            obj.put("date", record.date)
            obj.put("passed", record.passed)

            val errorsArray = JSONArray()
            record.errors.forEach { errorsArray.put(it) }
            obj.put("errors", errorsArray)

            recordsArray.put(obj)
        }

        // Glavni objekt varnostne kopije
        val backup = JSONObject()
        backup.put("schemaVersion", 1)
        backup.put("createdAt", System.currentTimeMillis())
        backup.put("profiles", profilesArray)
        backup.put("testRecords", recordsArray)

        return backup.toString(2)
    }

    // OBNOVA VARNOSTNE KOPIJE
    // Vrne true, če je bila kopija uspešno sprejeta.
    fun restoreBackupJson(json: String): Boolean {
        return try {
            val backup = JSONObject(json)

            // Preverjanje različice
            if (backup.optInt("schemaVersion", -1) != 1) {
                return false
            }

            // Preverjanje obveznih polj
            val profilesArray = backup.optJSONArray("profiles")
                ?: return false

            val recordsArray = backup.optJSONArray("testRecords")
                ?: return false

            // Preverimo, da so vsi elementi objekti,
            // preden zamenjamo obstoječe podatke.
            for (i in 0 until profilesArray.length()) {
                profilesArray.getJSONObject(i)
            }

            for (i in 0 until recordsArray.length()) {
                recordsArray.getJSONObject(i)
            }

            // Obnovimo oba seznama skupaj.
            val editor = prefs.edit()
                .putString("profiles", profilesArray.toString())
                .putString("test_records", recordsArray.toString())

            // Preprečimo sklic na profil, ki ga v kopiji ni.
            val restoredProfiles = loadProfileIdsFromJson(profilesArray)
            val previousLastId = prefs.getLong("last_profile_id", -1L)

            val lastId = if (restoredProfiles.contains(previousLastId)) {
                previousLastId
            } else {
                restoredProfiles.firstOrNull() ?: -1L
            }

            editor.putLong("last_profile_id", lastId)
            editor.commit()
        } catch (e: Exception) {
            false
        }
    }

    // POMOŽNA FUNKCIJA ZA PREVERJANJE ID-JEV PROFILOV
    private fun loadProfileIdsFromJson(
        profilesArray: JSONArray
    ): List<Long> {
        val ids = mutableListOf<Long>()

        for (i in 0 until profilesArray.length()) {
            ids.add(profilesArray.getJSONObject(i).optLong("id", 0L))
        }

        return ids
    }
}