package com.safemed.data.model

/**
 * Wrapper class combining Pharmacy with calculated straight-line distance
 * Used for sorting pharmacies by proximity to user location
 */
data class PharmacyDistance(
    val pharmacy: Pharmacy,
    val distanceMeters: Int? = null  // Straight-line distance in meters (null if location unknown)
) {
    /**
     * Format distance as human-readable string
     * @return Distance formatted as "X.X km" or "X m", or "Unknown" if null
     */
    fun formatDistance(): String {
        return when {
            distanceMeters == null -> "Unknown"
            distanceMeters >= 1000 -> String.format("%.1f km", distanceMeters / 1000.0)
            else -> "$distanceMeters m"
        }
    }
}
