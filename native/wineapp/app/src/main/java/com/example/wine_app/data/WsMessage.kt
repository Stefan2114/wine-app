package com.example.wine_app.data

import com.google.gson.JsonElement

data class WsMessage(
    val type: String,
    val payload: JsonElement
)

// Helper class for the "WINE_DELETED" payload
data class DeletePayload(
    val id: Int
)