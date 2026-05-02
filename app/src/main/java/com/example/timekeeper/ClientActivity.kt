package com.example.timekeeper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

private val ClientAppBackground = Color(0xFF121212)
private val ClientPanelBackground = Color(0xFF1E1E1E)
private val ClientCardBackground = Color(0xFF263238)
private val ClientPrimaryAction = Color(0xFF64B5F6)
private val ClientSecondaryAction = Color(0xFF37474F)
private val ClientDestructiveAction = Color(0xFFE57373)
private val ClientPrimaryText = Color(0xFFF5F5F5)
private val ClientSecondaryText = Color(0xFFCFD8DC)
private val ClientBorderColor = Color(0xFF90A4AE)
private val ClientEmptyStateText = Color(0xFFB0BEC5)

private enum class ClientTimeDialogMode {
    Stop,
    NextTask
}

class ClientActivity : ComponentActivity() {

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
                color = ClientAppBackground
            ) {
                ClientScreen(
                    clientId = clientId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun ClientScreen(
    clientId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var reloadKey by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                reloadKey += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val store = remember(reloadKey) { TimeLogStore(context.applicationContext) }
    val client = remember(store.clients, clientId) { store.getClientById(clientId) }

    if (client == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClientAppBackground)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Client not found",
                    color = ClientPrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Go back to clients"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClientSecondaryAction,
                        contentColor = ClientPrimaryText
                    )
                ) {
                    Text("Back")
                }
            }
        }
        return
    }

    val entries = store.getEntriesForClient(client.id).asReversed()
    val running = store.isClientActive(client.id)
    val activeStart = store.activeStartMillis
    val window = CsvWindowManager.calculateWindow(store.settings, System.currentTimeMillis())
    val safeClientName = client.clientName
        .trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "client" }
    val fileName = "timelog_${safeClientName}_${window.startDate}_to_${window.endDate}.csv"
    val weekEntries = entries.filter {
        it.stopMillis in window.startMillis until window.endExclusiveMillis
    }
    val totalMinutes = weekEntries.sumOf { it.durationMinutes }

    var showDescriptionDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var dialogMode by remember { mutableStateOf(ClientTimeDialogMode.Stop) }

    var showEditDescriptionDialog by remember { mutableStateOf(false) }
    var editDescription by remember { mutableStateOf("") }
    var editStartMillis by remember { mutableStateOf(0L) }
    var editStopMillis by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClientAppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = client.clientName,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() }
                .padding(top = 12.dp, bottom = 4.dp, start = 12.dp),
            color = ClientPrimaryText,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java)
                            .putExtra(SettingsActivity.EXTRA_CLIENT_ID, client.id)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Open settings for ${client.clientName}"
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClientSecondaryAction,
                    contentColor = ClientPrimaryText
                )
            ) {
                Text("Settings")
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Go back to clients"
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClientSecondaryAction,
                    contentColor = ClientPrimaryText
                )
            ) {
                Text("Back")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ClientPanelBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Current Week", modifier = Modifier.semantics { heading() }, color = ClientSecondaryText)
                Text(
                    text = "${window.startDate} → ${window.endDate}",
                    color = ClientPrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("CSV File", modifier = Modifier.semantics { heading() }, color = ClientSecondaryText)
                Text(
                    text = fileName,
                    color = ClientPrimaryText
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = formatLastSync(store)
                },
            shape = RoundedCornerShape(20.dp),
            color = ClientPanelBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatLastSync(store),
                    color = if (store.lastSyncFailed) ClientDestructiveAction else ClientPrimaryText,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ClientPanelBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (running) "Session in progress" else "Ready to start",
                    color = ClientPrimaryText,
                    fontWeight = FontWeight.Bold
                )

                if (running && activeStart != null) {
                    Text(
                        text = "Started at: ${TimeFormatUtils.formatTime(activeStart)} on ${TimeFormatUtils.formatDate(activeStart)}",
                        color = ClientSecondaryText
                    )
                } else {
                    Text(
                        text = "Use Start to begin tracking time for this client.",
                        color = ClientSecondaryText
                    )
                }

                if (!running) {
                    Button(
                        onClick = {
                            store.startTimer(client.id)
                            reloadKey += 1
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Start timer for ${client.clientName}"
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClientPrimaryAction,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Start")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                description = ""
                                dialogMode = ClientTimeDialogMode.Stop
                                showDescriptionDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Stop timer and describe work for ${client.clientName}"
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClientDestructiveAction,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Stop")
                        }

                        Button(
                            onClick = {
                                description = ""
                                dialogMode = ClientTimeDialogMode.NextTask
                                showDescriptionDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Save current task and start next task for ${client.clientName}"
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClientPrimaryAction,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Next Task")
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ClientPanelBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("This Week Total", modifier = Modifier.semantics { heading() }, color = ClientSecondaryText)
                Text(
                    text = "$totalMinutes min",
                    color = ClientPrimaryText,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ClientPanelBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recent Entries",
                    modifier = Modifier.semantics { heading() },
                    color = ClientPrimaryText,
                    fontWeight = FontWeight.Bold
                )

                if (entries.isEmpty()) {
                    Text(
                        text = "No time entries yet",
                        color = ClientEmptyStateText
                    )
                } else {
                    entries.take(10).forEach { entry ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Edit time entry. ${entry.description.ifBlank { "No description" }}. From ${TimeFormatUtils.formatTime(entry.startMillis)} to ${TimeFormatUtils.formatTime(entry.stopMillis)}. Duration ${entry.durationMinutes} minutes."
                                }
                                .clickable {
                                    editDescription = entry.description
                                    editStartMillis = entry.startMillis
                                    editStopMillis = entry.stopMillis
                                    showEditDescriptionDialog = true
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = ClientCardBackground
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = entry.description.ifBlank { "No description" },
                                    color = ClientPrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${TimeFormatUtils.formatTime(entry.startMillis)} - ${TimeFormatUtils.formatTime(entry.stopMillis)}",
                                    color = ClientSecondaryText
                                )
                                Text(
                                    text = "${entry.durationMinutes} min",
                                    color = ClientSecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDescriptionDialog) {
        val title = if (dialogMode == ClientTimeDialogMode.Stop) "Stop Timer" else "Next Task"
        val confirmLabel = if (dialogMode == ClientTimeDialogMode.Stop) "Stop" else "Save and Continue"

        AlertDialog(
            onDismissRequest = {
                showDescriptionDialog = false
                description = ""
            },
            containerColor = ClientPanelBackground,
            titleContentColor = ClientPrimaryText,
            textContentColor = ClientSecondaryText,
            title = {
                Text(title)
            },
            text = {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Work description" },
                    label = { Text("What did you work on") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ClientPanelBackground,
                        unfocusedContainerColor = ClientPanelBackground,
                        focusedBorderColor = ClientPrimaryAction,
                        unfocusedBorderColor = ClientBorderColor,
                        focusedTextColor = ClientPrimaryText,
                        unfocusedTextColor = ClientPrimaryText,
                        focusedLabelColor = ClientPrimaryAction,
                        unfocusedLabelColor = ClientSecondaryText,
                        cursorColor = ClientPrimaryAction
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedDescription = description.trim()
                        store.stopTimer(trimmedDescription)
                        if (dialogMode == ClientTimeDialogMode.NextTask) {
                            store.startTimer(client.id)
                        }
                        description = ""
                        showDescriptionDialog = false
                        reloadKey += 1
                    },
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = confirmLabel
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClientPrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        description = ""
                        showDescriptionDialog = false
                    },
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Cancel and keep timer running"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClientSecondaryAction,
                        contentColor = ClientPrimaryText
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDescriptionDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDescriptionDialog = false
                editDescription = ""
            },
            containerColor = ClientPanelBackground,
            titleContentColor = ClientPrimaryText,
            textContentColor = ClientSecondaryText,
            title = {
                Text("Edit Description")
            },
            text = {
                OutlinedTextField(
                    value = editDescription,
                    onValueChange = { editDescription = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Edit work description" },
                    label = { Text("What did you work on") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ClientPanelBackground,
                        unfocusedContainerColor = ClientPanelBackground,
                        focusedBorderColor = ClientPrimaryAction,
                        unfocusedBorderColor = ClientBorderColor,
                        focusedTextColor = ClientPrimaryText,
                        unfocusedTextColor = ClientPrimaryText,
                        focusedLabelColor = ClientPrimaryAction,
                        unfocusedLabelColor = ClientSecondaryText,
                        cursorColor = ClientPrimaryAction
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = store.updateEntryDescription(
                            clientId = client.id,
                            startMillis = editStartMillis,
                            stopMillis = editStopMillis,
                            description = editDescription.trim()
                        )
                        if (updated) {
                            CsvWindowManager.rewriteAllWindows(
                                context = context.applicationContext,
                                client = client,
                                settings = store.settings,
                                entries = store.getEntriesForClient(client.id)
                            )
                        }
                        editDescription = ""
                        showEditDescriptionDialog = false
                        reloadKey += 1
                    },
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Save edited description"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClientPrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        editDescription = ""
                        showEditDescriptionDialog = false
                    },
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Cancel editing description"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClientSecondaryAction,
                        contentColor = ClientPrimaryText
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatLastSync(store: TimeLogStore): String {
    if (store.lastSyncFailed) {
        return "Sync: failed"
    }

    val millis = store.lastSyncMillis ?: return "Last Sync: never"
    return "Last Sync: ${TimeFormatUtils.formatDate(millis)} at ${TimeFormatUtils.formatTime(millis)}"
}
