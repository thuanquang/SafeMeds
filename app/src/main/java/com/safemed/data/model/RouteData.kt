package com.safemed.data.model

import org.osmdroid.util.GeoPoint

/**
 * Domain model for route data
 * Provides clean interface for UI consumption
 */
data class RouteData(
    val path: List<GeoPoint>,        // Route polyline points (converted from ORS coordinates)
    val distanceMeters: Double,     // Total route distance in meters
    val durationSeconds: Double     // Total route duration in seconds
) {
    /**
     * Format distance as human-readable string
     * @return Distance formatted as "X.X km" or "X m"
     */
    fun formatDistance(): String {
        return if (distanceMeters >= 1000) {
            String.format("%.1f km", distanceMeters / 1000)
        } else {
            String.format("%.0f m", distanceMeters)
        }
    }

    /**
     * Format duration as human-readable string
     * @return Duration formatted as "X min" or "X hr X min"
     */
    fun formatDuration(): String {
        val totalMinutes = (durationSeconds / 60).toInt()
        return if (totalMinutes >= 60) {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (minutes > 0) "$hours hr $minutes min" else "$hours hr"
        } else {
            "$totalMinutes min"
        }
    }

    /**
     * Format as combined route info string
     * @return Combined string like "15 min • 3.2 km"
     */
    fun formatRouteInfo(): String {
        return "${formatDuration()} • ${formatDistance()}"
    }
}
