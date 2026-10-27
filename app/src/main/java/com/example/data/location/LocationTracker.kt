package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.data.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationTracker(private val context: Context) {

  private val fusedLocationClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(context)

  // Default coordinate if GPS is pending: San Francisco Downtown
  val defaultLocation = LatLng(37.7749, -122.4194)

  @SuppressLint("MissingPermission")
  fun getLocationUpdates(intervalMs: Long = 3000L): Flow<Location> = callbackFlow {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
      .setMinUpdateDistanceMeters(2f)
      .setMinUpdateIntervalMillis(intervalMs / 2)
      .build()

    val callback = object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        result.lastLocation?.let { trySend(it) }
      }
    }

    try {
      fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
    } catch (e: SecurityException) {
      // Permission not granted yet, close flow gracefully
      close(e)
    }

    awaitClose {
      fusedLocationClient.removeLocationUpdates(callback)
    }
  }

  @SuppressLint("MissingPermission")
  fun getLastKnownLocation(onResult: (LatLng) -> Unit) {
    try {
      fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) {
          onResult(LatLng(loc.latitude, loc.longitude))
        } else {
          onResult(defaultLocation)
        }
      }.addOnFailureListener {
        onResult(defaultLocation)
      }
    } catch (e: SecurityException) {
      onResult(defaultLocation)
    }
  }
}
