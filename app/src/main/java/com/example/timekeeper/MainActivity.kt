package com.example.timekeeper

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var store: TimeLogStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = TimeLogStore(applicationContext)

        setContent {
            MaterialTheme {
                @OptIn(ExperimentalMaterial3Api::class)
                MainScreen(store = store)
            }
        }
    }
}

@Composable
fun MainScreen(store: TimeLogStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddClientDialog by rememberSaveable { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<ClientProfile?>(null) }
    var stopClient by remember { mutableStateOf<ClientProfile?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TimeKeeper", style = MaterialTheme.typography.titleLarge)

                TextButton(onClick = { showAddClientDialog = true }) {
                    Text("Add Client")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            Text(
                text = "Clients",
                style = MaterialTheme.typography.titleLarge            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(store.clients, key = { it.id }) { client ->
                    val isActive = store.isClientActive(client.id)

                    ClientCard(
                        client = client,
                        isActive = isActive,
                        onStart = {
                            store.startTimer(client.id)
                        },
                        onStop = {
                            stopClient = client
                        },
                        onEdit = {
                            editingClient = client
                        },
                        onExport = {
                            scope.launch {
                                val file = CsvWindowManager.writeCurrentWindowCsv(
                                    context = context,
                                    client = client,
                                    settings = store.settings,
                                    entries = store.getEntriesForClient(client.id)
                                )
                                Toast.makeText(
                                    context,
                                    "CSV written: ${file.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddClientDialog) {
        ClientDialog(
            title = "Add Client",
            initialClientName = "",
            initialUserName = store.settings.userName,
            initialCsvFileName = "time_log.csv",
            initialLocalFolder = "",
            initialGoogleDriveAccount = "",
            initialGoogleDriveFolder = "",
            initialNextcloudUrl = "",
            initialNextcloudUser = "",
            initialNextcloudPassword = "",
            initialNextcloudFolder = "",
            onDismiss = { showAddClientDialog = false },
            onSave = { clientName,
                       userName,
                       csvFileName,
                       localFolder,
                       googleDriveAccount,
                       googleDriveFolder,
                       nextcloudUrl,
                       nextcloudUser,
                       nextcloudPassword,
                       nextcloudFolder ->

                store.addClient(
                    clientName = clientName,
                    userName = userName,
                    csvFileName = csvFileName,
                    localFolder = localFolder,
                    googleDriveAccount = googleDriveAccount,
                    googleDriveFolder = googleDriveFolder,
                    nextcloudUrl = nextcloudUrl,
                    nextcloudUser = nextcloudUser,
                    nextcloudPassword = nextcloudPassword,
                    nextcloudFolder = nextcloudFolder
                )
                showAddClientDialog = false
            }
        )
    }

    editingClient?.let { client ->
        ClientDialog(
            title = "Edit Client",
            initialClientName = client.clientName,
            initialUserName = client.userName,
            initialCsvFileName = client.csvFileName,
            initialLocalFolder = client.localFolder,
            initialGoogleDriveAccount = client.googleDriveAccount,
            initialGoogleDriveFolder = client.googleDriveFolder,
            initialNextcloudUrl = client.nextcloudUrl,
            initialNextcloudUser = client.nextcloudUser,
            initialNextcloudPassword = client.nextcloudPassword,
            initialNextcloudFolder = client.nextcloudFolder,
            onDismiss = { editingClient = null },
            onSave = { clientName,
                       userName,
                       csvFileName,
                       localFolder,
                       googleDriveAccount,
                       googleDriveFolder,
                       nextcloudUrl,
                       nextcloudUser,
                       nextcloudPassword,
                       nextcloudFolder ->

                store.updateClient(
                    client.copy(
                        clientName = clientName,
                        userName = userName,
                        csvFileName = csvFileName,
                        localFolder = localFolder,
                        googleDriveAccount = googleDriveAccount,
                        googleDriveFolder = googleDriveFolder,
                        nextcloudUrl = nextcloudUrl,
                        nextcloudUser = nextcloudUser,
                        nextcloudPassword = nextcloudPassword,
                        nextcloudFolder = nextcloudFolder
                    )
                )
                editingClient = null
            }
        )
    }

    stopClient?.let { client ->
        StopTimerDialog(
            clientName = client.clientName,
            onDismiss = { stopClient = null },
            onSave = { description ->
                store.stopTimer(description)
                stopClient = null

                val savedClient = store.getClientById(client.id) ?: client
                val file = CsvWindowManager.writeCurrentWindowCsv(
                    context = context,
                    client = savedClient,
                    settings = store.settings,
                    entries = store.getEntriesForClient(savedClient.id)
                )

                Toast.makeText(
                    context,
                    "Timer stopped. CSV updated: ${file.name}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}

@Composable
fun ClientCard(
    client: ClientProfile,
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = client.clientName,
                style = MaterialTheme.typography.titleLarge            )

            Spacer(modifier = Modifier.height(4.dp))

            if (client.userName.isNotBlank()) {
                Text("Name: ${client.userName}")
            }

            if (client.localFolder.isNotBlank()) {
                Text("Local Folder: ${client.localFolder}")
            }

            if (client.googleDriveAccount.isNotBlank()) {
                Text("Drive: ${client.googleDriveAccount}")
            }

            if (client.nextcloudUrl.isNotBlank()) {
                Text("Nextcloud: ${client.nextcloudUrl}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isActive) "Status: Running" else "Status: Idle"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isActive) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start")
                    }
                }

                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }

                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export")
                }
            }
        }
    }
}

@Composable
fun ClientDialog(
    title: String,
    initialClientName: String,
    initialUserName: String,
    initialCsvFileName: String,
    initialLocalFolder: String,
    initialGoogleDriveAccount: String,
    initialGoogleDriveFolder: String,
    initialNextcloudUrl: String,
    initialNextcloudUser: String,
    initialNextcloudPassword: String,
    initialNextcloudFolder: String,
    onDismiss: () -> Unit,
    onSave: (
        clientName: String,
        userName: String,
        csvFileName: String,
        localFolder: String,
        googleDriveAccount: String,
        googleDriveFolder: String,
        nextcloudUrl: String,
        nextcloudUser: String,
        nextcloudPassword: String,
        nextcloudFolder: String
    ) -> Unit
) {
    var clientName by rememberSaveable { mutableStateOf(initialClientName) }
    var userName by rememberSaveable { mutableStateOf(initialUserName) }
    var csvFileName by rememberSaveable { mutableStateOf(initialCsvFileName) }
    var localFolder by rememberSaveable { mutableStateOf(initialLocalFolder) }
    var googleDriveAccount by rememberSaveable { mutableStateOf(initialGoogleDriveAccount) }
    var googleDriveFolder by rememberSaveable { mutableStateOf(initialGoogleDriveFolder) }
    var nextcloudUrl by rememberSaveable { mutableStateOf(initialNextcloudUrl) }
    var nextcloudUser by rememberSaveable { mutableStateOf(initialNextcloudUser) }
    var nextcloudPassword by rememberSaveable { mutableStateOf(initialNextcloudPassword) }
    var nextcloudFolder by rememberSaveable { mutableStateOf(initialNextcloudFolder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Your Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = csvFileName,
                        onValueChange = { csvFileName = it },
                        label = { Text("CSV File Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = localFolder,
                        onValueChange = { localFolder = it },
                        label = { Text("Local Folder") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = googleDriveAccount,
                        onValueChange = { googleDriveAccount = it },
                        label = { Text("Google Drive Account") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = googleDriveFolder,
                        onValueChange = { googleDriveFolder = it },
                        label = { Text("Google Drive Folder") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = nextcloudUrl,
                        onValueChange = { nextcloudUrl = it },
                        label = { Text("Nextcloud URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = nextcloudUser,
                        onValueChange = { nextcloudUser = it },
                        label = { Text("Nextcloud User") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = nextcloudPassword,
                        onValueChange = { nextcloudPassword = it },
                        label = { Text("Nextcloud Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = nextcloudFolder,
                        onValueChange = { nextcloudFolder = it },
                        label = { Text("Nextcloud Folder") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank()) {
                        onSave(
                            clientName.trim(),
                            userName.trim(),
                            csvFileName.trim().ifBlank { "time_log.csv" },
                            localFolder.trim(),
                            googleDriveAccount.trim(),
                            googleDriveFolder.trim(),
                            nextcloudUrl.trim(),
                            nextcloudUser.trim(),
                            nextcloudPassword,
                            nextcloudFolder.trim()
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StopTimerDialog(
    clientName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var description by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Stop Timer")
        },
        text = {
            Column {
                Text("Add a description for $clientName")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(description.trim()) }) {
                Text("Stop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}