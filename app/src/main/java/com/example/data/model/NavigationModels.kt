package com.example.data.model

data class LatLng(
  val latitude: Double,
  val longitude: Double
) {
  fun distanceTo(other: LatLng): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(other.latitude - this.latitude)
    val dLng = Math.toRadians(other.longitude - this.longitude)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude)) *
        Math.sin(dLng / 2) * Math.sin(dLng / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
  }

  fun bearingTo(other: LatLng): Float {
    val lat1 = Math.toRadians(this.latitude)
    val lat2 = Math.toRadians(other.latitude)
    val dLng = Math.toRadians(other.longitude - this.longitude)
    val y = Math.sin(dLng) * Math.cos(lat2)
    val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
    val brng = Math.toDegrees(Math.atan2(y, x))
    return ((brng + 360) % 360).toFloat()
  }
}

enum class TravelMode(val key: String, val label: String) {
  DRIVING("driving", "Drive"),
  CYCLING("bicycling", "Bike"),
  WALKING("walking", "Walk")
}

enum class MapTheme(val id: String, val title: String) {
  LIGHT("light", "Clean Light"),
  DARK("dark", "Dark Mode"),
  STREETS("streets", "OSM Streets"),
  SATELLITE("satellite", "Satellite"),
  TERRAIN("terrain", "Outdoors Topo")
}

data class RouteStep(
  val instruction: String,
  val distanceMeters: Double,
  val durationSeconds: Double,
  val maneuverType: String,
  val maneuverModifier: String?,
  val location: LatLng,
  val streetName: String
)

data class RouteInfo(
  val distanceMeters: Double,
  val durationSeconds: Double,
  val coordinates: List<LatLng>,
  val steps: List<RouteStep>,
  val summary: String,
  val travelMode: TravelMode
)

data class PlaceSearchResult(
  val placeId: Long,
  val displayName: String,
  val name: String,
  val location: LatLng,
  val type: String?,
  val category: String?
)

enum class NavigationStatus {
  IDLE,
  ROUTE_PREVIEW,
  NAVIGATING,
  ARRIVED
}

data class NavigationProgress(
  val currentStepIndex: Int,
  val currentStep: RouteStep,
  val nextStep: RouteStep?,
  val distanceToNextManeuverMeters: Double,
  val remainingDistanceMeters: Double,
  val remainingDurationSeconds: Double,
  val currentSpeedKmh: Double,
  val currentHeading: Float,
  val currentLocation: LatLng,
  val isSimulation: Boolean = false
)
