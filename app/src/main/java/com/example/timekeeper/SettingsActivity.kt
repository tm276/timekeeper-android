package com.example.timekeeper

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private val SettingsAppBackground = Color(0xFF121212)
private val SettingsPanelBackground = Color(0xFF1E1E1E)
private val SettingsCardBackground = Color(0xFF263238)
private val SettingsPrimaryAction = Color(0xFF64B5F6)
private val SettingsSecondaryAction = Color(0xFF37474F)
private val SettingsDangerAction = Color(0xFFE57373)
private val SettingsPrimaryText = Color(0xFFF5F5F5)
private val SettingsSecondaryText = Color(0xFFCFD8DC)
private val SettingsBorderColor = Color(0xFF90A4AE)

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
    var showWeekEndingOptions by remember { mutableStateOf(false) }

    var userName by remember(client.id, store.settings.userName) {
        mutableStateOf(store.settings.userName)
    }
    var selectedWeekEndDay by remember(client.id, store.settings.weekEndDay) {
        mutableStateOf(store.settings.weekEndDay)
    }
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

    val workSiteStore = remember { WorkSiteStore(appContext) }
    var workSitesVersion by remember { mutableStateOf(0) }
    val clientWorkSites = remember(client.id, workSitesVersion) {
        workSiteStore.sitesForClient(client.id)
    }
    var workSiteName by remember(client.id) { mutableStateOf("") }
    var workSiteRadiusMeters by remember(client.id) {
        mutableStateOf(WorkSite.DEFAULT_RADIUS_METERS.toInt().toString())
    }
    var isAddingWorkSite by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        statusMessage = if (granted) {
            "Location permission granted. Tap Add Current Location again to save this work site."
        } else {
            "Location permission is needed to save your current location as a work site."
        }
    }

    val previewSettings = store.settings.copy(
        anchorMillis = anchorMillisForWeekEnd(selectedWeekEndDay),
        durationAmount = 1,
        durationUnit = DurationUnit.WEEKS,
        weekEndDay = selectedWeekEndDay
    )
    val window = CsvWindowManager.calculateWindow(previewSettings, System.currentTimeMillis())
    val safeClientName = client.clientName
        .trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "client" }
    val previewFileName = "timelog_${safeClientName}_${window.startDate}_to_${window.endDate}.csv"

    fun saveClient(
        url: String = nextcloudUrl,
        user: String = nextcloudUser,
        password: String = nextcloudPassword,
        folder: String = nextcloudFolder,
        autoSync: Boolean = autoSyncEnabled,
        nextcloudSync: Boolean = syncNextcloudEnabled,
        newGlobalUserName: String = userName,
        weekEndDay: WeekEndDay = selectedWeekEndDay
    ) {
        val trimmedGlobalUserName = newGlobalUserName.trim()
        val updatedSettings = store.settings.copy(
            anchorMillis = anchorMillisForWeekEnd(weekEndDay),
            durationAmount = 1,
            durationUnit = DurationUnit.WEEKS,
            userName = trimmedGlobalUserName,
            weekEndDay = weekEndDay
        )

        store.updateSettings(updatedSettings)

        val updatedClient = client.copy(
            userName = trimmedGlobalUserName,
            nextcloudUrl = url.trim(),
            nextcloudUser = user.trim(),
            nextcloudPassword = password,
            nextcloudFolder = folder.trim(),
            autoSyncEnabled = autoSync,
            syncGoogleDriveEnabled = false,
            syncNextcloudEnabled = nextcloudSync
        )
        store.updateClient(updatedClient)
        CsvWindowManager.rewriteAllWindows(
            context = appContext,
            client = updatedClient,
            settings = updatedSettings,
            entries = store.getEntriesForClient(updatedClient.id)
        )
        ClientSyncScheduler.rescheduleIfEnabled(appContext, updatedClient)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsAppBackground)
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${client.clientName} Settings",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, bottom = 4.dp),
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
                    text = "Identity",
                    color = SettingsPrimaryText,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SettingsPanelBackground,
                        unfocusedContainerColor = SettingsPanelBackground,
                        focusedBorderColor = SettingsPrimaryAction,
                        unfocusedBorderColor = SettingsBorderColor,
                        focusedTextColor = SettingsPrimaryText,
                        unfocusedTextColor = SettingsPrimaryText,
                        focusedLabelColor = SettingsPrimaryAction,
                        unfocusedLabelColor = SettingsSecondaryText,
                        cursorColor = SettingsPrimaryAction
                    )
                )

                Text(
                    text = "This name is written into CSV rows and copied to this client.",
                    color = SettingsSecondaryText
                )

                Button(
                    onClick = {
                        saveClient(newGlobalUserName = userName)
                        statusMessage = "Name saved."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsPrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save Name")
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

                Text(
                    text = "Nextcloud folder: ${nextcloudFolder.ifBlank { "Not set" }}",
                    color = SettingsSecondaryText
                )

                Button(
                    onClick = {
                        isManualSyncRunning = true
                        statusMessage = "Running sync..."
                        scope.launch {
                            val result = SyncOrchestrator.sync(appContext)
                            statusMessage = if (result.isSuccess) {
                                "Manual sync completed."
                            } else {
                                result.exceptionOrNull()?.message ?: "Manual sync failed."
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
                    Text(text = statusMessage, color = SettingsSecondaryText)
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
                    text = "CSV Window",
                    color = SettingsPrimaryText,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Create a new CSV file every week.",
                    color = SettingsSecondaryText
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWeekEndingOptions = !showWeekEndingOptions },
                    shape = RoundedCornerShape(16.dp),
                    color = SettingsCardBackground
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Week Ends On", color = SettingsSecondaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedWeekEndDay.displayName(),
                            color = SettingsPrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (showWeekEndingOptions) "Tap a day below to choose." else "Tap to choose a different day.",
                            color = SettingsSecondaryText
                        )
                    }
                }

                if (showWeekEndingOptions) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SettingsCardBackground
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            WeekEndDay.values().forEach { day ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedWeekEndDay = day }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = day == selectedWeekEndDay,
                                        onClick = { selectedWeekEndDay = day }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = day.displayName(),
                                        color = SettingsPrimaryText,
                                        fontWeight = if (day == selectedWeekEndDay) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SettingsCardBackground
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Current Weekly Range", color = SettingsSecondaryText)
                        Text(
                            text = "${window.startDate} -> ${window.endDate}",
                            color = SettingsPrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Generated CSV File", color = SettingsSecondaryText)
                        Text(
                            text = previewFileName,
                            color = SettingsPrimaryText
                        )
                    }
                }

                Text(
                    text = "Changing this rewrites this client's local CSV windows to match the new weekly boundary.",
                    color = SettingsSecondaryText
                )

                Button(
                    onClick = {
                        saveClient(weekEndDay = selectedWeekEndDay)
                        statusMessage = "Weekly CSV window saved."
                        localFilesVersion += 1
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsPrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save Weekly Window")
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
                    text = "Work Sites",
                    color = SettingsPrimaryText,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Save job-site locations for this client. Locations stay on this device and are only checked when opening the home page or starting time.",
                    color = SettingsSecondaryText
                )

                if (clientWorkSites.isEmpty()) {
                    Text(
                        text = "No work sites saved for this client yet.",
                        color = SettingsSecondaryText
                    )
                } else {
                    clientWorkSites.forEach { site ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = SettingsCardBackground
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = site.siteName,
                                    color = SettingsPrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Radius: ${site.radiusMeters.toInt()} meters",
                                    color = SettingsSecondaryText
                                )
                                Text(
                                    text = "Latitude ${site.latitude}, longitude ${site.longitude}",
                                    color = SettingsSecondaryText
                                )
                                Button(
                                    onClick = {
                                        workSiteStore.deleteSite(site.id)
                                        workSitesVersion += 1
                                        statusMessage = "Deleted work site: ${site.siteName}."
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SettingsDangerAction,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Delete Work Site")
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = workSiteName,
                    onValueChange = { workSiteName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Work site name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SettingsPanelBackground,
                        unfocusedContainerColor = SettingsPanelBackground,
                        focusedBorderColor = SettingsPrimaryAction,
                        unfocusedBorderColor = SettingsBorderColor,
                        focusedTextColor = SettingsPrimaryText,
                        unfocusedTextColor = SettingsPrimaryText,
                        focusedLabelColor = SettingsPrimaryAction,
                        unfocusedLabelColor = SettingsSecondaryText,
                        cursorColor = SettingsPrimaryAction
                    )
                )

                OutlinedTextField(
                    value = workSiteRadiusMeters,
                    onValueChange = { workSiteRadiusMeters = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Radius meters") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SettingsPanelBackground,
                        unfocusedContainerColor = SettingsPanelBackground,
                        focusedBorderColor = SettingsPrimaryAction,
                        unfocusedBorderColor = SettingsBorderColor,
                        focusedTextColor = SettingsPrimaryText,
                        unfocusedTextColor = SettingsPrimaryText,
                        focusedLabelColor = SettingsPrimaryAction,
                        unfocusedLabelColor = SettingsSecondaryText,
                        cursorColor = SettingsPrimaryAction
                    )
                )

                Button(
                    onClick = {
                        val hasLocationPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            return@Button
                        }

                        val radius = workSiteRadiusMeters.toDoubleOrNull()
                            ?: WorkSite.DEFAULT_RADIUS_METERS

                        isAddingWorkSite = true
                        statusMessage = "Getting current location..."

                        scope.launch {
                            val location = CurrentLocationProvider.getCurrentLocation(appContext)
                            if (location == null) {
                                statusMessage = "Could not get current location. Check location services and permission."
                                isAddingWorkSite = false
                                return@launch
                            }

                            val savedSite = workSiteStore.addSite(
                                clientId = client.id,
                                siteName = workSiteName,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                radiusMeters = radius
                            )

                            workSiteName = ""
                            workSiteRadiusMeters = WorkSite.DEFAULT_RADIUS_METERS.toInt().toString()
                            workSitesVersion += 1
                            statusMessage = "Saved work site: ${savedSite.siteName}."
                            isAddingWorkSite = false
                        }
                    },
                    enabled = !isAddingWorkSite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsPrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text(if (isAddingWorkSite) "Saving Work Site..." else "Add Current Location as Work Site")
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
                        if (showNextcloudConnect) showNextcloudManual = false
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
                                label = { Text("Server URL") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SettingsPanelBackground,
                                    unfocusedContainerColor = SettingsPanelBackground,
                                    focusedBorderColor = SettingsPrimaryAction,
                                    unfocusedBorderColor = SettingsBorderColor,
                                    focusedTextColor = SettingsPrimaryText,
                                    unfocusedTextColor = SettingsPrimaryText,
                                    focusedLabelColor = SettingsPrimaryAction,
                                    unfocusedLabelColor = SettingsSecondaryText,
                                    cursorColor = SettingsPrimaryAction
                                )
                            )

                            OutlinedTextField(
                                value = nextcloudFolder,
                                onValueChange = { nextcloudFolder = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Remote folder") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SettingsPanelBackground,
                                    unfocusedContainerColor = SettingsPanelBackground,
                                    focusedBorderColor = SettingsPrimaryAction,
                                    unfocusedBorderColor = SettingsBorderColor,
                                    focusedTextColor = SettingsPrimaryText,
                                    unfocusedTextColor = SettingsPrimaryText,
                                    focusedLabelColor = SettingsPrimaryAction,
                                    unfocusedLabelColor = SettingsSecondaryText,
                                    cursorColor = SettingsPrimaryAction
                                )
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
                                Text(if (isConnectingNextcloud) "Waiting for login..." else "Open Browser Login")
                            }

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
                        if (showNextcloudManual) showNextcloudConnect = false
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
                                label = { Text("Server URL") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SettingsPanelBackground,
                                    unfocusedContainerColor = SettingsPanelBackground,
                                    focusedBorderColor = SettingsPrimaryAction,
                                    unfocusedBorderColor = SettingsBorderColor,
                                    focusedTextColor = SettingsPrimaryText,
                                    unfocusedTextColor = SettingsPrimaryText,
                                    focusedLabelColor = SettingsPrimaryAction,
                                    unfocusedLabelColor = SettingsSecondaryText,
                                    cursorColor = SettingsPrimaryAction
                                )
                            )

                            OutlinedTextField(
                                value = nextcloudUser,
                                onValueChange = { nextcloudUser = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Username") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SettingsPanelBackground,
                                    unfocusedContainerColor = SettingsPanelBackground,
                                    focusedBorderColor = SettingsPrimaryAction,
                                    unfocusedBorderColor = SettingsBorderColor,
                                    focusedTextColor = SettingsPrimaryText,
                                    unfocusedTextColor = SettingsPrimaryText,
                                    focusedLabelColor = SettingsPrimaryAction,
                                    unfocusedLabelColor = SettingsSecondaryText,
                                    cursorColor = SettingsPrimaryAction
                                )
                            )

                            OutlinedTextField(
                                value = nextcloudPassword,
                                onValueChange = { nextcloudPassword = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("App Password") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SettingsPanelBackground,
                                    unfocusedContainerColor = SettingsPanelBackground,
                                    focusedBorderColor = SettingsPrimaryAction,
                                    unfocusedBorderColor = SettingsBorderColor,
                                    focusedTextColor = SettingsPrimaryText,
                                    unfocusedTextColor = SettingsPrimaryText,
                                    focusedLabelColor = SettingsPrimaryAction,
                                    unfocusedLabelColor = SettingsSecondaryText,
                                    cursorColor = SettingsPrimaryAction
                                )
                            )

                            OutlinedTextField(
                                value = nextcloudFolder,
                                onValueChange = { nextcloudFolder = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Remote folder") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SettingsPanelBackground,
                                    unfocusedContainerColor = SettingsPanelBackground,
                                    focusedBorderColor = SettingsPrimaryAction,
                                    unfocusedBorderColor = SettingsBorderColor,
                                    focusedTextColor = SettingsPrimaryText,
                                    unfocusedTextColor = SettingsPrimaryText,
                                    focusedLabelColor = SettingsPrimaryAction,
                                    unfocusedLabelColor = SettingsSecondaryText,
                                    cursorColor = SettingsPrimaryAction
                                )
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
                                                    if (!selectedFiles.contains(file.name)) selectedFiles.add(file.name)
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
                                            if (store.deleteLocalFileForClient(client.id, fileName)) deletedCount += 1
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
                    text = "Repair",
                    color = SettingsPrimaryText,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Regenerate local CSV files from saved time entries.",
                    color = SettingsSecondaryText
                )

                Button(
                    onClick = {
                        CsvWindowManager.rewriteAllWindows(
                            context = appContext,
                            client = client,
                            settings = store.settings,
                            entries = store.getEntriesForClient(client.id)
                        )
                        localFilesVersion += 1
                        statusMessage = "CSV files regenerated."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsPrimaryAction,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Regenerate CSV Files")
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
            containerColor = SettingsPanelBackground,
            titleContentColor = SettingsPrimaryText,
            textContentColor = SettingsSecondaryText,
            title = { Text("Delete all client entries?") },
            text = { Text("This will permanently delete all saved time entries for this client.") },
            confirmButton = {
                Button(
                    onClick = {
                        store.deleteEntriesForClient(client.id)
                        CsvWindowManager.rewriteAllWindows(
                            context = appContext,
                            client = client,
                            settings = store.settings,
                            entries = emptyList()
                        )
                        statusMessage = "Deleted all client entries."
                        showDeleteEntriesConfirm = false
                        localFilesVersion += 1
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

private fun WeekEndDay.displayName(): String {
    return when (this) {
        WeekEndDay.SUNDAY -> "Sunday"
        WeekEndDay.MONDAY -> "Monday"
        WeekEndDay.TUESDAY -> "Tuesday"
        WeekEndDay.WEDNESDAY -> "Wednesday"
        WeekEndDay.THURSDAY -> "Thursday"
        WeekEndDay.FRIDAY -> "Friday"
        WeekEndDay.SATURDAY -> "Saturday"
    }
}

private fun anchorMillisForWeekEnd(weekEndDay: WeekEndDay): Long {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val weekEndValue = weekEndDay.isoDayValue()
    val weekStartValue = if (weekEndValue == 7) 1 else weekEndValue + 1

    var anchorDate = today
    while (anchorDate.dayOfWeek.value != weekStartValue) {
        anchorDate = anchorDate.minusDays(1)
    }

    return anchorDate.atStartOfDay(zone).toInstant().toEpochMilli()
}

private fun WeekEndDay.isoDayValue(): Int {
    return when (this) {
        WeekEndDay.MONDAY -> 1
        WeekEndDay.TUESDAY -> 2
        WeekEndDay.WEDNESDAY -> 3
        WeekEndDay.THURSDAY -> 4
        WeekEndDay.FRIDAY -> 5
        WeekEndDay.SATURDAY -> 6
        WeekEndDay.SUNDAY -> 7
    }
}
