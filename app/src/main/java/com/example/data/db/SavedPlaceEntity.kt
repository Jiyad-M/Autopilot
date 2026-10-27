package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.LatLng

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val address: String,
  val latitude: Double,
  val longitude: Double,
  val category: String, // "home", "work", "favorite", "recent"
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toLatLng(): LatLng = LatLng(latitude, longitude)
}
