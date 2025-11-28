package com.example.wine_app.data


import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.wine_app.exception.ServerNotRespondingException
import com.example.wine_app.worker.SyncWorker
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
class WineRepositoryImpl(
    private val dao: WineDao,
    private val api: ApiService,
    private val networkMonitor: NetworkMonitor,
    private val workManager: WorkManager,
    private val context: Context
) : WineRepository {

    private val TAG = "WineRepo"

    init {
        listenForNetworkChanges()
    }

    override fun getWines(): Flow<List<Wine>> {
        return dao.getVisibleWines()
    }

    override suspend fun getWineById(id: Int): Wine? {
        return dao.getWineById(id)
    }

    override suspend fun addWine(wine: Wine) {
        val wineToSave = wine.copy(status = SyncStatus.PENDING_CREATE)

        if (networkMonitor.isOnline()) {
            try {
                val response = api.createWine(wineToSave)

                if (response.isSuccessful && response.body() != null) {
                    val serverWine = response.body()!!
                    serverWine.status = SyncStatus.SYNCED
                    dao.upsertWine(serverWine)
                    Log.d(TAG, "Created new wine on server and saved to local DB")

                    // 3. Handle HTTP error responses (4xx, 5xx)
                } else {
                    Log.w(TAG, "Server responded with error ${response.code()}: ${response.message()}")
                    throw Exception("Server error ${response.code()}: ${response.message()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to server. Saving locally.", e)

                dao.upsertWine(wineToSave)
                enqueueSyncWork()
                throw ServerNotRespondingException("Couldn't connect. Saved locally for sync.")
            }
        } else {
            Log.d(TAG, "Offline: Saving new wine as pending")
            dao.upsertWine(wineToSave)
            enqueueSyncWork()
        }
    }

    override suspend fun updateWine(wine: Wine) {
        val wineId = wine.id!!
        val currentStatus = dao.getWineById(wineId)!!.status

        val newStatus = if (currentStatus == SyncStatus.PENDING_CREATE) {
            SyncStatus.PENDING_CREATE
        } else {
            SyncStatus.PENDING_UPDATE
        }

        val wineToSave = wine.copy(status = newStatus)

        if (networkMonitor.isOnline()) {
            try {
                val response = if (newStatus == SyncStatus.PENDING_UPDATE) {
                    api.updateWine(wineToSave.id!!, wineToSave)
                } else {
                    api.createWine(wineToSave)
                }

                if (response.isSuccessful && response.body() != null) {
                    val savedWine = response.body()!!
                    savedWine.status = SyncStatus.SYNCED

                    if (newStatus == SyncStatus.PENDING_CREATE) {
                        dao.deletePermanently(wineId)
                    }

                    dao.upsertWine(savedWine)
                    Log.d(TAG, "Saved to server and local DB")
                } else {
                    Log.w(TAG, "Server responded with error ${response.code()}: ${response.message()}")
                    throw Exception("Server error ${response.code()}: ${response.message()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to server. Saving locally.", e)
                dao.upsertWine(wineToSave)
                enqueueSyncWork()
                throw ServerNotRespondingException("Couldn't connect. Saved locally for sync.")
            }
        } else {
            Log.d(TAG, "Offline: Saving as pending")
            dao.upsertWine(wineToSave)
            enqueueSyncWork()
        }
    }

    override suspend fun deleteWine(wine: Wine) {
        if (wine.status != SyncStatus.SYNCED) {
            Log.d(TAG, "Deleting PENDING item locally.")
            dao.deletePermanently(wine.id!!)
            return
        }

        if (networkMonitor.isOnline()) {
            wine.id?.let {
                try {
                    val response = api.deleteWine(it)
                    if (response.isSuccessful) {
                        dao.deletePermanently(it)
                        Log.d(TAG, "Deleted from server and local DB")
                    } else {
                        Log.e(TAG, "Failed to delete from server: ${response.code()}")
                        throw Exception("Server error: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to delete from server. Marking for deletion.", e)
                    // Save "pending delete" state locally
                    dao.updateStatus(it, SyncStatus.PENDING_DELETE)
                    enqueueSyncWork()
                    throw ServerNotRespondingException("Couldn't connect. Will delete when online.")
                }
            }
        } else {
            Log.d(TAG, "Offline: Marking for deletion")
            wine.id?.let { dao.updateStatus(it, SyncStatus.PENDING_DELETE) }
            enqueueSyncWork()
        }
    }

    override suspend fun fetchWinesFromServer() {
        if (!networkMonitor.isOnline()) {
            Log.d(TAG, "Offline, cannot fetch from server")
            return
        }
        try {
            Log.d(TAG, "Fetching all wines from server...")
            val response = api.getWines()

            if (response.isSuccessful && response.body() != null) {
                val serverWines = response.body()!!
                serverWines.forEach { it.status = SyncStatus.SYNCED }
                dao.clearAllSynced()
                dao.insertAll(serverWines)
            } else {
                Log.e(TAG, "Failed to fetch from server: ${response.code()}")
                throw Exception("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch from server", e)
            throw ServerNotRespondingException("Couldn't connect to server to fetch wines.")
        }
    }

    override suspend fun getPendingWines(): List<Wine> {
        return dao.getPendingWines()
    }

    override suspend fun executeSyncCreate(wine: Wine) {
        val response = api.createWine(wine)
        if (response.isSuccessful && response.body() != null) {
            val syncedWine = response.body()!!
            syncedWine.status = SyncStatus.SYNCED
            dao.deletePermanently(wine.id!!)
            dao.upsertWine(syncedWine)
        } else {
            throw Exception("Sync failed with server error: ${response.code()}")
        }
    }

    override suspend fun executeSyncUpdate(wine: Wine) {
        val response = api.updateWine(wine.id!!, wine)
        if (response.isSuccessful && response.body() != null) {
            val syncedWine = response.body()!!
            syncedWine.status = SyncStatus.SYNCED
            dao.upsertWine(syncedWine)
        } else {
            throw Exception("Sync failed with server error: ${response.code()}")
        }
    }

    override suspend fun executeSyncDelete(wine: Wine) {
        val response = api.deleteWine(wine.id!!)
        if (response.isSuccessful) {
            dao.deletePermanently(wine.id)
        } else {
            throw Exception("Sync failed with server error: ${response.code()}")
        }
    }

    override suspend fun deletePermanently(id: Int) {
        dao.deletePermanently(id)
    }

    private fun listenForNetworkChanges() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "Network is available. Enqueuing sync worker.")
                enqueueSyncWork()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "Network lost.")
            }
        })
    }

    override fun enqueueSyncWork() {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(SyncWorker.WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            syncRequest
        )

        Log.d(TAG, "Sync work enqueued.")
    }
}