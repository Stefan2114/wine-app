package com.example.wine_app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING_CREATE = "PENDING_CREATE"
    const val PENDING_UPDATE = "PENDING_UPDATE"
    const val PENDING_DELETE = "PENDING_DELETE"
}

@Entity
data class Wine(
    @PrimaryKey //(autoGenerate = true)
    val id: Int? = null,
    val name: String,
    val price: Double,
    val productionDate: String,
    val origin: String?,
    val alcoholDegree: Double,
    var status: String = SyncStatus.PENDING_CREATE
)
