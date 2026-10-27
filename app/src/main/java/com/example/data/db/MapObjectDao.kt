package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MapObjectDao {
  @Query("SELECT * FROM map_objects ORDER BY createdAt DESC")
  fun getAllObjects(): Flow<List<MapObjectEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertObject(obj: MapObjectEntity): Long

  @Delete
  suspend fun deleteObject(obj: MapObjectEntity)

  @Query("DELETE FROM map_objects WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM map_objects")
  suspend fun clearAllObjects()
}
