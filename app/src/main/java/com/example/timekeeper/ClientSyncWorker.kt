package com.example.timekeeper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClientSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val result = SyncOrchestrator.sync(applicationContext)
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}