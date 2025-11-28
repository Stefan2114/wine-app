package com.example.wine_app.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.wine_app.di.NetworkModule
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class RealtimeSyncManager @Inject constructor(
    private val client: OkHttpClient,
    private val dao: WineDao,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectAttempts = 0

    // Build the WebSocket URL from your existing BASE_URL
    private val wsUrl: String = NetworkModule.BASE_URL
        .replace("http://", "ws://") + "ws"

    init {
        Log.d(TAG, "Initializing RealtimeSyncManager and connecting...")
        connect()
    }

    private fun connect() {
        if (webSocket != null) {
            Log.d(TAG, "Already connected.")
            return
        }

        Log.d(TAG, "Connecting to WebSocket: $wsUrl")

        val request = Request.Builder().url(wsUrl).build()
        val listener = WineWebSocketListener()

        val wsClient = client.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        webSocket = wsClient.newWebSocket(request, listener)
    }

    private fun scheduleReconnect() {

        val backoffMillis = (INITIAL_RECONNECT_DELAY * 2.0.pow(reconnectAttempts.toDouble())).toLong()
        Log.d(TAG, "Scheduling reconnect in $backoffMillis ms (Attempt ${reconnectAttempts + 1})")
        webSocket = null

        Handler(Looper.getMainLooper()).postDelayed({
            reconnectAttempts++
            connect()
        }, backoffMillis.coerceAtMost(MAX_RECONNECT_DELAY))
    }

    inner class WineWebSocketListener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket Connected!")
            reconnectAttempts = 0
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "New Message: $text")
            try {
                val message = gson.fromJson(text, WsMessage::class.java)
                scope.launch {
                    handleWsMessage(message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing WebSocket message", e)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.w(TAG, "WebSocket Closing: $code / $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.w(TAG, "WebSocket Closed: $code / $reason")
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket Failure: ${t.message}", t)
            scheduleReconnect()
        }
    }

    private suspend fun handleWsMessage(message: WsMessage) {
        when (message.type) {
            "WINE_ADDED" -> {
                val wine = gson.fromJson(message.payload, Wine::class.java)
                wine.status = SyncStatus.SYNCED
                dao.upsertWine(wine)
                Log.i(TAG, "Real-time ADD: ${wine.name}")
            }
            "WINE_UPDATED" -> {
                val wine = gson.fromJson(message.payload, Wine::class.java)
                wine.status = SyncStatus.SYNCED
                dao.upsertWine(wine)
                Log.i(TAG, "Real-time UPDATE: ${wine.name}")
            }
            "WINE_DELETED" -> {
                val deleteData = gson.fromJson(message.payload, DeletePayload::class.java)
                dao.deletePermanently(deleteData.id)
                Log.i(TAG, "Real-time DELETE: ID ${deleteData.id}")
            }
            else -> Log.w(TAG, "Unknown WebSocket message type: ${message.type}")
        }
    }

    companion object {
        private const val TAG = "RealtimeSyncManager"
        private const val INITIAL_RECONNECT_DELAY = 1000L // 1 second
        private const val MAX_RECONNECT_DELAY = 60000L // 1 minute
    }
}