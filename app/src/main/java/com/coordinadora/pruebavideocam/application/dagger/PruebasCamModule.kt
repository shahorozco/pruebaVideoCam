package com.coordinadora.pruebavideocam.application.dagger

import androidx.room.Room
import com.coordinadora.pruebavideocam.database.AppDatabase
import com.coordinadora.pruebavideocam.utils.Connectivity
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class PruebasCamModule(val pruebasCamApplication: PruebasCamApplication) {
    @Provides
    @Singleton
    fun pruebasCamApplicationManager() = pruebasCamApplication

    @Provides
    @Singleton
    fun databaseManager(): AppDatabase {
        return Room.databaseBuilder(
            this.pruebasCamApplication,
            AppDatabase::class.java, "app-database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun connectivyManager(): Connectivity{
        return Connectivity(pruebasCamApplication)
    }
}