package com.example.timekeeper

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure work-site matching logic.
 *
 * Privacy notes:
 * - This file does not request device location.
 * - It does not store location history.
 * - It only compares a location provided by the caller against saved local work sites.
 */
object LocationMatcher {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    data class Match(
        val workSite: WorkSite,
        val distanceMeters: Double,
    )

    /**
     * Returns the closest saved work site that contains the provided location.
     */
    fun findCurrentSite(
        latitude: Double,
        longitude: Double,
        workSites: List<WorkSite>,
    ): Match? {
        return workSites
            .map { site ->
                Match(
                    workSite = site,
                    distanceMeters = distanceMeters(
                        fromLatitude = latitude,
                        fromLongitude = longitude,
                        toLatitude = site.latitude,
                        toLongitude = site.longitude,
                    ),
                )
            }
            .filter { match -> match.distanceMeters <= match.workSite.radiusMeters }
            .minByOrNull { match -> match.distanceMeters }
    }

    /**
     * Returns all saved work sites that contain the provided location, closest first.
     */
    fun findCurrentSites(
        latitude: Double,
        longitude: Double,
        workSites: List<WorkSite>,
    ): List<Match> {
        return workSites
            .map { site ->
                Match(
                    workSite = site,
                    distanceMeters = distanceMeters(
                        fromLatitude = latitude,
                        fromLongitude = longitude,
                        toLatitude = site.latitude,
                        toLongitude = site.longitude,
                    ),
                )
            }
            .filter { match -> match.distanceMeters <= match.workSite.radiusMeters }
            .sortedBy { match -> match.distanceMeters }
    }

    /**
     * Returns true when the provided location is inside the given work site's radius.
     */
    fun isInsideSite(
        latitude: Double,
        longitude: Double,
        workSite: WorkSite,
    ): Boolean {
        return distanceMeters(
            fromLatitude = latitude,
            fromLongitude = longitude,
            toLatitude = workSite.latitude,
            toLongitude = workSite.longitude,
        ) <= workSite.radiusMeters
    }

    /**
     * Haversine distance between two latitude/longitude points.
     */
    fun distanceMeters(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Double {
        val fromLatRadians = Math.toRadians(fromLatitude)
        val toLatRadians = Math.toRadians(toLatitude)
        val deltaLatRadians = Math.toRadians(toLatitude - fromLatitude)
        val deltaLonRadians = Math.toRadians(toLongitude - fromLongitude)

        val a = sin(deltaLatRadians / 2).pow(2.0) +
                cos(fromLatRadians) * cos(toLatRadians) *
                sin(deltaLonRadians / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
