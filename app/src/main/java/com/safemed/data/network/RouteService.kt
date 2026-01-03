package com.safemed.data.network

import com.safemed.data.model.OrsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for OpenRouteService Directions API
 * Used to fetch driving routes between user location and pharmacies
 */
interface RouteService {
    
    /**
     * Get driving directions between two points
     * @param apiKey OpenRouteService API key
     * @param start Origin coordinates in "longitude,latitude" format
     * @param end Destination coordinates in "longitude,latitude" format
     * @return OrsResponse containing route geometry and properties
     */
    @GET("v2/directions/driving-car")
    suspend fun getDirections(
        @Query("api_key") apiKey: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): OrsResponse
}
