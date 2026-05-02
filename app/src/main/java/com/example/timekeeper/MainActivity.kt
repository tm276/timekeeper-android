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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val MainAppBackground = Color(0xFF121212)
private val MainPanelBackground = Color(0xFF1E1E1E)
private val MainCardBackground = Color(0xFF263238)
private val MainPrimaryAction = Color(0xFF64B5F6)
private val MainSecondaryAction = Color(0xFF37474F)
private val MainDestructiveAction = Color(0xFFE57373)
private val MainPrimaryText = Color(0xFFF5F5F5)
private val MainSecondaryText = Color(0xFFCFD8DC)
private val MainBorderColor = Color(0xFF90A4AE)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MainAppBackground
            ) {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val store = remember { TimeLogStore(context.applicationContext) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<ClientProfile?>(null) }
    var deletingClient by remember { mutableStateOf<ClientProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainAppBackground)
            .padding(
                top = 48.dp,
                start = 12.dp,
                end = 12.dp,
                bottom = 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "TimeKeeper",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp)
                .semantics { heading() },
            color = MainPrimaryText,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MainPanelBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Clients",
                    modifier = Modifier.semantics { heading() },
                    color = MainPrimaryText,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Make a new client"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MainPrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Make Client")
                }
            }
        }

        if (store.clients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MainPanelBackground, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No clients yet",
                    color = MainSecondaryText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(store.clients, key = { it.id }) { client ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = if (client.userName.isNotBlank()) {
                                    "Open client ${client.clientName}, user ${client.userName}"
                                } else {
                                    "Open client ${client.clientName}"
                                }
                            }
                            .clickable {
                                context.startActivity(
                                    Intent(context, ClientActivity::class.java)
                                        .putExtra(ClientActivity.EXTRA_CLIENT_ID, client.id)
                                )
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = MainCardBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = client.clientName,
                                color = MainPrimaryText,
                                fontWeight = FontWeight.Bold
                            )

                            if (client.userName.isNotBlank()) {
                                Text(
                                    text = "User: ${client.userName}",
                                    color = MainSecondaryText
                                )
                            }

                            Text(
                                text = "Tap card to open client",
                                color = MainSecondaryText
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { editingClient = client },
                                    modifier = Modifier
                                        .heightIn(min = 56.dp)
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = "Edit client name for ${client.clientName}"
                                        },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MainSecondaryAction,
                                        contentColor = MainPrimaryText
                                    )
                                ) {
                                    Text("Edit Client Name")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { deletingClient = client },
                                    modifier = Modifier
                                        .heightIn(min = 56.dp)
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = "Delete client ${client.clientName}"
                                        },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MainDestructiveAction,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Delete Client")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ClientNameDialog(
            title = "Make Client",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotBlank()) {
                    store.addClient(
                        clientName = trimmed,
                        userName = store.settings.userName
                    )
                    showCreateDialog = false
                }
            }
        )
    }

    editingClient?.let { client ->
        ClientNameDialog(
            title = "Edit Client Name",
            initialValue = client.clientName,
            confirmLabel = "Save",
            onDismiss = { editingClient = null },
            onConfirm = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotBlank()) {
                    store.updateClient(client.copy(clientName = trimmed))
                    editingClient = null
                }
            }
        )
    }

    deletingClient?.let { client ->
        AlertDialog(
            onDismissRequest = { deletingClient = null },
            containerColor = MainPanelBackground,
            titleContentColor = MainPrimaryText,
            textContentColor = MainSecondaryText,
            title = { Text("Delete Client") },
            text = { Text("Delete client ${client.clientName}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        store.deleteClient(client.id)
                        deletingClient = null
                    },
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Confirm delete client ${client.clientName}"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MainDestructiveAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { deletingClient = null },
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Cancel deleting client ${client.clientName}"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MainSecondaryAction,
                        contentColor = MainPrimaryText
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ClientNameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MainPanelBackground,
        titleContentColor = MainPrimaryText,
        textContentColor = MainSecondaryText,
        title = { Text(title, modifier = Modifier.semantics { heading() }) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .semantics {
                        contentDescription = "Client name"
                    },
                label = { Text("Client Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MainPanelBackground,
                    unfocusedContainerColor = MainPanelBackground,
                    focusedBorderColor = MainPrimaryAction,
                    unfocusedBorderColor = MainBorderColor,
                    focusedTextColor = MainPrimaryText,
                    unfocusedTextColor = MainPrimaryText,
                    focusedLabelColor = MainPrimaryAction,
                    unfocusedLabelColor = MainSecondaryText,
                    cursorColor = MainPrimaryAction
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value) },
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "$confirmLabel client name"
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MainPrimaryAction,
                    contentColor = Color.Black
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Cancel client name dialog"
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MainSecondaryAction,
                    contentColor = MainPrimaryText
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
