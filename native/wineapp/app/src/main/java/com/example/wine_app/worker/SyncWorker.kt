package com.example.wine_app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wine_app.data.SyncStatus
import com.example.wine_app.data.WineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.io.IOException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WineRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work...")

        val pendingWines = repository.getPendingWines()
        if (pendingWines.isEmpty()) {
            Log.d(TAG, "No pending items to sync.")
            return fetchWinesFromServer()
        }

        Log.d(TAG, "Syncing ${pendingWines.size} items...")

        pendingWines.forEach { wine ->
            try {
                when (wine.status) {
                    SyncStatus.PENDING_CREATE -> repository.executeSyncCreate(wine)
                    SyncStatus.PENDING_UPDATE -> repository.executeSyncUpdate(wine)
                    SyncStatus.PENDING_DELETE -> repository.executeSyncDelete(wine)
                    else -> {}
                }
                Log.i(TAG, "Sync successful for wine ${wine.id}")

            } catch (e: Exception) {

                Log.w(TAG, "Failed to sync wine ${wine.id}", e)
                when (e) {
                    is IOException -> {
                        // --- TEMPORARY: Network Error ---
                        Log.w(TAG, "Network error. Stopping sync to retry later.")
                        return Result.retry()
                    }
                    is HttpException -> {
                        if (e.code() in 500..599) {
                            // --- TEMPORARY: Server Error ---
                            Log.w(TAG, "Server error ${e.code()}. Stopping sync to retry later.")
                            return Result.retry()
                        } else {
                            Log.e(TAG, "Client error ${e.code()} for wine ${wine.id}. Notifying user and removing item.", e)
                            repository.deletePermanently(wine.id!!)
                        }
                    }
                    else -> {
                        Log.e(TAG, "Critical error for wine ${wine.id}. Notifying user and removing item.", e)
                        repository.deletePermanently(wine.id!!)
                    }
                }
            }
        }

        Log.d(TAG, "Synchronization loop complete.")
        return fetchWinesFromServer()
    }

    private suspend fun fetchWinesFromServer(): Result{
        try {
            repository.fetchWinesFromServer()
        } catch (e: Exception) {
            Log.e(TAG, "Post-sync fetch failed. This will be retried on next connection.", e)
            if (e is IOException || (e is HttpException && e.code() in 500..599)) {
                return Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "WineSyncWork"
        private const val TAG = "SyncWorker"
    }
}