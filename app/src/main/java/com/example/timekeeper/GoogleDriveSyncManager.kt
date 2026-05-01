package com.example.timekeeper

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as JavaFile

object GoogleDriveSyncManager {

    private const val ROOT_FOLDER_NAME = "TimeKeeper"
    private val driveMappings = mutableListOf<DriveFileMapping>()

    fun initialize(context: Context) {
        driveMappings.clear()
        driveMappings.addAll(LocalPersistence.loadDriveMappings(context))
    }

    suspend fun syncCurrentWindow(
        context: Context,
        account: GoogleSignInAccount
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val csvFile = TimeLogStore.currentCsvFile(context)

            if (!csvFile.exists()) {
                CsvWindowManager.writeCurrentWindowCsv(
                    context = context,
                    settings = TimeLogStore.settings,
                    entries = TimeLogStore.entries,
                    nowMillis = System.currentTimeMillis()
                )
            }

            val currentFile: JavaFile = TimeLogStore.currentCsvFile(context)

            val window = CsvWindowManager.calculateWindow(
                settings = TimeLogStore.settings,
                targetMillis = System.currentTimeMillis()
            )

            val windowKey = "${window.startDate}_${window.endDate}"

            val driveService = createDriveService(context, account)
            val folderId = ensureFolderPathExists(driveService, ROOT_FOLDER_NAME)
            val existingMapping = driveMappings.firstOrNull { it.windowKey == windowKey }

            val mimeType = "text/csv"
            val mediaContent = FileContent(mimeType, currentFile)

            val remoteFileId = if (existingMapping == null) {
                val metadata = File().apply {
                    name = currentFile.name
                    this.mimeType = mimeType
                    parents = listOf(folderId)
                }

                val created = driveService.files()
                    .create(metadata, mediaContent)
                    .setFields("id,name,parents")
                    .execute()

                val newId = created.id

                driveMappings.removeAll { it.windowKey == windowKey }
                driveMappings.add(
                    DriveFileMapping(
                        windowKey = windowKey,
                        driveFileId = newId
                    )
                )
                LocalPersistence.saveDriveMappings(context, driveMappings)
                newId
            } else {
                driveService.files()
                    .update(existingMapping.driveFileId, null, mediaContent)
                    .setAddParents(folderId)
                    .setFields("id,name,parents")
                    .execute()

                existingMapping.driveFileId
            }

            LocalPersistence.saveDriveAccountEmail(context, account.email)
            Result.success(remoteFileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ensureFolderPathExists(driveService: Drive, folderPath: String): String {
        val segments = folderPath.split('/').filter { it.isNotBlank() }
        var parentId: String? = null

        for (segment in segments) {
            parentId = ensureSingleFolderExists(driveService, segment, parentId)
        }

        return parentId ?: throw IllegalStateException("Drive folder path is empty.")
    }

    private fun ensureSingleFolderExists(
        driveService: Drive,
        folderName: String,
        parentId: String?
    ): String {
        val escapedName = folderName.replace("'", "\\'")
        val query = buildString {
            append("mimeType = 'application/vnd.google-apps.folder' and trashed = false and name = '")
            append(escapedName)
            append("'")
            if (parentId == null) {
                append(" and 'root' in parents")
            } else {
                append(" and '")
                append(parentId)
                append("' in parents")
            }
        }

        val existing = driveService.files()
            .list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id,name)")
            .setPageSize(1)
            .execute()
            .files
            .firstOrNull()

        if (existing != null) {
            return existing.id
        }

        val metadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
            parents = listOf(parentId ?: "root")
        }

        return driveService.files()
            .create(metadata)
            .setFields("id,name")
            .execute()
            .id
    }

    private fun createDriveService(
        context: Context,
        account: GoogleSignInAccount
    ): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            setOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("TimeKeeper")
            .build()
    }
}