package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.OsmNavigationRepository
import com.example.data.db.AppDatabase
import com.example.data.db.SavedPlaceEntity
import com.example.data.location.LocationTracker
import com.example.data.model.LatLng
import com.example.data.model.MapTheme
import com.example.data.model.NavigationProgress
import com.example.data.model.NavigationStatus
import com.example.data.model.PlaceSearchResult
import com.example.data.model.RouteInfo
import com.example.data.model.RouteStep
import com.example.data.model.TravelMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavigationUiState(
  val userLocation: LatLng = LatLng(37.7749, -122.4194),
  val userHeading: Float = 0f,
  val mapTheme: MapTheme = MapTheme.LIGHT,
  val navStatus: NavigationStatus = NavigationStatus.IDLE,
  val originName: String = "Current Location",
  val originLocation: LatLng = LatLng(37.7749, -122.4194),
  val destinationName: String? = null,
  val destinationLocation: LatLng? = null,
  val travelMode: TravelMode = TravelMode.DRIVING,
  val routeInfo: RouteInfo? = null,
  val navProgress: NavigationProgress? = null,
  val isCalculatingRoute: Boolean = false,
  val isSimulating: Boolean = false,
  val simulationSpeed: Float = 1.5f,
  val isMuted: Boolean = false,
  val isFollowingUser: Boolean = true,
  val searchQuery: String = "",
  val searchResults: List<PlaceSearchResult> = emptyList(),
  val isSearching: Boolean = false,
  val activeStepIndex: Int = 0,
  val errorMessage: String? = null
)

class NavigationViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = OsmNavigationRepository()
  private val locationTracker = LocationTracker(application)
  private val database = AppDatabase.getDatabase(application)
  private val savedPlaceDao = database.savedPlaceDao()

  val savedPlaces = savedPlaceDao.getAllPlaces()

  private val _uiState = MutableStateFlow(NavigationUiState())
  val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

  private var locationJob: Job? = null
  private var simulationJob: Job? = null
  private var searchDebounceJob: Job? = null

  // Callback to Leaflet WebView
  var onMapCommand: ((String) -> Unit)? = null

  init {
    initUserLocation()
    seedDefaultSavedPlaces()
  }

  private fun initUserLocation() {
    locationTracker.getLastKnownLocation { loc ->
      _uiState.update { it.copy(userLocation = loc, originLocation = loc) }
      onMapCommand?.invoke("setUserPuck(${loc.latitude}, ${loc.longitude}, 0)")
      onMapCommand?.invoke("setCenter(${loc.latitude}, ${loc.longitude}, 15)")
    }
  }

  fun startTrackingLocation() {
    locationJob?.cancel()
    locationJob = viewModelScope.launch {
      locationTracker.getLocationUpdates(2500L)
        .catch { /* permission denied or unavailable */ }
        .collect { location ->
          val newLatLng = LatLng(location.latitude, location.longitude)
          val heading = if (location.hasBearing()) location.bearing else _uiState.value.userHeading
          _uiState.update {
            it.copy(
              userLocation = newLatLng,
              userHeading = heading
            )
          }

          if (!_uiState.value.isSimulating) {
            onMapCommand?.invoke("setUserPuck(${newLatLng.latitude}, ${newLatLng.longitude}, $heading)")
            if (_uiState.value.isFollowingUser) {
              onMapCommand?.invoke("setCenter(${newLatLng.latitude}, ${newLatLng.longitude})")
            }

            // If active real navigation, update progress
            if (_uiState.value.navStatus == NavigationStatus.NAVIGATING) {
              updateRealNavProgress(newLatLng, heading, location.speed * 3.6) // m/s to km/h
            }
          }
        }
    }
  }

  fun onMapReady() {
    val loc = _uiState.value.userLocation
    onMapCommand?.invoke("setUserPuck(${loc.latitude}, ${loc.longitude}, ${_uiState.value.userHeading})")
    onMapCommand?.invoke("setCenter(${loc.latitude}, ${loc.longitude}, 15)")
    onMapCommand?.invoke("setTileLayer('${_uiState.value.mapTheme.id}')")
  }

  fun setMapTheme(theme: MapTheme) {
    _uiState.update { it.copy(mapTheme = theme) }
    onMapCommand?.invoke("setTileLayer('${theme.id}')")
  }

  fun onMapClick(lat: Double, lng: Double) {
    val tappedLoc = LatLng(lat, lng)
    viewModelScope.launch {
      val placeName = repository.reverseGeocode(tappedLoc)
      selectDestination(tappedLoc, placeName)
    }
  }

  fun onSearchQueryChanged(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    searchDebounceJob?.cancel()
    if (query.trim().length >= 2) {
      searchDebounceJob = viewModelScope.launch {
        delay(400)
        _uiState.update { it.copy(isSearching = true) }
        val results = repository.searchPlaces(query, _uiState.value.userLocation)
        _uiState.update { it.copy(searchResults = results, isSearching = false) }
      }
    } else {
      _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
    }
  }

  fun searchCategory(categoryName: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSearching = true, searchQuery = categoryName) }
      val results = repository.searchNearbyCategory(categoryName, _uiState.value.userLocation)
      _uiState.update { it.copy(searchResults = results, isSearching = false) }
    }
  }

  fun selectSearchResult(result: PlaceSearchResult) {
    _uiState.update {
      it.copy(
        searchQuery = "",
        searchResults = emptyList(),
        isSearching = false
      )
    }
    selectDestination(result.location, result.name)

    // Save to recents in DB
    viewModelScope.launch {
      savedPlaceDao.insertPlace(
        SavedPlaceEntity(
          name = result.name,
          address = result.displayName,
          latitude = result.location.latitude,
          longitude = result.location.longitude,
          category = "recent"
        )
      )
    }
  }

  fun selectDestination(dest: LatLng, name: String) {
    _uiState.update {
      it.copy(
        destinationLocation = dest,
        destinationName = name,
        navStatus = NavigationStatus.ROUTE_PREVIEW
      )
    }
    calculateCurrentRoute()
  }

  fun setTravelMode(mode: TravelMode) {
    if (_uiState.value.travelMode == mode) return
    _uiState.update { it.copy(travelMode = mode) }
    calculateCurrentRoute()
  }

  fun calculateCurrentRoute() {
    val state = _uiState.value
    val origin = state.originLocation
    val destination = state.destinationLocation ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isCalculatingRoute = true, errorMessage = null) }
      try {
        val route = repository.calculateRoute(origin, destination, state.travelMode)
        _uiState.update {
          it.copy(
            routeInfo = route,
            isCalculatingRoute = false,
            navStatus = NavigationStatus.ROUTE_PREVIEW
          )
        }

        // Draw on Leaflet Map
        val coordsJson = route.coordinates.joinToString(separator = ",", prefix = "[", postfix = "]") {
          "[${it.latitude}, ${it.longitude}]"
        }
        onMapCommand?.invoke("setStartDestinationPins(${origin.latitude}, ${origin.longitude}, ${destination.latitude}, ${destination.longitude}, '${state.originName}', '${state.destinationName}')")
        onMapCommand?.invoke("drawRoute($coordsJson, true)")
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isCalculatingRoute = false,
            errorMessage = "Failed to calculate route: ${e.message}"
          )
        }
      }
    }
  }

  fun startNavigation(isSimulation: Boolean = false) {
    val route = _uiState.value.routeInfo ?: return
    if (route.steps.isEmpty() || route.coordinates.isEmpty()) return

    _uiState.update {
      it.copy(
        navStatus = NavigationStatus.NAVIGATING,
        isSimulating = isSimulation,
        activeStepIndex = 0,
        isFollowingUser = true
      )
    }

    if (isSimulation) {
      startRouteSimulation(route)
    } else {
      startTrackingLocation()
    }
  }

  private fun startRouteSimulation(route: RouteInfo) {
    simulationJob?.cancel()
    simulationJob = viewModelScope.launch {
      val polyline = route.coordinates
      if (polyline.isEmpty()) return@launch

      val totalPoints = polyline.size
      var currentIdx = 0
      var currentStepIdx = 0

      while (currentIdx < totalPoints && _uiState.value.navStatus == NavigationStatus.NAVIGATING) {
        val currPt = polyline[currentIdx]
        val nextPt = polyline.getOrNull(currentIdx + 1) ?: currPt
        val heading = currPt.bearingTo(nextPt)

        // Find active step
        val step = route.steps.getOrNull(currentStepIdx) ?: route.steps.last()
        val nextStep = route.steps.getOrNull(currentStepIdx + 1)
        val distToStep = currPt.distanceTo(step.location)

        if (distToStep < 25.0 && currentStepIdx < route.steps.size - 1) {
          currentStepIdx++
          _uiState.update { it.copy(activeStepIndex = currentStepIdx) }
        }

        // Calculate remaining distance & duration
        var remDist = 0.0
        for (k in currentIdx until totalPoints - 1) {
          remDist += polyline[k].distanceTo(polyline[k + 1])
        }
        val speedMps = when (route.travelMode) {
          TravelMode.DRIVING -> 14.0 * _uiState.value.simulationSpeed
          TravelMode.CYCLING -> 5.0 * _uiState.value.simulationSpeed
          TravelMode.WALKING -> 1.4 * _uiState.value.simulationSpeed
        }
        val remDur = if (speedMps > 0) remDist / speedMps else 0.0
        val speedKmh = speedMps * 3.6

        val progress = NavigationProgress(
          currentStepIndex = currentStepIdx,
          currentStep = route.steps.getOrElse(currentStepIdx) { route.steps.last() },
          nextStep = route.steps.getOrNull(currentStepIdx + 1),
          distanceToNextManeuverMeters = distToStep,
          remainingDistanceMeters = remDist,
          remainingDurationSeconds = remDur,
          currentSpeedKmh = speedKmh,
          currentHeading = heading,
          currentLocation = currPt,
          isSimulation = true
        )

        _uiState.update {
          it.copy(
            navProgress = progress,
            userLocation = currPt,
            userHeading = heading
          )
        }

        // Send to map
        onMapCommand?.invoke("updateUserPuck(${currPt.latitude}, ${currPt.longitude}, $heading)")
        if (_uiState.value.isFollowingUser) {
          onMapCommand?.invoke("setCenter(${currPt.latitude}, ${currPt.longitude})")
        }

        currentIdx++
        val baseDelay = when (route.travelMode) {
          TravelMode.DRIVING -> 400L
          TravelMode.CYCLING -> 600L
          TravelMode.WALKING -> 800L
        }
        val actualDelay = (baseDelay / _uiState.value.simulationSpeed).toLong().coerceAtLeast(100L)
        delay(actualDelay)
      }

      // Reached destination!
      _uiState.update { it.copy(navStatus = NavigationStatus.ARRIVED) }
    }
  }

  private fun updateRealNavProgress(currentLoc: LatLng, heading: Float, speedKmh: Double) {
    val route = _uiState.value.routeInfo ?: return
    val currentStepIdx = _uiState.value.activeStepIndex
    val currentStep = route.steps.getOrNull(currentStepIdx) ?: return
    val nextStep = route.steps.getOrNull(currentStepIdx + 1)
    val distToNext = currentLoc.distanceTo(currentStep.location)

    var stepIdx = currentStepIdx
    if (distToNext < 30.0 && currentStepIdx < route.steps.size - 1) {
      stepIdx++
      _uiState.update { it.copy(activeStepIndex = stepIdx) }
    }

    val dest = _uiState.value.destinationLocation ?: return
    val totalRemaining = currentLoc.distanceTo(dest)
    if (totalRemaining < 20.0) {
      _uiState.update { it.copy(navStatus = NavigationStatus.ARRIVED) }
      return
    }

    val estDuration = if (speedKmh > 5.0) (totalRemaining / (speedKmh / 3.6)) else (totalRemaining / 12.0)

    val progress = NavigationProgress(
      currentStepIndex = stepIdx,
      currentStep = route.steps.getOrElse(stepIdx) { route.steps.last() },
      nextStep = nextStep,
      distanceToNextManeuverMeters = distToNext,
      remainingDistanceMeters = totalRemaining,
      remainingDurationSeconds = estDuration,
      currentSpeedKmh = speedKmh.coerceAtLeast(0.0),
      currentHeading = heading,
      currentLocation = currentLoc,
      isSimulation = false
    )

    _uiState.update { it.copy(navProgress = progress) }
  }

  fun setSimulationSpeed(multiplier: Float) {
    _uiState.update { it.copy(simulationSpeed = multiplier) }
  }

  fun toggleFollowUser() {
    _uiState.update { it.copy(isFollowingUser = !it.isFollowingUser) }
    if (_uiState.value.isFollowingUser) {
      val loc = _uiState.value.userLocation
      onMapCommand?.invoke("setCenter(${loc.latitude}, ${loc.longitude})")
    }
  }

  fun toggleMute() {
    _uiState.update { it.copy(isMuted = !it.isMuted) }
  }

  fun recenterMap() {
    val loc = _uiState.value.userLocation
    _uiState.update { it.copy(isFollowingUser = true) }
    onMapCommand?.invoke("setCenter(${loc.latitude}, ${loc.longitude}, 16)")
  }

  fun zoomIn() {
    onMapCommand?.invoke("zoomIn()")
  }

  fun zoomOut() {
    onMapCommand?.invoke("zoomOut()")
  }

  fun stopNavigation() {
    simulationJob?.cancel()
    _uiState.update {
      it.copy(
        navStatus = NavigationStatus.IDLE,
        navProgress = null,
        isSimulating = false,
        destinationLocation = null,
        destinationName = null,
        routeInfo = null
      )
    }
    onMapCommand?.invoke("clearAll()")
    recenterMap()
  }

  fun savePlace(name: String, address: String, latLng: LatLng, category: String) {
    viewModelScope.launch {
      savedPlaceDao.insertPlace(
        SavedPlaceEntity(
          name = name,
          address = address,
          latitude = latLng.latitude,
          longitude = latLng.longitude,
          category = category
        )
      )
    }
  }

  fun deleteSavedPlace(place: SavedPlaceEntity) {
    viewModelScope.launch {
      savedPlaceDao.deletePlace(place)
    }
  }

  private fun seedDefaultSavedPlaces() {
    viewModelScope.launch {
      // Add standard quick shortcuts if empty
      savedPlaceDao.insertPlace(
        SavedPlaceEntity(
          id = 1,
          name = "Ferry Building Market",
          address = "1 Ferry Building, San Francisco, CA",
          latitude = 37.7955,
          longitude = -122.3937,
          category = "favorite"
        )
      )
      savedPlaceDao.insertPlace(
        SavedPlaceEntity(
          id = 2,
          name = "Golden Gate Park",
          address = "Golden Gate Park, San Francisco, CA",
          latitude = 37.7694,
          longitude = -122.4862,
          category = "favorite"
        )
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    locationJob?.cancel()
    simulationJob?.cancel()
    searchDebounceJob?.cancel()
  }
}
