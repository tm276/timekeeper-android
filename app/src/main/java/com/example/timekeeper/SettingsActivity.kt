package com.example.timekeeper
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
private val SettingsAppBackground = Color(0xFF121212)
private val SettingsPanelBackground = Color(0xFF1E1E1E)
private val SettingsCardBackground = Color(0xFF263238)
private val SettingsPrimaryAction = Color(0xFF64B5F6)
private val SettingsSecondaryAction = Color(0xFF37474F)
private val SettingsDangerAction = Color(0xFFE57373)
private val SettingsPrimaryText = Color(0xFFF5F5F5)
private val SettingsSecondaryText = Color(0xFFCFD8DC)
class SettingsActivity : ComponentActivity() {
    companion object {
        const val EXTRA_CLIENT_ID = "client_id"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = SettingsAppBackground
            ) {
                SettingsScreen(
                    clientId = clientId,
                    onBack = { finish() }
                )
            }
        }
    }
}
@Composable
private fun SettingsScreen(
    clientId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val store = remember { TimeLogStore(appContext) }
    val scope = rememberCoroutineScope()
    val client = store.getClientById(clientId)
    if (client == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsAppBackground)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Client not found",
                    color = SettingsPrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) {
                    Text("Back")
                }
            }
        }
        return
    }
    var showNextcloudConnect by remember { mutableStateOf(false) }
    var showNextcloudManual by remember { mutableStateOf(false) }
    var showLocalFiles by remember { mutableStateOf(false) }
    var showDeleteEntriesConfirm by remember { mutableStateOf(false) }
    var nextcloudUrl by remember(client.id) { mutableStateOf(client.nextcloudUrl) }
    var nextcloudUser by remember(client.id) { mutableStateOf(client.nextcloudUser) }
    var nextcloudPassword by remember(client.id) { mutableStateOf(client.nextcloudPassword) }
    var nextcloudFolder by remember(client.id) {
        mutableStateOf(client.nextcloudFolder.ifBlank { "TimeKeeper" })
    }
    var autoSyncEnabled by remember(client.id) { mutableStateOf(client.autoSyncEnabled) }
    var syncNextcloudEnabled by remember(client.id) { mutableStateOf(client.syncNextcloudEnabled) }
    var isConnectingNextcloud by remember { mutableStateOf(false) }
    var isManualSyncRunning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var localFilesVersion by remember { mutableStateOf(0) }
    val selectedFiles = remember { mutableStateListOf<String>() }
    val localFiles = remember(client.id, localFilesVersion) {
        store.getLocalFilesForClient(client.id)
    }
    fun saveClient(
        url: String = nextcloudUrl,
        user: String = nextcloudUser,
        password: String = nextcloudPassword,
        folder: String = nextcloudFolder,
        autoSync: Boolean = autoSyncEnabled,
        nextcloudSync: Boolean = syncNextcloudEnabled
    ) {
        val updatedClient = client.copy(
            nextcloudUrl = url.trim(),
            nextcloudUser = user.trim(),
            nextcloudPassword = password,
            nextcloudFolder = folder.trim(),
            autoSyncEnabled = autoSync,
            syncGoogleDriveEnabled = false,
            syncNextcloudEnabled = nextcloudSync
        )
        store.updateClient(updatedClient)
        ClientSyncScheduler.rescheduleIfEnabled(appContext, updatedClient)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsAppBackground)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${client.clientName} Settings",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp),
            color = SettingsPrimaryText,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsSecondaryAction,
                    contentColor = SettingsPrimaryText
                )
            ) {
                Text("Back")
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SettingsPanelBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sync",
                    color = SettingsPrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto sync",
                        modifier = Modifier.weight(1f),
                        color = SettingsPrimaryText
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = {
                            autoSyncEnabled = it
                            saveClient(autoSync = it)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Nextcloud sync",
                        modifier = Modifier.weight(1f),
                        color = SettingsPrimaryText
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = syncNextcloudEnabled,
                        onCheckedChange = {
                            syncNextcloudEnabled = it
                            saveClient(nextcloudSync = it)
                        }
                    )
                }
                Button(
                    onClick = {
                        isManualSyncRunning = true
                        statusMessage = "Running sync..."
                        scope.launch {
                            val result = SyncOrchestrator.sync(appContext)
                            if (result.isSuccess) {
                                statusMessage = "Manual sync completed."
                            } else {
                                statusMessage = result.exceptionOrNull()?.message
                                    ?: "Manual sync failed."
                            }
                            isManualSyncRunning = false
                        }
                    },
                    enabled = !isManualSyncRunning,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsPrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text(if (isManualSyncRunning) "Syncing..." else "Manual Sync Now")
                }
                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        color = SettingsSecondaryText
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SettingsPanelBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Services",
                    color = SettingsPrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        showNextcloudConnect = !showNextcloudConnect
                        if (showNextcloudConnect) {
                            showNextcloudManual = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsPrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Connect to Nextcloud")
                }
                if (showNextcloudConnect) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SettingsCardBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = nextcloudUrl,
                                onValueChange = { nextcloudUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Server URL") }
                            )
                            OutlinedTextField(
                                value = nextcloudFolder,
                                onValueChange = { nextcloudFolder = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Remote folder") }
                            )
                            Button(
                                onClick = {
                                    val trimmedServer = nextcloudUrl.trim()
                                    if (trimmedServer.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Enter your Nextcloud server URL first.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    isConnectingNextcloud = true
                                    statusMessage = "Opening browser for Nextcloud sign-in..."
                                    scope.launch {
                                        try {
                                            val initResult = NextcloudLoginFlowManager.startLogin(trimmedServer)
                                            val initData = initResult.getOrNull()
                                            if (initData == null) {
                                                statusMessage = "Unable to start Nextcloud sign-in."
                                                isConnectingNextcloud = false
                                                return@launch
                                            }

                                            openLoginInBrowser(context, initData.loginUrl)

                                            val pollResult = NextcloudLoginFlowManager.pollForResult(initData)
                                            val loginData = pollResult.getOrNull()
                                            if (loginData == null) {
                                                statusMessage = "Nextcloud sign-in failed."
                                                isConnectingNextcloud = false
                                                return@launch
                                            }

                                            nextcloudUrl = loginData.server
                                            nextcloudUser = loginData.loginName
                                            nextcloudPassword = loginData.appPassword

                                            saveClient(
                                                url = loginData.server,
                                                user = loginData.loginName,
                                                password = loginData.appPassword,
                                                folder = nextcloudFolder
                                            )

                                            statusMessage = "Nextcloud connected."
                                        } catch (e: Exception) {
                                            statusMessage = e.message ?: "Nextcloud sign-in failed."
                                        }

                                        isConnectingNextcloud = false
                                    }
                                },
                                enabled = !isConnectingNextcloud,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SettingsPrimaryAction,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(if (isConnectingNextcloud) "Waiting for login..." else "Open Browser Login")                            }
                            if (nextcloudUser.isNotBlank()) {
                                Text(
                                    text = "Signed in as: $nextcloudUser",
                                    color = SettingsSecondaryText
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        showNextcloudManual = !showNextcloudManual
                        if (showNextcloudManual) {
                            showNextcloudConnect = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsSecondaryAction,
                        contentColor = SettingsPrimaryText
                    )
                ) {
                    Text("Manual Nextcloud Setup")
                }
                if (showNextcloudManual) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SettingsCardBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = nextcloudUrl,
                                onValueChange = { nextcloudUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Server URL") }
                            )
                            OutlinedTextField(
                                value = nextcloudUser,
                                onValueChange = { nextcloudUser = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Username") }
                            )
                            OutlinedTextField(
                                value = nextcloudPassword,
                                onValueChange = { nextcloudPassword = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("App Password") }
                            )
                            OutlinedTextField(
                                value = nextcloudFolder,
                                onValueChange = { nextcloudFolder = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Remote folder") }
                            )
                            Button(
                                onClick = {
                                    saveClient()
                                    statusMessage = "Manual Nextcloud settings saved."
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SettingsPrimaryAction,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Save Manual Setup")
                            }
                        }
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SettingsPanelBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showLocalFiles = !showLocalFiles },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsSecondaryAction,
                        contentColor = SettingsPrimaryText
                    )
                ) {
                    Text("Local Files")
                }
                if (showLocalFiles) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SettingsCardBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (localFiles.isEmpty()) {
                                Text(
                                    text = "No local CSV files for this client.",
                                    color = SettingsSecondaryText
                                )
                            } else {
                                localFiles.forEach { file ->
                                    val isChecked = selectedFiles.contains(file.name)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    if (!selectedFiles.contains(file.name)) {
                                                        selectedFiles.add(file.name)
                                                    }
                                                } else {
                                                    selectedFiles.remove(file.name)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = file.name,
                                            modifier = Modifier.weight(1f),
                                            color = SettingsPrimaryText
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        val names = selectedFiles.toList()
                                        var deletedCount = 0
                                        names.forEach { fileName ->
                                            if (store.deleteLocalFileForClient(client.id, fileName)) {
                                                deletedCount += 1
                                            }
                                        }
                                        selectedFiles.clear()
                                        localFilesVersion += 1
                                        statusMessage = if (deletedCount > 0) {
                                            "Deleted $deletedCount selected local file(s)."
                                        } else {
                                            "No selected files were deleted."
                                        }
                                    },
                                    enabled = selectedFiles.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SettingsDangerAction,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Delete Selected Files")
                                }
                        }
                        Button(
                            onClick = {
                                val deletedCount = store.deleteAllLocalFilesForClient(client.id)
                                selectedFiles.clear()
                                localFilesVersion += 1
                                statusMessage = if (deletedCount > 0) {
                                    "Deleted $deletedCount local file(s)."
                                } else {
                                    "No local files were found to delete."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SettingsDangerAction,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Delete All Local Files")
                        }
                    }
                }
            }
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SettingsPanelBackground
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Danger Zone",
                color = SettingsPrimaryText,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showDeleteEntriesConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsDangerAction,
                    contentColor = Color.Black
                )
            ) {
                Text("Delete All Client Entries")
            }
        }
    }
}
if (showDeleteEntriesConfirm) {
    AlertDialog(
        onDismissRequest = { showDeleteEntriesConfirm = false },
        title = {
            Text("Delete all client entries?")
        },
        text = {
            Text("This will permanently delete all saved time entries for this client.")
        },
        confirmButton = {
            Button(
                onClick = {
                    store.deleteEntriesForClient(client.id)
                    statusMessage = "Deleted all client entries."
                    showDeleteEntriesConfirm = false
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsDangerAction,
                    contentColor = Color.Black
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(
                onClick = { showDeleteEntriesConfirm = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsSecondaryAction,
                    contentColor = SettingsPrimaryText
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
}