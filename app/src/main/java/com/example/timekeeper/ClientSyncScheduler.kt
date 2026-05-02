package com.example.timekeeper

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import java.util.concurrent.TimeUnit

object ClientSyncScheduler {
    private const val WORK_NAME_PREFIX = "client_periodic_sync_"
    private const val INPUT_CLIENT_ID = "client_id"

    fun schedule(context: Context, clientId: String) {
        val request = PeriodicWorkRequestBuilder<ClientSyncWorker>(15, TimeUnit.MINUTES)
            .setInputData(
                Data.Builder()
                    .putString(INPUT_CLIENT_ID, clientId)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWorkName(clientId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context, clientId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(clientId))
    }

    fun rescheduleIfEnabled(context: Context, client: ClientProfile) {
        if (client.autoSyncEnabled) {
            schedule(context, client.id)
        } else {
            cancel(context, client.id)
        }
    }

    private fun uniqueWorkName(clientId: String): String = WORK_NAME_PREFIX + clientId
}
