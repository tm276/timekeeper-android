package com.example.timekeeper

data class NextcloudSettings(
    val serverUrl: String,
    val username: String,
    val appPassword: String,
    val remoteFolder: String
) {
    companion object {
        fun default(): NextcloudSettings {
            return NextcloudSettings(
                serverUrl = "",
                username = "",
                appPassword = "",
                remoteFolder = "TimeKeeper"
            )
        }
    }
}