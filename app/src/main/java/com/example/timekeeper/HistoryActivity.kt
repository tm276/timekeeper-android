package com.example.timekeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

private val HistoryBackground = Color(0xFF121212)
private val HistoryPanel = Color(0xFF1E1E1E)
private val HistoryCard = Color(0xFF263238)
private val HistoryPrimary = Color(0xFF64B5F6)
private val HistoryText = Color.White
private val HistorySecondaryText = Color(0xFFCFD8DC)

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = HistoryBackground
            ) {
                HistoryScreen(onBack = { finish() }, filesDir = filesDir)
            }
        }
    }
}

data class CsvHistoryEntry(
    val date: String,
    val startTime: String,
    val stopTime: String,
    val durationMinutes: String,
    val description: String
)

@Composable
private fun HistoryScreen(onBack: () -> Unit, filesDir: File) {
    val csvFiles = remember {
        filesDir.walkTopDown()
            .filter { it.isFile && it.name.startsWith("timelog_") && it.name.endsWith(".csv") }
            .sortedByDescending { it.name }
            .toList()
    }

    var selectedFile by remember { mutableStateOf(csvFiles.firstOrNull()) }
    val selectedEntries = remember(selectedFile?.absolutePath) {
        selectedFile?.let { parseCsvHistory(it) }.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HistoryBackground)
            .padding(12.dp)
    ) {
        Text(
            text = "Timecard History",
            color = HistoryText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .semantics { heading() }
        )

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Go back to the previous screen"
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = HistoryPrimary,
                contentColor = Color.Black
            )
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(12.dp))

        HistoryPanelCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Available timecards",
                    color = HistoryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )

                if (csvFiles.isEmpty()) {
                    Text("No saved CSV files found yet.", color = HistorySecondaryText)
                } else {
                    csvFiles.forEach { file ->
                        val isSelected = selectedFile?.absolutePath == file.absolutePath
                        TimecardFileRow(
                            fileName = file.name,
                            isSelected = isSelected,
                            onClick = { selectedFile = file }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HistoryPanelCard(
            modifier = Modifier.weight(1f)
        ) {
            when {
                selectedFile == null -> {
                    Text("Select a timecard to view it.", color = HistorySecondaryText)
                }

                selectedEntries.isEmpty() -> {
                    Text("No entries found in ${selectedFile?.name}.", color = HistorySecondaryText)
                }

                else -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = selectedFile?.name.orEmpty(),
                            color = HistoryText,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() }
                        )

                        selectedEntries.forEach { entry ->
                            HistoryEntryCard(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPanelCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HistoryPanel, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun TimecardFileRow(
    fileName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val visibleLabel = if (isSelected) "Selected: $fileName" else fileName
    val talkBackLabel = if (isSelected) {
        "Selected timecard $fileName"
    } else {
        "Open timecard $fileName"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(
                color = if (isSelected) HistoryPrimary.copy(alpha = 0.25f) else HistoryCard,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = talkBackLabel
            }
            .padding(16.dp)
    ) {
        Text(
            text = visibleLabel,
            color = HistoryText,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun HistoryEntryCard(entry: CsvHistoryEntry) {
    val description = entry.description.ifBlank { "No description" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .background(HistoryCard, RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "Time entry. Date ${entry.date}. Start ${entry.startTime}. " +
                            "Stop ${entry.stopTime}. Duration ${entry.durationMinutes} minutes. " +
                            "Description: $description."
            }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.date, color = HistoryText, fontWeight = FontWeight.Bold)
            Text("Start: ${entry.startTime}", color = HistorySecondaryText)
            Text("Stop: ${entry.stopTime}", color = HistorySecondaryText)
            Text("Duration: ${entry.durationMinutes} minutes", color = HistorySecondaryText)
            Text("Description: $description", color = HistorySecondaryText)
        }
    }
}

private fun parseCsvHistory(file: File): List<CsvHistoryEntry> {
    val lines = file.readLines().drop(1)
    return lines.mapNotNull { line ->
        val parts = splitCsvLine(line)
        if (parts.size < 5) return@mapNotNull null
        CsvHistoryEntry(
            date = parts[0],
            startTime = parts[1],
            stopTime = parts[2],
            durationMinutes = parts[3],
            description = parts.subList(4, parts.size).joinToString(",")
        )
    }
}

private fun splitCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false

    line.forEach { ch ->
        when {
            ch == '"' -> {
                inQuotes = !inQuotes
            }
            ch == ',' && !inQuotes -> {
                result.add(current.toString())
                current.clear()
            }
            else -> current.append(ch)
        }
    }
    result.add(current.toString())

    return result.map { it.replace("\"\"", "\"").trim() }
}
