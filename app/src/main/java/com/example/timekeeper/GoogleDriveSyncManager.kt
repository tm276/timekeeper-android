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
    private lateinit var persistence: LocalPersistence

    fun initialize(context: Context) {
        persistence = LocalPersistence(context)
        driveMappings.clear()
        driveMappings.addAll(persistence.loadDriveMappings())
    }

    suspend fun syncCurrentWindow(
        context: Context,
        store: TimeLogStore,
        client: ClientProfile,
        account: GoogleSignInAccount
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val currentFile: JavaFile = CsvWindowManager.writeCurrentWindowCsv(
                context = context,
                client = client,
                settings = store.settings,
                entries = store.getEntriesForClient(client.id),
                nowMillis = System.currentTimeMillis()
            )

            val window = CsvWindowManager.calculateWindow(
                settings = store.settings,
                targetMillis = System.currentTimeMillis()
            )

            val windowKey = "${client.id}_${window.startDate}_${window.endDate}"
            val driveService = createDriveService(context, account)

            val targetFolder = buildClientRemoteFolder(
                baseFolder = client.googleDriveFolder,
                clientName = client.clientName
            )

            val folderId = ensureFolderPathExists(
                driveService = driveService,
                folderPath = targetFolder
            )

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
                persistence.saveDriveMappings(driveMappings)

                newId
            } else {
                driveService.files()
                    .update(existingMapping.driveFileId, null, mediaContent)
                    .setAddParents(folderId)
                    .setFields("id,name,parents")
                    .execute()

                existingMapping.driveFileId
            }

            val sharedAccountEmail = account.email.orEmpty()
            if (sharedAccountEmail.isNotBlank()) {
                store.clients.forEach { existingClient ->
                    if (existingClient.googleDriveAccount != sharedAccountEmail) {
                        store.updateClient(
                            existingClient.copy(
                                googleDriveAccount = sharedAccountEmail
                            )
                        )
                    }
                }
            }

            Result.success(remoteFileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildClientRemoteFolder(baseFolder: String, clientName: String): String {
        val base = baseFolder.trim('/').ifBlank { ROOT_FOLDER_NAME }
        val safeClientName = sanitizeDriveFolderSegment(clientName)
        val lastSegment = base.split('/').lastOrNull().orEmpty()

        return if (lastSegment.equals(safeClientName, ignoreCase = true) ||
            lastSegment.equals(clientName.trim(), ignoreCase = true)
        ) {
            base
        } else {
            "$base/$safeClientName"
        }
    }

    private fun sanitizeDriveFolderSegment(value: String): String {
        return value
            .trim()
            .replace('/', '_')
            .replace('\\', '_')
            .ifBlank { "Client" }
    }

    private fun ensureFolderPathExists(
        driveService: Drive,
        folderPath: String
    ): String {
        val segments = folderPath.split('/').filter { it.isNotBlank() }
        var parentId: String? = null

        for (segment in segments) {
            parentId = ensureSingleFolderExists(
                driveService = driveService,
                folderName = segment,
                parentId = parentId
            )
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
