package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.LatLng

@Entity(tableName = "map_objects")
data class MapObjectEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val description: String = "",
  val objectType: String, // "marker", "circle", "hazard", "zone"
  val latitude: Double,
  val longitude: Double,
  val radiusMeters: Double = 0.0,
  val colorHex: String = "#0284C7",
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toLatLng(): LatLng = LatLng(latitude, longitude)
}
