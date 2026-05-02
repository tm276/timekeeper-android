package com.example.timekeeper

data class ClientProfile(
    val id: String,
    val clientName: String,
    val userName: String = "",
    val csvFileName: String = "time_log.csv",

    // Local storage
    val localFolder: String = "",

    // Google Drive
    val googleDriveAccount: String = "",
    val googleDriveFolder: String = "",

    // Nextcloud
    val nextcloudUrl: String = "",
    val nextcloudUser: String = "",
    val nextcloudPassword: String = "",
    val nextcloudFolder: String = "",

    // 🔧 NEW: Auto sync settings
    val autoSyncEnabled: Boolean = false,
    val syncGoogleDriveEnabled: Boolean = false,
    val syncNextcloudEnabled: Boolean = false
)