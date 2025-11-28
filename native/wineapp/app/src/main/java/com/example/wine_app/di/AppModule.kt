package com.example.wine_app.di

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Room
import androidx.work.WorkManager
import com.example.wine_app.data.ApiService
import com.example.wine_app.data.NetworkMonitor
import com.example.wine_app.data.NetworkMonitorImpl
import com.example.wine_app.data.WineDao
import com.example.wine_app.data.WineDatabase
import com.example.wine_app.data.WineRepository
import com.example.wine_app.data.WineRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWineDatabase(app: Application): WineDatabase {
        return Room.databaseBuilder(
            app,
            WineDatabase::class.java,
            "wine_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideWineDao(db: WineDatabase): WineDao {
        return db.dao
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(app: Application): WorkManager {
        return WorkManager.getInstance(app)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    @Singleton
    fun provideWineRepository(
        db: WineDatabase,
        api: ApiService,
        networkMonitor: NetworkMonitor,
        workManager: WorkManager,
        @ApplicationContext context: Context
    ): WineRepository {
        return WineRepositoryImpl(db.dao, api, networkMonitor, workManager, context)
    }
}