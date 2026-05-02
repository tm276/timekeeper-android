package com.example.timekeeper

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object CsvShareUtils {

    fun shareCurrentCsv(context: Context, store: TimeLogStore, client: ClientProfile) {
        val sourceFile = CsvWindowManager.getCurrentWindowFile(
            context = context,
            client = client,
            settings = store.settings,
            nowMillis = System.currentTimeMillis()
        )
        if (!sourceFile.exists()) {
            CsvWindowManager.writeCurrentWindowCsv(
                context = context,
                client = client,
                settings = store.settings,
                entries = store.getEntriesForClient(client.id),
                nowMillis = System.currentTimeMillis()
            )
        }

        val fileToShare = CsvWindowManager.getCurrentWindowFile(
            context = context,
            client = client,
            settings = store.settings,
            nowMillis = System.currentTimeMillis()
        )
        shareFile(context, fileToShare)
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Export CSV")
        )
    }
}