package com.safemed.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.safemed.R
import com.safemed.data.model.Pharmacy
import com.safemed.data.model.PharmacyDistance
import com.safemed.ui.screen.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline


// Default location (Ho Chi Minh City, Vietnam)
private val DEFAULT_LOCATION = GeoPoint(10.7769, 106.7009)
private const val DEFAULT_ZOOM = 16.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Stitch Colors
    val stitchBg = colorResource(id = R.color.stitch_bg)
    val stitchDarkGreen = colorResource(id = R.color.stitch_dark_green)
    val stitchLime = colorResource(id = R.color.stitch_lime)
    val stitchTextPrimary = colorResource(id = R.color.stitch_text_primary)
    
    // Lifecycle handling for MapView
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Initialize OSM Configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }
    
    // Permission Handle
    var hasLocationPermission by remember {
        mutableStateOf(checkLocationPermission(context))
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let { viewModel.updateUserLocation(it) }
                }
            } catch (e: SecurityException) { /* Handle error */ }
        }
    }

    // MapView
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(DEFAULT_LOCATION)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    // Camera Updates
    LaunchedEffect(uiState.userLocation) {
        uiState.userLocation?.let { location ->
            mapView.controller.animateTo(location)
             mapView.controller.setZoom(16.0)
        }
    }
    
    LaunchedEffect(uiState.selectedPharmacy) {
        uiState.selectedPharmacy?.let { pharmacy ->
            val point = GeoPoint(pharmacy.latitude, pharmacy.longitude)
            mapView.controller.animateTo(point)
            mapView.controller.setZoom(18.0)
        }
    }

    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
        confirmValueChange = { true }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 220.dp,
        sheetContainerColor = stitchBg,
        sheetContent = {
            BottomSheetContent(
                uiState = uiState,
                onPharmacySelected = viewModel::onPharmacySelected,
                onClearSelection = viewModel::clearSelection,
                onNavigateClick = { openGoogleMapsNavigation(context, it) },
                onCallClick = { callPharmacy(context, it) }
            )
        },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.map_title_osm),
                        color = stitchTextPrimary,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = stitchTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = stitchBg
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.overlays.clear()
                    
                    // Load custom marker icons
                    val pharmacyMarkerDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_marker_pharmacy)
                    val userLocationDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_marker_user_location)
                    
                    // Add user location marker (blue)
                    uiState.userLocation?.let { userLoc ->
                        val userMarker = Marker(view)
                        userMarker.position = userLoc
                        userMarker.title = "Your Location"
                        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        userLocationDrawable?.let { drawable ->
                            userMarker.icon = drawable
                        }
                        view.overlays.add(userMarker)
                    }
                    
                    // Add pharmacy markers (green)
                    uiState.pharmacies.forEach { pharmacyDistance ->
                        val pharmacy = pharmacyDistance.pharmacy
                        val marker = Marker(view)
                        marker.position = GeoPoint(pharmacy.latitude, pharmacy.longitude)
                        marker.title = pharmacy.name
                        marker.snippet = pharmacyDistance.formatDistance()
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        pharmacyMarkerDrawable?.let { drawable ->
                            marker.icon = drawable
                        }
                        marker.setOnMarkerClickListener { m, _ ->
                            viewModel.onPharmacySelected(pharmacy)
                            m.showInfoWindow()
                            true
                        }
                        view.overlays.add(marker)
                    }
                    
                    if (uiState.routePolyline.isNotEmpty()) {
                        val polyline = Polyline()
                        polyline.setPoints(uiState.routePolyline)
                        polyline.outlinePaint.color = android.graphics.Color.BLUE
                        polyline.outlinePaint.strokeWidth = 12f
                        view.overlays.add(polyline)
                    }
                    view.invalidate()
                }
            )
            
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = stitchLime
                )
            }
        }
    }
}

@Composable
private fun BottomSheetContent(
    uiState: MapUiState,
    onPharmacySelected: (Pharmacy) -> Unit,
    onClearSelection: () -> Unit,
    onNavigateClick: (Pharmacy) -> Unit,
    onCallClick: (Pharmacy) -> Unit
) {
    if (uiState.selectedPharmacy != null) {
        PharmacyDetailContent(
            pharmacy = uiState.selectedPharmacy,
            routeInfo = uiState.routeInfo,
            isLoadingRoute = uiState.isLoadingRoute,
            onBackClick = onClearSelection,
            onNavigateClick = { onNavigateClick(uiState.selectedPharmacy) },
            onCallClick = { onCallClick(uiState.selectedPharmacy) }
        )
    } else {
        PharmacyListContent(
            pharmacies = uiState.pharmacies,
            onPharmacyClick = onPharmacySelected
        )
    }
}

