package com.example.timekeeper

import android.content.Context

fun getAvailableSyncServices(context: Context): Set<SyncService> {
    val available = mutableSetOf<SyncService>()

    val driveAccount = LocalPersistence.loadDriveAccountEmail(context)
    if (!driveAccount.isNullOrBlank()) {
        available.add(SyncService.GOOGLE_DRIVE)
    }

    val nc = LocalPersistence.loadNextcloudSettings(context)
    if (
        nc.serverUrl.isNotBlank() &&
        nc.username.isNotBlank() &&
        nc.appPassword.isNotBlank()
    ) {
        available.add(SyncService.NEXTCLOUD)
    }

    return available
}