package com.example.timekeeper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocalPersistence {
    private const val PREFS_NAME = "timekeeper_prefs"
    private const val KEY_ENTRIES = "entries"
    private const val KEY_ACTIVE_START = "active_start"
    private const val KEY_SETTINGS = "settings"
    private const val KEY_NEXTCLOUD_SETTINGS = "nextcloud_settings"
    private const val KEY_DRIVE_MAPPINGS = "drive_mappings"
    private const val KEY_DRIVE_ACCOUNT_EMAIL = "drive_account_email"
    fun saveEntries(context: Context, entries: List<TimeEntry>) {
        val array = JSONArray()

        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("startMillis", entry.startMillis)
                put("stopMillis", entry.stopMillis)
                put("description", entry.description)
            }
            array.put(obj)
        }

        prefs(context)
            .edit()
            .putString(KEY_ENTRIES, array.toString())
            .apply()
    }

    fun loadEntries(context: Context): MutableList<TimeEntry> {
        val raw = prefs(context).getString(KEY_ENTRIES, null) ?: return mutableListOf()
        val array = JSONArray(raw)
        val entries = mutableListOf<TimeEntry>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            entries.add(
                TimeEntry(
                    id = obj.getString("id"),
                    startMillis = obj.getLong("startMillis"),
                    stopMillis = obj.getLong("stopMillis"),
                    description = obj.getString("description")
                )
            )
        }

        return entries
    }

    fun saveActiveStart(context: Context, activeStartMillis: Long) {
        prefs(context)
            .edit()
            .putLong(KEY_ACTIVE_START, activeStartMillis)
            .apply()
    }

    fun loadActiveStart(context: Context): Long {
        return prefs(context).getLong(KEY_ACTIVE_START, 0L)
    }

    fun saveSettings(context: Context, settings: TimeSettings) {
        val obj = JSONObject().apply {
            put("anchorMillis", settings.anchorMillis)
            put("durationAmount", settings.durationAmount)
            put("durationUnit", settings.durationUnit.name)
        }

        prefs(context)
            .edit()
            .putString(KEY_SETTINGS, obj.toString())
            .apply()
    }

    fun loadSettings(context: Context): TimeSettings {
        val raw = prefs(context).getString(KEY_SETTINGS, null)
            ?: return TimeSettings.default()

        val obj = JSONObject(raw)

        return TimeSettings(
            anchorMillis = obj.getLong("anchorMillis"),
            durationAmount = obj.getInt("durationAmount"),
            durationUnit = DurationUnit.valueOf(obj.getString("durationUnit"))
        )
    }

    fun saveNextcloudSettings(context: Context, settings: NextcloudSettings) {
        val obj = JSONObject().apply {
            put("serverUrl", settings.serverUrl)
            put("username", settings.username)
            put("appPassword", settings.appPassword)
            put("remoteFolder", settings.remoteFolder)
        }

        prefs(context)
            .edit()
            .putString(KEY_NEXTCLOUD_SETTINGS, obj.toString())
            .apply()
    }

    fun loadNextcloudSettings(context: Context): NextcloudSettings {
        val raw = prefs(context).getString(KEY_NEXTCLOUD_SETTINGS, null)
            ?: return NextcloudSettings.default()

        val obj = JSONObject(raw)

        return NextcloudSettings(
            serverUrl = obj.getString("serverUrl"),
            username = obj.getString("username"),
            appPassword = obj.getString("appPassword"),
            remoteFolder = obj.getString("remoteFolder")
        )
    }

    fun saveDriveMappings(context: Context, mappings: List<DriveFileMapping>) {
        val array = JSONArray()
        mappings.forEach { mapping ->
            val obj = JSONObject().apply {
                put("windowKey", mapping.windowKey)
                put("driveFileId", mapping.driveFileId)
            }
            array.put(obj)
        }

        prefs(context)
            .edit()
            .putString(KEY_DRIVE_MAPPINGS, array.toString())
            .apply()
    }

    fun loadDriveMappings(context: Context): MutableList<DriveFileMapping> {
        val raw = prefs(context).getString(KEY_DRIVE_MAPPINGS, null) ?: return mutableListOf()
        val array = JSONArray(raw)
        val mappings = mutableListOf<DriveFileMapping>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            mappings.add(
                DriveFileMapping(
                    windowKey = obj.getString("windowKey"),
                    driveFileId = obj.getString("driveFileId")
                )
            )
        }

        return mappings
    }

    fun saveDriveAccountEmail(context: Context, email: String?) {
        prefs(context)
            .edit()
            .putString(KEY_DRIVE_ACCOUNT_EMAIL, email)
            .apply()
    }

    fun loadDriveAccountEmail(context: Context): String? {
        return prefs(context).getString(KEY_DRIVE_ACCOUNT_EMAIL, null)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}