package com.safemed.ui.screen

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.osmdroid.util.GeoPoint
import com.safemed.data.model.Pharmacy
import com.safemed.data.model.PharmacyDistance
import com.safemed.data.repository.PharmacyRepository
import com.safemed.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Map Screen
 */
data class MapUiState(
    val userLocation: GeoPoint? = null,
    val pharmacies: List<PharmacyDistance> = emptyList(),
    val selectedPharmacy: Pharmacy? = null,
    val routePolyline: List<GeoPoint> = emptyList(),
    val routeInfo: String = "",
    val isLoading: Boolean = false,
    val isLoadingRoute: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for MapScreen managing pharmacy data, user location, and routing
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val pharmacyRepository: PharmacyRepository,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Cache for pharmacies before distance calculation
    private var allPharmacies: List<Pharmacy> = emptyList()

    init {
        loadPharmacies()
    }

    /**
     * Load pharmacies from Firestore
     */
    fun loadPharmacies() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            pharmacyRepository.getAllPharmacies()
                .onSuccess { pharmacies ->
                    allPharmacies = pharmacies
                    
                    // If we have user location, calculate distances
                    val pharmaciesWithDistance = if (_uiState.value.userLocation != null) {
                        calculateDistances(pharmacies, _uiState.value.userLocation!!)
                    } else {
                        pharmacies.map { PharmacyDistance(it, null) }
                    }
                    
                    _uiState.update { 
                        it.copy(
                            pharmacies = pharmaciesWithDistance,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load pharmacies: ${error.message}"
                        )
                    }
                }
        }
    }

    /**
     * Update user location and recalculate distances
     * @param location Android Location object from GPS/Fused Location
     */
    fun updateUserLocation(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        
        // If location hasn't changed significantly, don't recalculate
        val currentLocation = _uiState.value.userLocation
        if (currentLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                geoPoint.latitude, geoPoint.longitude,
                results
            )
            if (results[0] < 50) return // Less than 50 meters change
        }
        
        val pharmaciesWithDistance = calculateDistances(allPharmacies, geoPoint)
        
        _uiState.update { 
            it.copy(
                userLocation = geoPoint,
                pharmacies = pharmaciesWithDistance
            )
        }
    }

    /**
     * Calculate straight-line distances and sort pharmacies by proximity
     */
    private fun calculateDistances(
        pharmacies: List<Pharmacy>,
        userLocation: GeoPoint
    ): List<PharmacyDistance> {
        return pharmacies.map { pharmacy ->
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                pharmacy.latitude, pharmacy.longitude,
                results
            )
            PharmacyDistance(pharmacy, results[0].toInt())
        }
        .sortedBy { it.distanceMeters ?: Int.MAX_VALUE }
        .take(10) // Limit to 10 nearest pharmacies
    }

    /**
     * Select a pharmacy and fetch route from ORS API
     * @param pharmacy The pharmacy to select
     */
    fun onPharmacySelected(pharmacy: Pharmacy) {
        val userLocation = _uiState.value.userLocation
        
        _uiState.update { 
            it.copy(
                selectedPharmacy = pharmacy,
                routePolyline = emptyList(),
                routeInfo = "",
                isLoadingRoute = true,
                errorMessage = null
            )
        }
        
        if (userLocation == null) {
            _uiState.update { 
                it.copy(
                    isLoadingRoute = false,
                    errorMessage = "User location not available"
                )
            }
            return
        }
        
        viewModelScope.launch {
            val pharmacyLocation = GeoPoint(pharmacy.latitude, pharmacy.longitude)
            
            routeRepository.getRoute(userLocation, pharmacyLocation)
                .onSuccess { routeData ->
                    _uiState.update { 
                        it.copy(
                            routePolyline = routeData.path,
                            routeInfo = routeData.formatRouteInfo(),
                            isLoadingRoute = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoadingRoute = false,
                            errorMessage = "Failed to get route: ${error.message}"
                        )
                    }
                }
        }
    }

    /**
     * Clear pharmacy selection and route
     */
    fun clearSelection() {
        _uiState.update { 
            it.copy(
                selectedPharmacy = null,
                routePolyline = emptyList(),
                routeInfo = "",
                errorMessage = null
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
