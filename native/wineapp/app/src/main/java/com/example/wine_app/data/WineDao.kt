package com.example.wine_app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao
interface WineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWine(wine: Wine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wines: List<Wine>)

    @Query("UPDATE wine SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("DELETE FROM wine WHERE id = :id")
    suspend fun deletePermanently(id: Int)

    @Query("SELECT * FROM wine WHERE id = :id")
    suspend fun getWineById(id: Int): Wine?

    @Query("SELECT * FROM wine WHERE status != 'PENDING_DELETE'")
    fun getVisibleWines(): Flow<List<Wine>>

    @Query("SELECT * FROM wine WHERE status != 'SYNCED'")
    suspend fun getPendingWines(): List<Wine>


    @Query("DELETE FROM wine WHERE status = 'SYNCED'")
    suspend fun clearAllSynced()
}