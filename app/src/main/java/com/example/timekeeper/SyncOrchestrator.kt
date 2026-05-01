package com.example.timekeeper

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncOrchestrator {

    suspend fun sync(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val available = getAvailableSyncServices(context)
            val selected = LocalPersistence.loadSelectedSyncServices(context)
            val active = selected.intersect(available)

            if (active.isEmpty()) {
                return@withContext Result.success(Unit)
            }

            for (service in active) {
                when (service) {
                    SyncService.GOOGLE_DRIVE -> {
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        if (account != null) {
                            GoogleDriveSyncManager.syncCurrentWindow(context, account)
                        }
                    }

                    SyncService.NEXTCLOUD -> {
                        val settings = LocalPersistence.loadNextcloudSettings(context)
                        NextcloudSyncManager.syncCurrentWindow(context, settings)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}