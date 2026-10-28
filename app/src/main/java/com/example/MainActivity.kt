package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NavigationStatus
import com.example.ui.dialogs.AddMapObjectDialog
import com.example.ui.dialogs.ManageMapObjectsSheet
import com.example.ui.dialogs.MapObjectDetailSheet
import com.example.ui.map.LeafletMapView
import com.example.ui.map.MapControlButtons
import com.example.ui.navigation.NavigationHud
import com.example.ui.search.SearchBarWithCategories
import com.example.ui.sheet.RoutePreviewSheet
import com.example.ui.sheet.SavedPlacesSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.trip.TripControlBar
import com.example.ui.trip.TripSummaryDialog
import com.example.ui.viewmodel.NavigationViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: NavigationViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        LeafletNavigationApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun LeafletNavigationApp(viewModel: NavigationViewModel) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle(initialValue = emptyList())
  val mapObjects by viewModel.mapObjects.collectAsStateWithLifecycle(initialValue = emptyList())
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var showSavedPlacesSheet by remember { mutableStateOf(false) }
  var showManageObjectsSheet by remember { mutableStateOf(false) }

  // Location permission request launcher
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
    if (fineGranted || coarseGranted) {
      viewModel.startTrackingLocation()
    } else {
      scope.launch {
        snackbarHostState.showSnackbar("Location permission denied. Map running in preview mode.")
      }
    }
  }

  LaunchedEffect(Unit) {
    permissionLauncher.launch(
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
    )
  }

  // Handle Android Back Navigation
  BackHandler(
    enabled = uiState.navStatus != NavigationStatus.IDLE ||
        uiState.searchResults.isNotEmpty() ||
        uiState.selectedMapObject != null ||
        uiState.isAddObjectDialogOpen ||
        uiState.tripState.isActive
  ) {
    when {
      uiState.selectedMapObject != null -> {
        viewModel.clearSelectedMapObject()
      }
      uiState.isAddObjectDialogOpen -> {
        viewModel.closeAddObjectDialog()
      }
      uiState.tripState.isActive -> {
        viewModel.stopTrip()
      }
      uiState.navStatus == NavigationStatus.NAVIGATING -> {
        viewModel.stopNavigation()
      }
      uiState.navStatus == NavigationStatus.ROUTE_PREVIEW -> {
        viewModel.stopNavigation()
      }
      uiState.searchResults.isNotEmpty() -> {
        viewModel.onSearchQueryChanged("")
      }
    }
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("main_scaffold"),
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Default Leaflet Map View
      LeafletMapView(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize()
      )

      // Idle Mode: Top Search Bar & Categories
      if (uiState.navStatus == NavigationStatus.IDLE) {
        SearchBarWithCategories(
          query = uiState.searchQuery,
          searchResults = uiState.searchResults,
          isSearching = uiState.isSearching,
          onQueryChange = { viewModel.onSearchQueryChanged(it) },
          onResultSelected = { viewModel.selectSearchResult(it) },
          onCategorySelected = { viewModel.searchCategory(it) },
          onOpenSavedPlaces = { showSavedPlacesSheet = true },
          modifier = Modifier.align(Alignment.TopCenter)
        )
      }

      // Map Controls (Zoom In/Out, Recenter, Add Object, Objects List)
      if (uiState.navStatus != NavigationStatus.NAVIGATING) {
        MapControlButtons(
          onZoomIn = { viewModel.zoomIn() },
          onZoomOut = { viewModel.zoomOut() },
          onRecenter = { viewModel.recenterMap() },
          onAddObject = { viewModel.openAddObjectDialog() },
          onOpenObjectsList = { showManageObjectsSheet = true },
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(
              bottom = if (uiState.navStatus == NavigationStatus.ROUTE_PREVIEW) 260.dp
              else if (uiState.tripState.isActive) 180.dp
              else 80.dp
            )
        )
      }

      // Start Trip Button / On-going Trip KM Calculator HUD
      if (uiState.navStatus == NavigationStatus.IDLE) {
        TripControlBar(
          tripState = uiState.tripState,
          onStartTrip = { viewModel.startTrip() },
          onPauseResumeTrip = { viewModel.pauseResumeTrip() },
          onStopTrip = { viewModel.stopTrip() },
          modifier = if (!uiState.tripState.isActive) {
            Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .padding(bottom = 24.dp)
          } else {
            Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .padding(bottom = 12.dp)
          }
        )
      }

      // Route Preview Mode Sheet
      if (uiState.navStatus == NavigationStatus.ROUTE_PREVIEW) {
        RoutePreviewSheet(
          originName = uiState.originName,
          destinationName = uiState.destinationName ?: "Destination",
          travelMode = uiState.travelMode,
          routeInfo = uiState.routeInfo,
          isCalculating = uiState.isCalculatingRoute,
          onModeSelected = { viewModel.setTravelMode(it) },
          onStartNavigation = { viewModel.startNavigation() },
          onSavePlace = {
            val destLoc = uiState.destinationLocation
            val destName = uiState.destinationName
            if (destLoc != null && destName != null) {
              viewModel.savePlace(destName, destName, destLoc, "favorite")
              Toast.makeText(context, "Saved to favorites!", Toast.LENGTH_SHORT).show()
            }
          },
          onCancel = { viewModel.stopNavigation() },
          modifier = Modifier.align(Alignment.BottomCenter)
        )
      }

      // Active Turn-by-Turn Navigation HUD (Live GPS)
      NavigationHud(
        navStatus = uiState.navStatus,
        progress = uiState.navProgress,
        isMuted = uiState.isMuted,
        isFollowingUser = uiState.isFollowingUser,
        onToggleMute = { viewModel.toggleMute() },
        onToggleFollow = { viewModel.toggleFollowUser() },
        onRecenter = { viewModel.recenterMap() },
        onStopNavigation = { viewModel.stopNavigation() }
      )

      // Add Object to Map Sheet/Modal
      if (uiState.isAddObjectDialogOpen) {
        val targetLocation = uiState.pendingObjectLocation ?: uiState.userLocation
        AddMapObjectDialog(
          location = targetLocation,
          onSave = { title, desc, type, lat, lng, radius, color ->
            viewModel.saveMapObject(title, desc, type, lat, lng, radius, color)
            Toast.makeText(context, "Added object to map!", Toast.LENGTH_SHORT).show()
          },
          onDismiss = { viewModel.closeAddObjectDialog() }
        )
      }

      // Map Object Detail Sheet (when tapping any object on map or list)
      uiState.selectedMapObject?.let { selectedObj ->
        MapObjectDetailSheet(
          mapObject = selectedObj,
          onNavigateTo = { obj ->
            viewModel.clearSelectedMapObject()
            viewModel.selectDestination(obj.toLatLng(), obj.title)
          },
          onDelete = { obj ->
            viewModel.deleteMapObject(obj)
            Toast.makeText(context, "Object deleted", Toast.LENGTH_SHORT).show()
          },
          onDismiss = { viewModel.clearSelectedMapObject() }
        )
      }

      // Manage Map Objects Sheet
      if (showManageObjectsSheet) {
        ManageMapObjectsSheet(
          objects = mapObjects,
          onObjectSelected = { obj ->
            showManageObjectsSheet = false
            viewModel.onMapObjectClick(obj.id, obj.title, obj.objectType, obj.latitude, obj.longitude)
            viewModel.onMapCommand?.invoke("setCenter(${obj.latitude}, ${obj.longitude}, 16)")
          },
          onNavigateTo = { obj ->
            showManageObjectsSheet = false
            viewModel.selectDestination(obj.toLatLng(), obj.title)
          },
          onDeleteObject = { obj ->
            viewModel.deleteMapObject(obj)
          },
          onAddNewObject = {
            viewModel.openAddObjectDialog()
          },
          onDismiss = { showManageObjectsSheet = false }
        )
      }

      // Saved Places Modal Sheet
      if (showSavedPlacesSheet) {
        SavedPlacesSheet(
          places = savedPlaces,
          onPlaceSelected = { savedPlace ->
            showSavedPlacesSheet = false
            viewModel.selectDestination(savedPlace.toLatLng(), savedPlace.name)
          },
          onDeletePlace = { viewModel.deleteSavedPlace(it) },
          onDismiss = { showSavedPlacesSheet = false }
        )
      }

      // Completed Trip Summary Modal
      uiState.tripSummary?.let { summary ->
        TripSummaryDialog(
          summary = summary,
          onDismiss = { viewModel.dismissTripSummary() }
        )
      }
    }
  }
}