@Composable
private fun PharmacyListContent(
    pharmacies: List<PharmacyDistance>,
    onPharmacyClick: (Pharmacy) -> Unit
) {
    val stitchTextPrimary = colorResource(id = R.color.stitch_text_primary)
    val stitchTextSecondary = colorResource(id = R.color.stitch_text_secondary)

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Handle bar styling
        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 20.dp)
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .background(stitchTextSecondary.copy(alpha = 0.4f), CircleShape)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.map_nearby_pharmacies),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = stitchTextPrimary
            )
            Text(
                text = "${pharmacies.size} results",
                style = MaterialTheme.typography.bodyMedium,
                color = stitchTextSecondary
            )
        }
        
        if (pharmacies.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocalPharmacy,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = stitchTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.map_no_pharmacy),
                        style = MaterialTheme.typography.bodyLarge,
                        color = stitchTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pharmacies) { pharmacy ->
                    PharmacyListItem(pharmacyDistance = pharmacy, onClick = { onPharmacyClick(pharmacy.pharmacy) })
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun PharmacyListItem(
    pharmacyDistance: PharmacyDistance,
    onClick: () -> Unit
) {
    val darkGreen = colorResource(id = R.color.stitch_dark_green)
    val lime = colorResource(id = R.color.stitch_lime)
    
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = darkGreen),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(lime, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalPharmacy,
                    contentDescription = null,
                    tint = darkGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pharmacyDistance.pharmacy.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, // Stitch style bold
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f), // Secondary text
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pharmacyDistance.pharmacy.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Distance Pill
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = pharmacyDistance.formatDistance(),
                    style = MaterialTheme.typography.labelSmall,
                    color = lime,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PharmacyDetailContent(
    pharmacy: Pharmacy,
    routeInfo: String,
    isLoadingRoute: Boolean,
    onBackClick: () -> Unit,
    onNavigateClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val darkGreen = colorResource(id = R.color.stitch_dark_green)
    val lime = colorResource(id = R.color.stitch_lime)
    val textColor = colorResource(id = R.color.stitch_text_primary)
    val secondaryText = colorResource(id = R.color.stitch_text_secondary)

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 20.dp)
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .background(secondaryText.copy(alpha = 0.4f), CircleShape)
        )
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.map_pharmacy_detail),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.btn_close),
                    tint = textColor
                )
            }
        }
        
        // Detail Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = darkGreen)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(lime, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalPharmacy,
                            contentDescription = null,
                            tint = darkGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = pharmacy.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Info Items
                InfoRow(icon = Icons.Default.LocationOn, text = pharmacy.address, color = secondaryText)
                
                if (pharmacy.phone.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(icon = Icons.Default.Phone, text = pharmacy.phone, color = secondaryText)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                // Open status
                val statusColor = if (pharmacy.isOpen) Color(0xFFDEFF7D) else Color(0xFFFF7D7D) // Lime vs Red
                val statusText = if (pharmacy.isOpen) stringResource(R.string.map_open_now) else stringResource(R.string.map_closed)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = statusColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = statusText, color = statusColor, fontWeight = FontWeight.Bold)
                }

                if (isLoadingRoute) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = lime)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(R.string.map_calculating_route), color = secondaryText)
                    }
                } else if (routeInfo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(icon = Icons.AutoMirrored.Filled.DirectionsWalk, text = routeInfo, color = lime)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (pharmacy.phone.isNotEmpty()) {
                Button(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f), // Glassy look
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Phone, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.map_call))
                }
            }
            
            Button(
                onClick = onNavigateClick,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = lime,
                    contentColor = darkGreen
                )
            ) {
                Icon(Icons.Default.Navigation, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.map_navigate), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun openGoogleMapsNavigation(context: Context, pharmacy: Pharmacy) {
    val uri = Uri.parse("google.navigation:q=${pharmacy.latitude},${pharmacy.longitude}")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${pharmacy.latitude},${pharmacy.longitude}")))
}

private fun callPharmacy(context: Context, pharmacy: Pharmacy) {
    if (pharmacy.phone.isNotEmpty()) {
        context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${pharmacy.phone}") })
    }
}
