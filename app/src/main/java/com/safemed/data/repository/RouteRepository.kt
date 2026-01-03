package com.safemed.data.repository

import org.osmdroid.util.GeoPoint
import com.safemed.data.model.RouteData
import com.safemed.data.network.RouteService
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Repository for fetching route data from OpenRouteService API
 * Handles coordinate conversion from ORS format to Google Maps format
 */
@Singleton
class RouteRepository @Inject constructor(
    private val routeService: RouteService,
    @Named("OrsApiKey") private val apiKey: String
) {
    /**
     * Fetch driving route between two points
     * @param start Origin LatLng (user location)
     * @param end Destination LatLng (pharmacy location)
     * @return Result containing RouteData or error
     */
    suspend fun getRoute(start: GeoPoint, end: GeoPoint): Result<RouteData> {
        return try {
            // ORS expects coordinates in "longitude,latitude" format
            val startStr = "${start.longitude},${start.latitude}"
            val endStr = "${end.longitude},${end.latitude}"
            
            val response = routeService.getDirections(
                apiKey = apiKey,
                start = startStr,
                end = endStr
            )
            
            // Check if response contains valid data
            val feature = response.features.firstOrNull()
                ?: return Result.failure(Exception("No route found"))
            
            // Convert ORS coordinates [lon, lat] to LatLng(lat, lng)
            val path = feature.geometry.coordinates.map { coord ->
                GeoPoint(coord[1], coord[0])  // ORS: [lon, lat] -> GeoPoint(lat, lon)
            }
            
            // Extract distance and duration from summary
            val summary = feature.properties.summary
            
            val routeData = RouteData(
                path = path,
                distanceMeters = summary.distance,
                durationSeconds = summary.duration
            )
            
            Result.success(routeData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
