package com.example.timekeeper

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as JavaFile

object GoogleDriveSyncManager {

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
            val existingMapping = driveMappings.firstOrNull { it.windowKey == windowKey }

            val mimeType = "text/csv"
            val mediaContent = FileContent(mimeType, currentFile)

            val remoteFileId = if (existingMapping == null) {
                val metadata = com.google.api.services.drive.model.File().apply {
                    name = currentFile.name
                    this.mimeType = mimeType
                }

                val created = driveService.files()
                    .create(metadata, mediaContent)
                    .setFields("id,name")
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
                    .setFields("id,name")
                    .execute()

                existingMapping.driveFileId
            }

            LocalPersistence.saveDriveAccountEmail(context, account.email)
            Result.success(remoteFileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
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