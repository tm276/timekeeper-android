package com.example.timekeeper
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
class LocalPersistence(context: Context) {
    private val prefs = context.getSharedPreferences("timekeeper_prefs", Context.MODE_PRIVATE)
    companion object {
        private const val KEY_SETTINGS = "settings_json"
        private const val KEY_CLIENTS = "clients_json"
        private const val KEY_ACTIVE_CLIENT_ID = "active_client_id"
        private const val KEY_DRIVE_MAPPINGS = "drive_mappings_json"
    }
    fun saveSettings(settings: TimeSettings) {
        val json = JSONObject().apply {
            put("anchorMillis", settings.anchorMillis)
            put("durationAmount", settings.durationAmount)
            put("durationUnit", settings.durationUnit.name)
            put("userName", settings.userName)
        }
        prefs.edit().putString(KEY_SETTINGS, json.toString()).apply()
    }
    fun loadSettings(): TimeSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return TimeSettings.default()
        return try {
            val json = JSONObject(raw)
            TimeSettings(
                anchorMillis = json.optLong("anchorMillis", System.currentTimeMillis()),
                durationAmount = json.optInt("durationAmount", 7),
                durationUnit = DurationUnit.valueOf(
                    json.optString("durationUnit", DurationUnit.DAYS.name)
                ),
                userName = json.optString("userName", "")
            )
        } catch (_: Exception) {
            TimeSettings.default()
        }
    }
    fun saveClients(clients: List<ClientProfile>) {
        val array = JSONArray()
        clients.forEach { client ->
            val json = JSONObject().apply {
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
            }
            array.put(json)
        }
        prefs.edit().putString(KEY_CLIENTS, array.toString()).apply()
    }
    fun loadClients(): List<ClientProfile> {
        val raw = prefs.getString(KEY_CLIENTS, null)
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val json = array.getJSONObject(i)
                    add(
                        ClientProfile(
                            id = json.optString("id", ""),
                            clientName = json.optString("clientName", "Client"),
                            userName = json.optString("userName", ""),
                            csvFileName = json.optString("csvFileName", "time_log.csv"),
                            localFolder = json.optString("localFolder", ""),
                            googleDriveAccount = json.optString("googleDriveAccount", ""),
                            googleDriveFolder = json.optString("googleDriveFolder", ""),
                            nextcloudUrl = json.optString("nextcloudUrl", ""),
                            nextcloudUser = json.optString("nextcloudUser", ""),
                            nextcloudPassword = json.optString("nextcloudPassword", ""),
                            nextcloudFolder = json.optString("nextcloudFolder", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    fun saveDriveMappings(mappings: List<DriveFileMapping>) {
        val array = JSONArray()
        mappings.forEach { mapping ->
            val json = JSONObject().apply {
                put("windowKey", mapping.windowKey)
                put("driveFileId", mapping.driveFileId)
            }
            array.put(json)
        }
        prefs.edit().putString(KEY_DRIVE_MAPPINGS, array.toString()).apply()
    }
    fun loadDriveMappings(): List<DriveFileMapping> {
        val raw = prefs.getString(KEY_DRIVE_MAPPINGS, null)
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val json = array.getJSONObject(i)
                    add(
                        DriveFileMapping(
                            windowKey = json.optString("windowKey", ""),
                            driveFileId = json.optString("driveFileId", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    fun saveActiveClientId(clientId: String?) {
        prefs.edit().putString(KEY_ACTIVE_CLIENT_ID, clientId).apply()
    }
    fun loadActiveClientId(): String? {
        return prefs.getString(KEY_ACTIVE_CLIENT_ID, null)
    }
    fun clearActiveClientId() {
        prefs.edit().remove(KEY_ACTIVE_CLIENT_ID).apply()
    }
}