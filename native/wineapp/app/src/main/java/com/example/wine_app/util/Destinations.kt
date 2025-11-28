package com.example.wine_app.util

import kotlinx.serialization.Serializable

@Serializable
object WineList

@Serializable
data class AddEditWine(val wineId: Int = -1)