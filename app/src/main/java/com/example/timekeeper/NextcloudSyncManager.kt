package com.example.timekeeper

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLEncoder

object NextcloudSyncManager {

    private val client = OkHttpClient()

    suspend fun syncCurrentWindow(
        context: Context,
        settings: NextcloudSettings
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (
                settings.serverUrl.isBlank() ||
                settings.username.isBlank() ||
                settings.appPassword.isBlank()
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

            val encodedUsername = encodeDavSegment(settings.username)
            val trimmedServer = settings.serverUrl.trimEnd('/')
            val trimmedFolder = settings.remoteFolder.trim('/').ifBlank { "TimeKeeper" }
            val auth = basicAuth(settings.username, settings.appPassword)

            ensureRemoteFolderExists(
                serverUrl = trimmedServer,
                encodedUsername = encodedUsername,
                remoteFolder = trimmedFolder,
                auth = auth
            )

            val remoteFolderPath = encodeRemotePath(trimmedFolder)
            val encodedFileName = encodeDavSegment(currentFile.name)
            val remoteFileUrl =
                "$trimmedServer/remote.php/dav/files/$encodedUsername/$remoteFolderPath/$encodedFileName"

            uploadFile(
                file = currentFile,
                remoteFileUrl = remoteFileUrl,
                auth = auth
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ensureRemoteFolderExists(
        serverUrl: String,
        encodedUsername: String,
        remoteFolder: String,
        auth: String
    ) {
        val segments = remoteFolder.split('/').filter { it.isNotBlank() }
        var currentPath = ""

        for (segment in segments) {
            currentPath = if (currentPath.isBlank()) segment else "$currentPath/$segment"
            val remoteFolderUrl =
                "$serverUrl/remote.php/dav/files/$encodedUsername/${encodeRemotePath(currentPath)}"

            val request = Request.Builder()
                .url(remoteFolderUrl)
                .header("Authorization", auth)
                .method("MKCOL", null)
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                if (code !in 200..299 && code != 301 && code != 405) {
                    throw IllegalStateException(
                        "Nextcloud folder creation failed for '$currentPath' with HTTP $code"
                    )
                }
            }
        }
    }

    private fun uploadFile(
        file: File,
        remoteFileUrl: String,
        auth: String
    ) {
        val request = Request.Builder()
            .url(remoteFileUrl)
            .header("Authorization", auth)
            .put(file.asRequestBody("text/csv".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val code = response.code
            if (code !in 200..299 && code != 201 && code != 204) {
                throw IllegalStateException("Nextcloud upload failed with HTTP $code")
            }
        }
    }

    private fun encodeRemotePath(path: String): String {
        return path
            .split('/')
            .filter { it.isNotBlank() }
            .joinToString("/") { encodeDavSegment(it) }
    }

    private fun encodeDavSegment(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    private fun basicAuth(username: String, password: String): String {
        val raw = "$username:$password"
        val encoded = Base64.encodeToString(raw.toByteArray(), Base64.NO_WRAP)
        return "Basic $encoded"
    }
}