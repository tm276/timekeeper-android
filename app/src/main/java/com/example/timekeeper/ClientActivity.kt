package com.example.timekeeper

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timelogger.TimeFormatUtils

private val ClientAppBackground = Color(0xFF121212)
private val ClientPanelBackground = Color(0xFF1E1E1E)
private val ClientCardBackground = Color(0xFF263238)
private val ClientPrimaryAction = Color(0xFF64B5F6)
private val ClientSecondaryAction = Color(0xFF263238)
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
    val store = remember { TimeLogStore(context.applicationContext) }
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
                Text("Client not found", color = ClientPrimaryText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClientPrimaryAction,
                        contentColor = Color.Black
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

    var showDescriptionDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var dialogMode by remember { mutableStateOf(ClientTimeDialogMode.Stop) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClientAppBackground)
            .padding(12.dp)
    ) {
        Text(
            text = client.clientName,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp),
            color = ClientPrimaryText,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java)
                            .putExtra(SettingsActivity.EXTRA_CLIENT_ID, client.id)
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClientPrimaryAction,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings")
            }

            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClientSecondaryAction,
                    contentColor = ClientPrimaryText
                )
            ) {
                Text("Back")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = ClientPanelBackground,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (running && activeStart != null) {
                    Text(
                        text = "Session in progress",
                        color = ClientPrimaryText,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Started at: ${TimeFormatUtils.formatTime(activeStart)} on ${TimeFormatUtils.formatDate(activeStart)}",
                        color = ClientSecondaryText
                    )
                } else {
                    Text(
                        text = "Ready to start",
                        color = ClientPrimaryText,
                        fontWeight = FontWeight.Bold
                    )

                    if (client.userName.isNotBlank()) {
                        Text(
                            text = "User: ${client.userName}",
                            color = ClientSecondaryText
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!running) {
                                store.startTimer(client.id)
                            }
                        },
                        enabled = !running,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClientPrimaryAction,
                            contentColor = Color.Black,
                            disabledContainerColor = ClientPrimaryAction.copy(alpha = 0.35f),
                            disabledContentColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start")
                    }

                    Button(
                        onClick = {
                            if (running) {
                                description = ""
                                dialogMode = ClientTimeDialogMode.Stop
                                showDescriptionDialog = true
                            }
                        },
                        enabled = running,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClientDestructiveAction,
                            contentColor = Color.Black,
                            disabledContainerColor = ClientDestructiveAction.copy(alpha = 0.35f),
                            disabledContentColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("Stop")
                    }
                }

                Button(
                    onClick = {
                        if (running) {
                            description = ""
                            dialogMode = ClientTimeDialogMode.NextTask
                            showDescriptionDialog = true
                        }
                    },
                    enabled = running,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClientSecondaryAction,
                        contentColor = ClientPrimaryText,
                        disabledContainerColor = ClientBorderColor.copy(alpha = 0.35f),
                        disabledContentColor = ClientPrimaryText.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Next Task")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = ClientPanelBackground,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(12.dp)
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = "No entries yet",
                    color = ClientEmptyStateText
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entries.forEach { entry ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = ClientCardBackground,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = TimeFormatUtils.formatDate(entry.startMillis),
                                    color = ClientPrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Start: ${TimeFormatUtils.formatTime(entry.startMillis)}",
                                    color = ClientSecondaryText
                                )
                                Text(
                                    text = "Stop: ${TimeFormatUtils.formatTime(entry.stopMillis)}",
                                    color = ClientSecondaryText
                                )
                                Text(
                                    text = "Duration: ${entry.durationMinutes} minutes",
                                    color = ClientSecondaryText
                                )
                                Text(
                                    text = "Description: ${entry.description}",
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
        AlertDialog(
            onDismissRequest = { showDescriptionDialog = false },
            containerColor = ClientPanelBackground,
            titleContentColor = ClientPrimaryText,
            textContentColor = ClientSecondaryText,
            title = {
                Text(
                    if (dialogMode == ClientTimeDialogMode.NextTask) {
                        "Describe the task you just finished"
                    } else {
                        "Describe this time period"
                    }
                )
            },
            text = {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    placeholder = { Text("What did you do?") },
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
                        val trimmed = description.trim()
                        if (trimmed.isBlank()) return@Button

                        when (dialogMode) {
                            ClientTimeDialogMode.Stop -> {
                                store.stopTimer(trimmed)
                            }

                            ClientTimeDialogMode.NextTask -> {
                                store.stopTimer(trimmed)
                                store.startTimer(client.id)
                            }
                        }
                        showDescriptionDialog = false
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
                    onClick = { showDescriptionDialog = false },
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
