package com.example.timekeeper

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object NextcloudLoginFlowManager {

    data class LoginInit(
        val loginUrl: String,
        val pollEndpoint: String,
        val pollToken: String
    )

    data class LoginResult(
        val server: String,
        val loginName: String,
        val appPassword: String
    )

    suspend fun startLogin(serverUrl: String): Result<LoginInit> = withContext(Dispatchers.IO) {
        try {
            val trimmedServer = normalizeServerUrl(serverUrl)
            val endpoint = "$trimmedServer/index.php/login/v2"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
            }

            val code = connection.responseCode
            val body = readResponseBody(connection, code)
            if (code !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("Nextcloud login init failed with HTTP $code")
                )
            }

            val json = JSONObject(body)
            val poll = json.getJSONObject("poll")
            Result.success(
                LoginInit(
                    loginUrl = json.getString("login"),
                    pollEndpoint = poll.getString("endpoint"),
                    pollToken = poll.getString("token")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pollForResult(init: LoginInit, timeoutMillis: Long = 180_000L): Result<LoginResult> =
        withContext(Dispatchers.IO) {
            val startedAt = System.currentTimeMillis()

            while (System.currentTimeMillis() - startedAt < timeoutMillis) {
                try {
                    val connection = (URL(init.pollEndpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 15000
                        readTimeout = 15000
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        setRequestProperty("Accept", "application/json")
                    }

                    val body = "token=" + URLEncoder.encode(init.pollToken, "UTF-8")
                    connection.outputStream.use { it.write(body.toByteArray()) }

                    val code = connection.responseCode
                    if (code == 404) {
                        delay(2000)
                        continue
                    }

                    val responseBody = readResponseBody(connection, code)
                    if (code !in 200..299) {
                        return@withContext Result.failure(
                            IllegalStateException("Nextcloud login polling failed with HTTP $code")
                        )
                    }

                    val json = JSONObject(responseBody)
                    return@withContext Result.success(
                        LoginResult(
                            server = json.getString("server"),
                            loginName = json.getString("loginName"),
                            appPassword = json.getString("appPassword")
                        )
                    )
                } catch (_: Exception) {
                    delay(2000)
                }
            }

            Result.failure(IllegalStateException("Timed out waiting for Nextcloud sign-in."))
        }
        }

fun openLoginInBrowser(context: Context, loginUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

    private fun normalizeServerUrl(serverUrl: String): String {
        val trimmed = serverUrl.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun readResponseBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }

