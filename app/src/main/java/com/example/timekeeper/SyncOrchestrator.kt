package com.example.timekeeper

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncOrchestrator {

    suspend fun sync(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val available = getAvailableSyncServices(context)
            if (available.isEmpty()) {
                return@withContext Result.success(Unit)
            }

            val appContext = context.applicationContext
            val store = TimeLogStore(appContext)
            val client = store.getActiveClient() ?: store.clients.firstOrNull()
            ?: return@withContext Result.failure(
                IllegalStateException("No client is available to sync.")
            )

            if (SyncService.GOOGLE_DRIVE in available) {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account != null) {
                    GoogleDriveSyncManager.initialize(appContext)
                    val driveResult = GoogleDriveSyncManager.syncCurrentWindow(
                        context = appContext,
                        store = store,
                        client = client,
                        account = account
                    )
                    if (driveResult.isFailure) {
                        return@withContext Result.failure(
                            driveResult.exceptionOrNull()
                                ?: IllegalStateException("Google Drive sync failed.")
                        )
                    }
                }
            }

            if (SyncService.NEXTCLOUD in available) {
                val settings = NextcloudSettings(
                    serverUrl = client.nextcloudUrl,
                    username = client.nextcloudUser,
                    appPassword = client.nextcloudPassword,
                    remoteFolder = client.nextcloudFolder.ifBlank { "TimeKeeper" }
                )
                val nextcloudResult = NextcloudSyncManager.syncCurrentWindow(
                    context = appContext,
                    store = store,
                    clientProfile = client,
                    settings = settings
                )
                if (nextcloudResult.isFailure) {
                    return@withContext Result.failure(
                        nextcloudResult.exceptionOrNull()
                            ?: IllegalStateException("Nextcloud sync failed.")
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
