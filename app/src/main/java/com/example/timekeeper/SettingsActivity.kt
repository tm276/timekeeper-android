package com.example.timekeeper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import java.io.File

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
                color = Color(0xFF121212)
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
fun SettingsScreen(
    clientId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val store = remember { TimeLogStore(appContext) }

    val currentSettings = store.settings
    val currentClient = remember(store.clients, store.activeClientId, clientId) {
        getSettingsClient(store, clientId)
    }
    val currentNextcloud = currentClient?.let {
        NextcloudSettings(
            serverUrl = it.nextcloudUrl,
            username = it.nextcloudUser,
            appPassword = it.nextcloudPassword,
            remoteFolder = it.nextcloudFolder.ifBlank { "TimeKeeper" }
        )
    } ?: NextcloudSettings.default()

    var duration by remember(currentSettings.durationAmount) {
        mutableStateOf(currentSettings.durationAmount.toString())
    }
    var durationError by remember { mutableStateOf("") }
    var userName by remember(currentSettings.userName) {
        mutableStateOf(currentSettings.userName)
    }

    var serverUrl by remember(currentClient?.id, currentNextcloud.serverUrl) {
        mutableStateOf(currentNextcloud.serverUrl)
    }
    var username by remember(currentClient?.id, currentNextcloud.username) {
        mutableStateOf(currentNextcloud.username)
    }
    var appPassword by remember(currentClient?.id, currentNextcloud.appPassword) {
        mutableStateOf(currentNextcloud.appPassword)
    }
    var remoteFolder by remember(currentClient?.id, currentNextcloud.remoteFolder) {
        mutableStateOf(currentNextcloud.remoteFolder)
    }

    var nextcloudMessage by remember { mutableStateOf("") }
    var driveMessage by remember { mutableStateOf("") }
    var localFilesRefreshTrigger by remember { mutableStateOf(0) }
    var driveAccount by remember {
        mutableStateOf(findConnectedDriveAccount(context))
    }
    var showNextcloudManualSetup by remember { mutableStateOf(false) }

    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            driveAccount = account

            val client = getSettingsClient(store, clientId)
            if (client != null) {
                store.updateClient(
                    client.copy(
                        googleDriveAccount = account.email ?: ""
                    )
                )
            }

            driveMessage = "Google Drive connected${account.email?.let { " as $it" } ?: "."}"
        } catch (e: Exception) {
            driveMessage = "Google Drive sign-in failed: ${e.message ?: "Unknown error"}"
        }
    }

    val savedNextcloudSettings = NextcloudSettings(
        serverUrl = serverUrl.trim(),
        username = username.trim(),
        appPassword = appPassword,
        remoteFolder = remoteFolder.trim().ifBlank { "TimeKeeper" }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(12.dp)
                .semantics { heading() },
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("CSV Window", color = Color.White, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your Name") },
                    singleLine = true,
                    colors = textFieldColors()
                )

                OutlinedTextField(
                    value = duration,
                    onValueChange = {
                        duration = it
                        durationError = when {
                            it.isBlank() -> ""
                            it.all(Char::isDigit) -> ""
                            else -> "Enter numbers only"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Duration in days") },
                    singleLine = true,
                    isError = durationError.isNotBlank(),
                    supportingText = {
                        if (durationError.isNotBlank()) {
                            Text(durationError, color = Color(0xFFE57373))
                        }
                    },
                    colors = textFieldColors()
                )

                Button(
                    onClick = {
                        val parsed = duration.toIntOrNull()
                        if (parsed == null || parsed < 1) {
                            durationError = "Enter a whole number greater than 0"
                            return@Button
                        }

                        store.updateSettings(
                            currentSettings.copy(
                                durationAmount = parsed,
                                durationUnit = DurationUnit.DAYS,
                                userName = userName.trim()
                            )
                        )
                        nextcloudMessage = "CSV settings saved."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = primaryButtonColors()
                ) {
                    Text("Save CSV Settings")
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("History", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Browse saved local CSV timecards in the app.", color = Color(0xFFCFD8DC))
                Button(
                    onClick = {
                        context.startActivity(Intent(context, HistoryActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = primaryButtonColors()
                ) {
                    Text("View Local Timecards")
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Google Drive", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = driveAccount?.email?.let { "Connected as $it" }
                        ?: currentClient?.googleDriveAccount?.takeIf { it.isNotBlank() }?.let { "Connected as $it" }
                        ?: "Not connected",
                    color = Color(0xFFCFD8DC)
                )

                Button(
                    onClick = {
                        val signInClient = GoogleSignIn.getClient(context, googleSignInOptions())
                        driveSignInLauncher.launch(signInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = primaryButtonColors()
                ) {
                    Text(if (driveAccount == null) "Connect Google Drive" else "Reconnect Google Drive")
                }

                Button(
                    onClick = {
                        val account = driveAccount
                        val client = getSettingsClient(store, clientId)
                        if (account == null) {
                            driveMessage = "Connect Google Drive first."
                        } else if (client == null) {
                            driveMessage = "No client available to sync."
                        } else {
                            coroutineScope.launch {
                                driveMessage = "Google Drive sync started..."
                                GoogleDriveSyncManager.initialize(appContext)
                                val result = GoogleDriveSyncManager.syncCurrentWindow(
                                    context = appContext,
                                    store = store,
                                    client = client,
                                    account = account
                                )
                                driveMessage = if (result.isSuccess) {
                                    "Google Drive sync complete."
                                } else {
                                    "Google Drive sync failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = primaryButtonColors()
                ) {
                    Text("Sync Current CSV to Google Drive")
                }

                Button(
                    onClick = {
                        val signInClient = GoogleSignIn.getClient(context, googleSignInOptions())
                        signInClient.signOut().addOnCompleteListener {
                            val client = getSettingsClient(store, clientId)
                            if (client != null) {
                                store.updateClient(client.copy(googleDriveAccount = ""))
                            }
                            driveAccount = null
                            driveMessage = "Google Drive disconnected."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = secondaryButtonColors()
                ) {
                    Text("Disconnect Google Drive")
                }

                if (driveMessage.isNotBlank()) {
                    Text(driveMessage, color = Color(0xFFCFD8DC))
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Nextcloud", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = if (username.isNotBlank() && serverUrl.isNotBlank()) {
                        "Connected settings for $username"
                    } else {
                        "Manual setup or browser sign-in"
                    },
                    color = Color(0xFFCFD8DC)
                )

                Button(
                    onClick = {
                        if (serverUrl.isBlank()) {
                            nextcloudMessage = "Enter your server URL first."
                        } else {
                            coroutineScope.launch {
                                nextcloudMessage = "Opening Nextcloud sign-in..."
                                val initResult = NextcloudLoginFlowManager.startLogin(serverUrl)
                                if (initResult.isFailure) {
                                    nextcloudMessage = "Nextcloud sign-in failed: ${initResult.exceptionOrNull()?.message ?: "Unknown error"}"
                                    return@launch
                                }

                                val init = initResult.getOrThrow()
                                NextcloudLoginFlowManager.openLoginInBrowser(context, init.loginUrl)
                                nextcloudMessage = "Finish signing in in your browser. Waiting for Nextcloud..."

                                val pollResult = NextcloudLoginFlowManager.pollForResult(init)
                                if (pollResult.isFailure) {
                                    nextcloudMessage = "Nextcloud sign-in failed: ${pollResult.exceptionOrNull()?.message ?: "Unknown error"}"
                                    return@launch
                                }

                                val login = pollResult.getOrThrow()
                                serverUrl = login.server
                                username = login.loginName
                                appPassword = login.appPassword
                                remoteFolder = remoteFolder.trim().ifBlank { "TimeKeeper" }

                                val client = getSettingsClient(store, clientId)
                                if (client != null) {
                                    store.updateClient(
                                        client.copy(
                                            nextcloudUrl = login.server,
                                            nextcloudUser = login.loginName,
                                            nextcloudPassword = login.appPassword,
                                            nextcloudFolder = remoteFolder.trim().ifBlank { "TimeKeeper" }
                                        )
                                    )
                                }
                                nextcloudMessage = "Nextcloud connected and app password saved."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = primaryButtonColors()
                ) {
                    Text("Connect with Nextcloud")
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF263238)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNextcloudManualSetup = !showNextcloudManualSetup },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Manual Nextcloud Setup",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = if (showNextcloudManualSetup) "Hide" else "Show",
                                color = Color(0xFF64B5F6)
                            )
                        }

                        AnimatedVisibility(visible = showNextcloudManualSetup) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = serverUrl,
                                    onValueChange = { serverUrl = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Server URL") },
                                    placeholder = { Text("https://cloud.example.com") },
                                    singleLine = true,
                                    colors = textFieldColors()
                                )

                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Username") },
                                    singleLine = true,
                                    colors = textFieldColors()
                                )

                                OutlinedTextField(
                                    value = appPassword,
                                    onValueChange = { appPassword = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("App password") },
                                    singleLine = true,
                                    colors = textFieldColors()
                                )

                                OutlinedTextField(
                                    value = remoteFolder,
                                    onValueChange = { remoteFolder = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Remote folder") },
                                    singleLine = true,
                                    colors = textFieldColors()
                                )

                                Button(
                                    onClick = {
                                        val client = getSettingsClient(store, clientId)
                                        if (client == null) {
                                            nextcloudMessage = "No client available to save."
                                        } else {
                                            store.updateClient(
                                                client.copy(
                                                    nextcloudUrl = savedNextcloudSettings.serverUrl,
                                                    nextcloudUser = savedNextcloudSettings.username,
                                                    nextcloudPassword = savedNextcloudSettings.appPassword,
                                                    nextcloudFolder = savedNextcloudSettings.remoteFolder
                                                )
                                            )
                                            nextcloudMessage = "Nextcloud settings saved."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = primaryButtonColors()
                                ) {
                                    Text("Save Nextcloud Settings")
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val client = getSettingsClient(store, clientId)
                        if (client == null) {
                            nextcloudMessage = "No client available to sync."
                        } else {
                            store.updateClient(
                                client.copy(
                                    nextcloudUrl = savedNextcloudSettings.serverUrl,
                                    nextcloudUser = savedNextcloudSettings.username,
                                    nextcloudPassword = savedNextcloudSettings.appPassword,
                                    nextcloudFolder = savedNextcloudSettings.remoteFolder
                                )
                            )
                            coroutineScope.launch {
                                nextcloudMessage = "Nextcloud sync started..."
                                val result = NextcloudSyncManager.syncCurrentWindow(
                                    context = appContext,
                                    store = store,
                                    clientProfile = client.copy(
                                        nextcloudUrl = savedNextcloudSettings.serverUrl,
                                        nextcloudUser = savedNextcloudSettings.username,
                                        nextcloudPassword = savedNextcloudSettings.appPassword,
                                        nextcloudFolder = savedNextcloudSettings.remoteFolder
                                    ),
                                    settings = savedNextcloudSettings
                                )
                                nextcloudMessage = if (result.isSuccess) {
                                    "Nextcloud sync complete."
                                } else {
                                    "Nextcloud sync failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = primaryButtonColors()
                ) {
                    Text("Sync Current CSV to Nextcloud")
                }

                if (nextcloudMessage.isNotBlank()) {
                    Text(nextcloudMessage, color = Color(0xFFCFD8DC))
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E)
        ) {
            val localFiles = remember(currentClient?.id, localFilesRefreshTrigger) {
                currentClient?.let { store.getLocalFilesForClient(it.id) } ?: emptyList<File>()
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Local Files", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Delete individual local CSV files for this client, or remove all local files for this client.",
                    color = Color(0xFFCFD8DC)
                )

                if (currentClient == null) {
                    Text("No client selected.", color = Color(0xFFCFD8DC))
                } else if (localFiles.isEmpty()) {
                    Text("No local files found for this client.", color = Color(0xFFCFD8DC))
                } else {
                    localFiles.forEach { file ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF263238)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = file.name,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = {
                                        val client = getSettingsClient(store, clientId)
                                        if (client == null) {
                                            nextcloudMessage = "No client available to delete files."
                                        } else {
                                            val deleted = store.deleteLocalFileForClient(client.id, file.name)
                                            nextcloudMessage = if (deleted) {
                                                "Deleted local file: ${file.name}"
                                            } else {
                                                "Could not delete local file: ${file.name}"
                                            }
                                            localFilesRefreshTrigger += 1
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE57373),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val client = getSettingsClient(store, clientId)
                            if (client == null) {
                                nextcloudMessage = "No client available to delete files."
                            } else {
                                val deletedCount = store.deleteAllLocalFilesForClient(client.id)
                                nextcloudMessage = if (deletedCount > 0) {
                                    "Deleted $deletedCount local file(s) for this client."
                                } else {
                                    "No local files were deleted for this client."
                                }
                                localFilesRefreshTrigger += 1
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE57373),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Delete All Local Files")
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = secondaryButtonColors()
        ) {
            Text("Back")
        }
    }

    LaunchedEffect(Unit) {
        driveAccount = findConnectedDriveAccount(context)
    }
}

private fun getSettingsClient(store: TimeLogStore, clientId: String?): ClientProfile? {
    return store.getClientById(clientId)
        ?: store.getActiveClient()
        ?: store.clients.firstOrNull()
}

private fun findConnectedDriveAccount(context: android.content.Context): GoogleSignInAccount? {
    val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
    return if (GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) account else null
}

private fun googleSignInOptions(): GoogleSignInOptions {
    return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .build()
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF1E1E1E),
    unfocusedContainerColor = Color(0xFF1E1E1E),
    focusedBorderColor = Color(0xFF64B5F6),
    unfocusedBorderColor = Color(0xFF90A4AE),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFF64B5F6),
    unfocusedLabelColor = Color(0xFFCFD8DC),
    cursorColor = Color(0xFF64B5F6)
)

@Composable
private fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color(0xFF64B5F6),
    contentColor = Color.Black
)

@Composable
private fun secondaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color(0xFF263238),
    contentColor = Color.White
)
