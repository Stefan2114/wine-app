package com.example.wine_app.data

import kotlinx.coroutines.flow.Flow

interface WineRepository {
    fun getWines(): Flow<List<Wine>>
    suspend fun addWine(wine: Wine)
    suspend fun updateWine(wine: Wine)

    suspend fun deleteWine(wine: Wine)
    suspend fun getWineById(id: Int): Wine?
    suspend fun fetchWinesFromServer()
    suspend fun getPendingWines(): List<Wine>
    suspend fun executeSyncCreate(wine: Wine)
    suspend fun executeSyncUpdate(wine: Wine)
    suspend fun executeSyncDelete(wine: Wine)
    suspend fun deletePermanently(id: Int)
    fun enqueueSyncWork()

}