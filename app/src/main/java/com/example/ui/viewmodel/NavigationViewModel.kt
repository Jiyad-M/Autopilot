package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.OsmNavigationRepository
import com.example.data.db.AppDatabase
import com.example.data.db.MapObjectEntity
import com.example.data.db.SavedPlaceEntity
import com.example.data.location.LocationTracker
import com.example.data.model.LatLng
import com.example.data.model.MapTheme
import com.example.data.model.NavigationProgress
import com.example.data.model.NavigationStatus
import com.example.data.model.PlaceSearchResult
import com.example.data.model.RouteInfo
import com.example.data.model.TravelMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

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
  val isMuted: Boolean = false,
  val isFollowingUser: Boolean = true,
  val searchQuery: String = "",
  val searchResults: List<PlaceSearchResult> = emptyList(),
  val isSearching: Boolean = false,
  val activeStepIndex: Int = 0,
  val errorMessage: String? = null,
  val selectedMapObject: MapObjectEntity? = null,
  val isAddObjectDialogOpen: Boolean = false,
  val pendingObjectLocation: LatLng? = null
)

class NavigationViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = OsmNavigationRepository()
  private val locationTracker = LocationTracker(application)
  private val database = AppDatabase.getDatabase(application)
  private val savedPlaceDao = database.savedPlaceDao()
  private val mapObjectDao = database.mapObjectDao()

  val savedPlaces = savedPlaceDao.getAllPlaces()
  val mapObjects = mapObjectDao.getAllObjects()

  private val _uiState = MutableStateFlow(NavigationUiState())
  val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

  private var locationJob: Job? = null
  private var searchDebounceJob: Job? = null
  private var currentCachedObjects: List<MapObjectEntity> = emptyList()

  // Callback to Leaflet WebView
  var onMapCommand: ((String) -> Unit)? = null

  init {
    initUserLocation()
    seedDefaultSavedPlaces()
    observeMapObjects()
  }

  private fun initUserLocation() {
    locationTracker.getLastKnownLocation { loc ->
      _uiState.update { it.copy(userLocation = loc, originLocation = loc) }
      onMapCommand?.invoke("setUserPuck(${loc.latitude}, ${loc.longitude}, 0)")
      onMapCommand?.invoke("setCenter(${loc.latitude}, ${loc.longitude}, 15)")
    }
  }

  private fun observeMapObjects() {
    viewModelScope.launch {
      mapObjects.collect { objects ->
        currentCachedObjects = objects
        syncObjectsToMap(objects)
      }
    }
  }

  private fun syncObjectsToMap(objects: List<MapObjectEntity>) {
    if (onMapCommand == null) return
    val jsonArr = JSONArray()
    objects.forEach { obj ->
      val item = JSONObject()
      item.put("id", obj.id)
      item.put("title", obj.title)
      item.put("description", obj.description)
      item.put("objectType", obj.objectType)
      item.put("latitude", obj.latitude)
      item.put("longitude", obj.longitude)
      item.put("radiusMeters", obj.radiusMeters)
      item.put("colorHex", obj.colorHex)
      jsonArr.put(item)
    }
    onMapCommand?.invoke("renderCustomObjects($jsonArr)")
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

          onMapCommand?.invoke("updateUserPuck(${newLatLng.latitude}, ${newLatLng.longitude}, $heading)")
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

  fun onMapReady() {
    val loc = _uiState.value.userLocation
    onMapCommand?.invoke("setUserPuck(${loc.latitude}, ${loc.longitude}, ${_uiState.value.userHeading})")
    onMapCommand?.invoke("setCenter(${loc.latitude}, ${loc.longitude}, 15)")
    onMapCommand?.invoke("setTileLayer('${_uiState.value.mapTheme.id}')")
    syncObjectsToMap(currentCachedObjects)
  }

  fun setMapTheme(theme: MapTheme) {
    _uiState.update { it.copy(mapTheme = theme) }
    onMapCommand?.invoke("setTileLayer('${theme.id}')")
  }

  fun onMapClick(lat: Double, lng: Double) {
    val tappedLoc = LatLng(lat, lng)

    // If currently creating an object, set location to tapped point
    if (_uiState.value.isAddObjectDialogOpen) {
      _uiState.update { it.copy(pendingObjectLocation = tappedLoc) }
      return
    }

    viewModelScope.launch {
      val placeName = repository.reverseGeocode(tappedLoc)
      selectDestination(tappedLoc, placeName)
    }
  }

  fun onMapObjectClick(id: Long, title: String, type: String, lat: Double, lng: Double) {
    val obj = currentCachedObjects.firstOrNull { it.id == id } ?: MapObjectEntity(
      id = id,
      title = title,
      objectType = type,
      latitude = lat,
      longitude = lng
    )
    _uiState.update { it.copy(selectedMapObject = obj) }
  }

  fun clearSelectedMapObject() {
    _uiState.update { it.copy(selectedMapObject = null) }
  }

  fun openAddObjectDialog(location: LatLng? = null) {
    val targetLoc = location ?: _uiState.value.userLocation
    _uiState.update {
      it.copy(
        isAddObjectDialogOpen = true,
        pendingObjectLocation = targetLoc
      )
    }
  }

  fun closeAddObjectDialog() {
    _uiState.update {
      it.copy(
        isAddObjectDialogOpen = false,
        pendingObjectLocation = null
      )
    }
  }

  fun saveMapObject(
    title: String,
    description: String,
    objectType: String,
    latitude: Double,
    longitude: Double,
    radiusMeters: Double,
    colorHex: String
  ) {
    viewModelScope.launch {
      mapObjectDao.insertObject(
        MapObjectEntity(
          title = title.ifBlank { "Map Object" },
          description = description,
          objectType = objectType,
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          colorHex = colorHex
        )
      )
      closeAddObjectDialog()
    }
  }

  fun deleteMapObject(obj: MapObjectEntity) {
    viewModelScope.launch {
      mapObjectDao.deleteObject(obj)
      _uiState.update { it.copy(selectedMapObject = null) }
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

  fun startNavigation() {
    val route = _uiState.value.routeInfo ?: return
    if (route.steps.isEmpty() || route.coordinates.isEmpty()) return

    _uiState.update {
      it.copy(
        navStatus = NavigationStatus.NAVIGATING,
        activeStepIndex = 0,
        isFollowingUser = true
      )
    }

    startTrackingLocation()
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
      currentLocation = currentLoc
    )

    _uiState.update { it.copy(navProgress = progress) }
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
    _uiState.update {
      it.copy(
        navStatus = NavigationStatus.IDLE,
        navProgress = null,
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
    searchDebounceJob?.cancel()
  }
}
