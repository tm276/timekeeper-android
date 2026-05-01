package com.example.timekeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF121212)
            ) {
                SettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentSettings = TimeLogStore.settings
    val currentNextcloud = LocalPersistence.loadNextcloudSettings(context)

    var duration by remember { mutableStateOf(currentSettings.durationAmount.toString()) }
    var durationError by remember { mutableStateOf("") }

    var serverUrl by remember { mutableStateOf(currentNextcloud.serverUrl) }
    var username by remember { mutableStateOf(currentNextcloud.username) }
    var appPassword by remember { mutableStateOf(currentNextcloud.appPassword) }
    var remoteFolder by remember { mutableStateOf(currentNextcloud.remoteFolder) }

    var syncMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(12.dp)
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
                Text(
                    text = "CSV Window",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
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

                        TimeLogStore.updateSettings(
                            context = context,
                            newSettings = currentSettings.copy(
                                durationAmount = parsed,
                                durationUnit = DurationUnit.DAYS
                            )
                        )
                        syncMessage = "Duration saved."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Duration")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nextcloud",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

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
                        LocalPersistence.saveNextcloudSettings(
                            context,
                            NextcloudSettings(
                                serverUrl = serverUrl.trim(),
                                username = username.trim(),
                                appPassword = appPassword,
                                remoteFolder = remoteFolder.trim()
                            )
                        )
                        syncMessage = "Nextcloud settings saved."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Nextcloud Settings")
                }

                Button(
                    onClick = {
                        val saved = NextcloudSettings(
                            serverUrl = serverUrl.trim(),
                            username = username.trim(),
                            appPassword = appPassword,
                            remoteFolder = remoteFolder.trim()
                        )
                        LocalPersistence.saveNextcloudSettings(context, saved)
                        syncMessage = "Sync started..."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sync Current CSV to Nextcloud")
                }

                if (syncMessage.isNotBlank()) {
                    Text(
                        text = syncMessage,
                        color = Color(0xFFCFD8DC)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }

    val savedSettings = NextcloudSettings(
        serverUrl = serverUrl.trim(),
        username = username.trim(),
        appPassword = appPassword,
        remoteFolder = remoteFolder.trim()
    )

    LaunchedEffect(syncMessage) {
        if (syncMessage == "Sync started...") {
            val result = NextcloudSyncManager.syncCurrentWindow(context, savedSettings)
            syncMessage = if (result.isSuccess) {
                "Nextcloud sync complete."
            } else {
                "Sync failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }
        }
    }
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