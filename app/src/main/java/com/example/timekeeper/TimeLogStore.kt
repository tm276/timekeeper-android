package com.example.timekeeper

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.UUID

class TimeLogStore(context: Context) {

    private val appContext = context.applicationContext
    private val persistence = LocalPersistence(appContext)

    var settings by mutableStateOf(persistence.loadSettings())
        private set

    val clients = mutableStateListOf<ClientProfile>()

    val entries = mutableStateListOf<TimeEntry>().apply {
        addAll(persistence.loadEntries())
    }
    var activeClientId by mutableStateOf(persistence.loadActiveClientId())
        private set

    var activeStartMillis by mutableStateOf<Long?>(null)
        private set

    init {
        val loadedClients = persistence.loadClients()
        if (loadedClients.isEmpty()) {
            val defaultClient = ClientProfile(
                id = UUID.randomUUID().toString(),
                clientName = "Default Client",
                userName = settings.userName
            )
            clients.add(defaultClient)
            persistence.saveClients(clients)
        } else {
            clients.addAll(loadedClients)
        }

        if (activeClientId != null && clients.none { it.id == activeClientId }) {
            activeClientId = null
            persistence.clearActiveClientId()
        }
    }

    fun updateSettings(newSettings: TimeSettings) {
        settings = newSettings
        persistence.saveSettings(newSettings)
    }

    fun addClient(
        clientName: String,
        userName: String = settings.userName,
        csvFileName: String = "time_log.csv",
        localFolder: String = "",
        googleDriveAccount: String = "",
        googleDriveFolder: String = "",
        nextcloudUrl: String = "",
        nextcloudUser: String = "",
        nextcloudPassword: String = "",
        nextcloudFolder: String = "",
        autoSyncEnabled: Boolean = false,
        syncGoogleDriveEnabled: Boolean = false,
        syncNextcloudEnabled: Boolean = false
    ) {
        val client = ClientProfile(
            id = UUID.randomUUID().toString(),
            clientName = clientName,
            userName = userName,
            csvFileName = csvFileName,
            localFolder = localFolder,
            googleDriveAccount = googleDriveAccount,
            googleDriveFolder = googleDriveFolder,
            nextcloudUrl = nextcloudUrl,
            nextcloudUser = nextcloudUser,
            nextcloudPassword = nextcloudPassword,
            nextcloudFolder = nextcloudFolder,
            autoSyncEnabled = autoSyncEnabled,
            syncGoogleDriveEnabled = syncGoogleDriveEnabled,
            syncNextcloudEnabled = syncNextcloudEnabled
        )
        clients.add(client)
        persistence.saveClients(clients)
    }

    fun updateClient(updatedClient: ClientProfile) {
        val index = clients.indexOfFirst { it.id == updatedClient.id }
        if (index >= 0) {
            clients[index] = updatedClient
            persistence.saveClients(clients)
        }
    }

    fun deleteClient(clientId: String) {
        val client = getClientById(clientId) ?: return
        val wasActive = activeClientId == clientId

        if (wasActive) {
            cancelActiveTimer()
        }

        deleteAllLocalFilesForClient(client)

        clients.removeAll { it.id == clientId }
        persistence.saveClients(clients)

        if (clients.isEmpty()) {
            val fallback = ClientProfile(
                id = UUID.randomUUID().toString(),
                clientName = "Default Client",
                userName = settings.userName
            )
            clients.add(fallback)
            persistence.saveClients(clients)
        }
    }

    fun deleteAllLocalFilesForClient(clientId: String): Int {
        val client = getClientById(clientId) ?: return 0
        return deleteAllLocalFilesForClient(client)
    }

    fun deleteLocalFileForClient(clientId: String, fileName: String): Boolean {
        val client = getClientById(clientId) ?: return false
        val targetFile = getLocalFilesForClient(client)
            .firstOrNull { it.name == fileName }
            ?: return false

        return targetFile.delete()
    }

    fun getLocalFilesForClient(clientId: String): List<File> {
        val client = getClientById(clientId) ?: return emptyList()
        return getLocalFilesForClient(client)
    }

    fun getClientById(clientId: String?): ClientProfile? {
        return clients.firstOrNull { it.id == clientId }
    }

    fun startTimer(clientId: String) {
        activeClientId = clientId
        activeStartMillis = System.currentTimeMillis()
        persistence.saveActiveClientId(clientId)
    }

    fun stopTimer(description: String) {
        val clientId = activeClientId ?: return
        val startMillis = activeStartMillis ?: return
        val stopMillis = System.currentTimeMillis()
        val durationMinutes = ((stopMillis - startMillis) / 60000L).coerceAtLeast(1L)

        entries.add(
            TimeEntry(
                clientId = clientId,
                startMillis = startMillis,
                stopMillis = stopMillis,
                description = description,
                durationMinutes = durationMinutes
            )
        )

        persistence.saveEntries(entries)

        activeClientId = null
        activeStartMillis = null
        persistence.clearActiveClientId()
    }

    fun cancelActiveTimer() {
        activeClientId = null
        activeStartMillis = null
        persistence.clearActiveClientId()
    }

    fun getEntriesForClient(clientId: String): List<TimeEntry> {
        return entries.filter { it.clientId == clientId }
    }

    fun isClientActive(clientId: String): Boolean {
        return activeClientId == clientId
    }

    fun getActiveClient(): ClientProfile? {
        return getClientById(activeClientId)
    }

    private fun deleteAllLocalFilesForClient(client: ClientProfile): Int {
        val files = getLocalFilesForClient(client)
        var deletedCount = 0

        files.forEach { file ->
            if (file.delete()) {
                deletedCount += 1
            }
        }

        return deletedCount
    }

    private fun getLocalFilesForClient(client: ClientProfile): List<File> {
        val safeClientName = sanitizeFileName(client.clientName)

        return appContext.filesDir
            .listFiles()
            ?.filter { file ->
                file.isFile &&
                        file.name.startsWith("timelog_${safeClientName}_") &&
                        file.name.endsWith(".csv")
            }
            ?.sortedByDescending { it.name }
            .orEmpty()
    }

    private fun sanitizeFileName(value: String): String {
        return value
            .trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "client" }
    }
}
