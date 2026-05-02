package com.example.timekeeper

/**
 * A locally stored job site associated with a client.
 *
 * Privacy notes:
 * - This model is intended for local persistence only.
 * - It does not store visit history.
 * - It does not imply background tracking.
 */
data class WorkSite(
    val id: String,
    val clientId: String,
    val siteName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double = DEFAULT_RADIUS_METERS,
) {
    companion object {
        const val DEFAULT_RADIUS_METERS: Double = 100.0
    }
}
