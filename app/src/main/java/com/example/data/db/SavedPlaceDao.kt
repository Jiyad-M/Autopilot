package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
  @Query("SELECT * FROM saved_places ORDER BY createdAt DESC")
  fun getAllPlaces(): Flow<List<SavedPlaceEntity>>

  @Query("SELECT * FROM saved_places WHERE category = :category ORDER BY createdAt DESC")
  fun getPlacesByCategory(category: String): Flow<List<SavedPlaceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlace(place: SavedPlaceEntity): Long

  @Delete
  suspend fun deletePlace(place: SavedPlaceEntity)

  @Query("DELETE FROM saved_places WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM saved_places WHERE category = 'recent'")
  suspend fun clearRecents()
}
