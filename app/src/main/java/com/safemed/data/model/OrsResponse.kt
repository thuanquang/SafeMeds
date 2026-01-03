package com.safemed.data.model

/**
 * Data models for parsing OpenRouteService API JSON response
 * ORS uses GeoJSON format with [longitude, latitude] coordinate order
 */

/**
 * Top-level response from ORS Directions API
 */
data class OrsResponse(
    val features: List<Feature>
)

/**
 * GeoJSON Feature containing geometry and properties
 */
data class Feature(
    val geometry: Geometry,
    val properties: Properties
)

/**
 * Geometry containing route coordinates as LineString
 * Coordinates are in [longitude, latitude] format (GeoJSON standard)
 */
data class Geometry(
    val coordinates: List<List<Double>>
)

/**
 * Properties containing route segments with distance and duration
 */
data class Properties(
    val segments: List<Segment>,
    val summary: Summary
)

/**
 * Route segment with cumulative distance and duration
 */
data class Segment(
    val distance: Double,  // meters
    val duration: Double   // seconds
)

/**
 * Route summary with total distance and duration
 */
data class Summary(
    val distance: Double,  // meters
    val duration: Double   // seconds
)
