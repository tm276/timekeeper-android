package com.example.timekeeper

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object NextcloudSyncManager {

    suspend fun syncCurrentWindow(
        context: Context,
        settings: NextcloudSettings
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (
                settings.serverUrl.isBlank() ||
                settings.username.isBlank() ||
                settings.appPassword.isBlank() ||
                settings.remoteFolder.isBlank()
            ) {
                return@withContext Result.failure(
                    IllegalArgumentException("Nextcloud settings are incomplete.")
                )
            }

            val currentFile = TimeLogStore.currentCsvFile(context)
            if (!currentFile.exists()) {
                CsvWindowManager.writeCurrentWindowCsv(
                    context = context,
                    settings = TimeLogStore.settings,
                    entries = TimeLogStore.entries,
                    nowMillis = System.currentTimeMillis()
                )
            }

            ensureFolderExists(settings)

            val encodedUsername = URLEncoder.encode(settings.username, "UTF-8")
            val trimmedServer = settings.serverUrl.trimEnd('/')
            val trimmedFolder = settings.remoteFolder.trim('/')

            val remoteFileUrl =
                "$trimmedServer/remote.php/dav/files/$encodedUsername/$trimmedFolder/${currentFile.name}"

            val auth = basicAuth(settings.username, settings.appPassword)

            val connection = (URL(remoteFileUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Authorization", auth)
                setRequestProperty("Content-Type", "text/csv")
            }

            currentFile.inputStream().use { input ->
                BufferedInputStream(input).copyTo(connection.outputStream)
            }

            val code = connection.responseCode
            if (code !in 200..299 && code != 201 && code != 204) {
                return@withContext Result.failure(
                    IllegalStateException("Nextcloud upload failed with HTTP $code")
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ensureFolderExists(settings: NextcloudSettings) {
        val encodedUsername = URLEncoder.encode(settings.username, "UTF-8")
        val trimmedServer = settings.serverUrl.trimEnd('/')
        val trimmedFolder = settings.remoteFolder.trim('/')

        val folderUrl =
            "$trimmedServer/remote.php/dav/files/$encodedUsername/$trimmedFolder"

        val auth = basicAuth(settings.username, settings.appPassword)

        val connection = (URL(folderUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "MKCOL"
            setRequestProperty("Authorization", auth)
        }

        val code = connection.responseCode

        if (code !in listOf(201, 405)) {
            throw IllegalStateException("Could not create/access remote folder. HTTP $code")
        }
    }

    private fun basicAuth(username: String, password: String): String {
        val raw = "$username:$password"
        val encoded = Base64.encodeToString(raw.toByteArray(), Base64.NO_WRAP)
        return "Basic $encoded"
    }
}