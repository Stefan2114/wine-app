package com.example.wine_app.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Wine::class],
    version = 2,
//    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class WineDatabase: RoomDatabase() {

    abstract val dao: WineDao
}