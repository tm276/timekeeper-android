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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timelogger.TimeFormatUtils

private val AppBackground = Color(0xFF121212)
private val PanelBackground = Color(0xFF1E1E1E)
private val CardBackground = Color(0xFF263238)
private val PrimaryAction = Color(0xFF64B5F6)
private val SecondaryAction = Color(0xFF263238)
private val DestructiveAction = Color(0xFFE57373)
private val PrimaryText = Color(0xFFF5F5F5)
private val SecondaryText = Color(0xFFCFD8DC)
private val BorderColor = Color(0xFF90A4AE)
private val EmptyStateText = Color(0xFFB0BEC5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        TimeLogStore.initialize(this)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppBackground
            ) {
                TimeLoggerScreen()
            }
        }
    }
}

@Composable
fun TimeLoggerScreen() {
    val context = LocalContext.current
    val storeVersion = TimeLogStore.version.intValue
    val entries = remember(storeVersion) { TimeLogStore.entries.toList().asReversed() }
    val running = remember(storeVersion) { TimeLogStore.isRunning() }
    val activeStart = TimeLogStore.activeStartMillis.longValue

    var showStopDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(12.dp)
    ) {
        Text(
            text = "Time Logger",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp)
                .semantics { heading() },
            color = PrimaryText,
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
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Open settings"
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAction,
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
                onClick = {
                    CsvShareUtils.shareCurrentCsv(context)
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Export current CSV"
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAction,
                    contentColor = Color.Black
                )
            ) {
                Text("Export CSV")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = PanelBackground,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
                .semantics {
                    contentDescription = "Timer controls"
                    stateDescription = if (running) {
                        "Timer is running"
                    } else {
                        "Timer is not running"
                    }
                }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (running) "Session in progress" else "Ready to start",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )

                if (running) {
                    Text(
                        text = "Started at: ${TimeFormatUtils.formatTime(activeStart)} on ${TimeFormatUtils.formatDate(activeStart)}",
                        color = SecondaryText
                    )
                } else {
                    Text(
                        text = "Tap Start to begin tracking time.",
                        color = SecondaryText
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!running) {
                                TimeLogStore.startNow(context, System.currentTimeMillis())
                            }
                        },
                        enabled = !running,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "Start timer"
                                role = Role.Button
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAction,
                            contentColor = Color.Black
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
                                showStopDialog = true
                            }
                        },
                        enabled = running,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "Stop timer"
                                role = Role.Button
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DestructiveAction,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Stop")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = PanelBackground,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(12.dp)
                .semantics {
                    contentDescription = "Saved time entries"
                    stateDescription =
                        if (entries.isEmpty()) {
                            "No saved entries"
                        } else {
                            "${entries.size} saved entries"
                        }
                }
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = "No entries yet",
                    color = EmptyStateText
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entries.forEach { entry ->
                        val duration = TimeFormatUtils.formatDurationMinutes(
                            startMillis = entry.startMillis,
                            stopMillis = entry.stopMillis
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = CardBackground,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = buildString {
                                        append("Entry on ${TimeFormatUtils.formatDate(entry.startMillis)}. ")
                                        append("Start ${TimeFormatUtils.formatTime(entry.startMillis)}. ")
                                        append("Stop ${TimeFormatUtils.formatTime(entry.stopMillis)}. ")
                                        append("Duration ${duration} minutes. ")
                                        append("Description ${entry.description}.")
                                    }
                                }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = TimeFormatUtils.formatDate(entry.startMillis),
                                    color = PrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Start: ${TimeFormatUtils.formatTime(entry.startMillis)}",
                                    color = SecondaryText
                                )
                                Text(
                                    text = "Stop: ${TimeFormatUtils.formatTime(entry.stopMillis)}",
                                    color = SecondaryText
                                )
                                Text(
                                    text = "Duration: ${duration} minutes",
                                    color = SecondaryText
                                )
                                Text(
                                    text = "Description: ${entry.description}",
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = {
                Text("Describe this time period")
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
                        focusedContainerColor = PanelBackground,
                        unfocusedContainerColor = PanelBackground,
                        focusedBorderColor = PrimaryAction,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedLabelColor = PrimaryAction,
                        unfocusedLabelColor = SecondaryText,
                        cursorColor = PrimaryAction
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (description.isNotBlank()) {
                            TimeLogStore.stopAndSave(
                                context = context,
                                nowMillis = System.currentTimeMillis(),
                                description = description
                            )
                            showStopDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showStopDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryAction,
                        contentColor = PrimaryText
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}