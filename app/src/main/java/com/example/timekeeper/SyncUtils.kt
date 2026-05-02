package com.example.timekeeper

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

fun getAvailableSyncServices(context: Context): Set<SyncService> {
    val available = mutableSetOf<SyncService>()
    val appContext = context.applicationContext
    val store = TimeLogStore(appContext)
    val client = store.getActiveClient() ?: store.clients.firstOrNull()

    val driveAccount = GoogleSignIn.getLastSignedInAccount(context)
    if (
        client != null &&
        driveAccount != null &&
        GoogleSignIn.hasPermissions(driveAccount, Scope(DriveScopes.DRIVE_FILE))
    ) {
        available.add(SyncService.GOOGLE_DRIVE)
    }

    if (
        client != null &&
        client.nextcloudUrl.isNotBlank() &&
        client.nextcloudUser.isNotBlank() &&
        client.nextcloudPassword.isNotBlank()
    ) {
        available.add(SyncService.NEXTCLOUD)
    }

    return available
}
