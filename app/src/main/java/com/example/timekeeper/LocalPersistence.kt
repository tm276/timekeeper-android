package com.example.timekeeper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalPersistence(context: Context) {

    private val prefs = context.getSharedPreferences("timekeeper_prefs", Context.MODE_PRIVATE)

    // -------------------------
    // SETTINGS
    // -------------------------
    fun saveSettings(settings: TimeSettings) {
        val json = JSONObject().apply {
            put("anchorMillis", settings.anchorMillis)
            put("durationAmount", settings.durationAmount)
            put("durationUnit", settings.durationUnit.name)
            put("userName", settings.userName)
        }
        prefs.edit().putString("settings", json.toString()).apply()
    }

    fun loadSettings(): TimeSettings {
        val json = prefs.getString("settings", null) ?: return TimeSettings.default()
        val obj = JSONObject(json)
        return TimeSettings(
            anchorMillis = obj.optLong("anchorMillis", System.currentTimeMillis()),
            durationAmount = obj.optInt("durationAmount", 7),
            durationUnit = DurationUnit.valueOf(
                obj.optString("durationUnit", DurationUnit.DAYS.name)
            ),
            userName = obj.optString("userName", "")
        )
    }

    // -------------------------
    // CLIENTS
    // -------------------------
    fun saveClients(clients: List<ClientProfile>) {
        val array = JSONArray()
        clients.forEach { client ->
            val obj = JSONObject().apply {
                put("id", client.id)
                put("clientName", client.clientName)
                put("userName", client.userName)
                put("csvFileName", client.csvFileName)
                put("localFolder", client.localFolder)

                put("googleDriveAccount", client.googleDriveAccount)
                put("googleDriveFolder", client.googleDriveFolder)

                put("nextcloudUrl", client.nextcloudUrl)
                put("nextcloudUser", client.nextcloudUser)
                put("nextcloudPassword", client.nextcloudPassword)
                put("nextcloudFolder", client.nextcloudFolder)

                put("autoSyncEnabled", client.autoSyncEnabled)
                put("syncGoogleDriveEnabled", client.syncGoogleDriveEnabled)
                put("syncNextcloudEnabled", client.syncNextcloudEnabled)
            }
            array.put(obj)
        }
        prefs.edit().putString("clients", array.toString()).apply()
    }

    fun loadClients(): List<ClientProfile> {
        val json = prefs.getString("clients", null) ?: return emptyList()
        val array = JSONArray(json)
        val result = mutableListOf<ClientProfile>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            val client = ClientProfile(
                id = obj.getString("id"),
                clientName = obj.getString("clientName"),
                userName = obj.optString("userName", ""),
                csvFileName = obj.optString("csvFileName", "time_log.csv"),
                localFolder = obj.optString("localFolder", ""),
                googleDriveAccount = obj.optString("googleDriveAccount", ""),
                googleDriveFolder = obj.optString("googleDriveFolder", ""),
                nextcloudUrl = obj.optString("nextcloudUrl", ""),
                nextcloudUser = obj.optString("nextcloudUser", ""),
                nextcloudPassword = obj.optString("nextcloudPassword", ""),
                nextcloudFolder = obj.optString("nextcloudFolder", ""),
                autoSyncEnabled = obj.optBoolean("autoSyncEnabled", false),
                syncGoogleDriveEnabled = obj.optBoolean("syncGoogleDriveEnabled", false),
                syncNextcloudEnabled = obj.optBoolean("syncNextcloudEnabled", false)
            )

            result.add(client)
        }

        return result
    }

    // -------------------------
    // ENTRIES
    // -------------------------
    fun saveEntries(entries: List<TimeEntry>) {
        val array = JSONArray()

        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("clientId", entry.clientId)
                put("startMillis", entry.startMillis)
                put("stopMillis", entry.stopMillis)
                put("description", entry.description)
                put("durationMinutes", entry.durationMinutes)
            }
            array.put(obj)
        }

        prefs.edit().putString("entries", array.toString()).apply()
    }

    fun loadEntries(): List<TimeEntry> {
        val json = prefs.getString("entries", null) ?: return emptyList()
        val array = JSONArray(json)

        val result = mutableListOf<TimeEntry>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            result.add(
                TimeEntry(
                    clientId = obj.getString("clientId"),
                    startMillis = obj.getLong("startMillis"),
                    stopMillis = obj.getLong("stopMillis"),
                    description = obj.optString("description", ""),
                    durationMinutes = obj.getLong("durationMinutes")
                )
            )
        }

        return result
    }

    // -------------------------
    // DRIVE MAPPINGS
    // -------------------------
    fun saveDriveMappings(mappings: List<DriveFileMapping>) {
        val array = JSONArray()

        mappings.forEach { mapping ->
            val obj = JSONObject().apply {
                put("windowKey", mapping.windowKey)
                put("driveFileId", mapping.driveFileId)
            }
            array.put(obj)
        }

        prefs.edit().putString("drive_mappings", array.toString()).apply()
    }

    fun loadDriveMappings(): List<DriveFileMapping> {
        val json = prefs.getString("drive_mappings", null) ?: return emptyList()
        val array = JSONArray(json)
        val result = mutableListOf<DriveFileMapping>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            result.add(
                DriveFileMapping(
                    windowKey = obj.optString("windowKey", ""),
                    driveFileId = obj.optString("driveFileId", "")
                )
            )
        }

        return result
    }

    // -------------------------
    // ACTIVE TIMER
    // -------------------------
    fun saveActiveClientId(clientId: String?) {
        prefs.edit().putString("activeClientId", clientId).apply()
    }

    fun loadActiveClientId(): String? {
        return prefs.getString("activeClientId", null)
    }

    fun clearActiveClientId() {
        prefs.edit().remove("activeClientId").apply()
    }

    fun saveActiveStartMillis(startMillis: Long?) {
        if (startMillis == null) {
            prefs.edit().remove("activeStartMillis").apply()
        } else {
            prefs.edit().putLong("activeStartMillis", startMillis).apply()
        }
    }

    fun loadActiveStartMillis(): Long? {
        return if (prefs.contains("activeStartMillis")) {
            prefs.getLong("activeStartMillis", 0L)
        } else {
            null
        }
    }

    fun clearActiveStartMillis() {
        prefs.edit().remove("activeStartMillis").apply()
    }
}
