package com.example.timekeeper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local-only persistence for client work sites.
 *
 * Privacy notes:
 * - Stores user-created work sites locally in SharedPreferences.
 * - Does not store current location.
 * - Does not store visits or location history.
 * - Does not perform background tracking.
 */
class WorkSiteStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSites(): List<WorkSite> {
        val json = prefs.getString(KEY_WORK_SITES, null) ?: return emptyList()
        val array = runCatching { JSONArray(json) }.getOrElse { return emptyList() }
        val result = mutableListOf<WorkSite>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val site = obj.toWorkSiteOrNull() ?: continue
            result.add(site)
        }

        return result
    }

    fun saveSites(sites: List<WorkSite>) {
        val array = JSONArray()
        sites.forEach { site -> array.put(site.toJson()) }
        prefs.edit().putString(KEY_WORK_SITES, array.toString()).apply()
    }

    fun sitesForClient(clientId: String): List<WorkSite> {
        return loadSites().filter { it.clientId == clientId }
    }

    fun addSite(
        clientId: String,
        siteName: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = WorkSite.DEFAULT_RADIUS_METERS,
    ): WorkSite {
        val site = WorkSite(
            id = UUID.randomUUID().toString(),
            clientId = clientId,
            siteName = siteName.trim().ifBlank { DEFAULT_SITE_NAME },
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters.coerceAtLeast(MIN_RADIUS_METERS),
        )
        saveSites(loadSites() + site)
        return site
    }

    fun upsertSite(site: WorkSite) {
        val normalizedSite = site.copy(
            siteName = site.siteName.trim().ifBlank { DEFAULT_SITE_NAME },
            radiusMeters = site.radiusMeters.coerceAtLeast(MIN_RADIUS_METERS),
        )
        val updated = loadSites()
            .filterNot { it.id == normalizedSite.id }
            .plus(normalizedSite)
        saveSites(updated)
    }

    fun deleteSite(siteId: String) {
        saveSites(loadSites().filterNot { it.id == siteId })
    }

    fun deleteSitesForClient(clientId: String) {
        saveSites(loadSites().filterNot { it.clientId == clientId })
    }

    private fun WorkSite.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("clientId", clientId)
            put("siteName", siteName)
            put("latitude", latitude)
            put("longitude", longitude)
            put("radiusMeters", radiusMeters)
        }
    }

    private fun JSONObject.toWorkSiteOrNull(): WorkSite? {
        val id = optString("id", "").trim()
        val clientId = optString("clientId", "").trim()
        val siteName = optString("siteName", DEFAULT_SITE_NAME).trim().ifBlank { DEFAULT_SITE_NAME }

        if (id.isBlank() || clientId.isBlank()) return null
        if (!has("latitude") || !has("longitude")) return null

        return WorkSite(
            id = id,
            clientId = clientId,
            siteName = siteName,
            latitude = optDouble("latitude"),
            longitude = optDouble("longitude"),
            radiusMeters = optDouble("radiusMeters", WorkSite.DEFAULT_RADIUS_METERS)
                .coerceAtLeast(MIN_RADIUS_METERS),
        )
    }

    companion object {
        private const val PREFS_NAME = "timekeeper_work_sites"
        private const val KEY_WORK_SITES = "work_sites"
        private const val DEFAULT_SITE_NAME = "Work site"
        private const val MIN_RADIUS_METERS = 25.0
    }
}
