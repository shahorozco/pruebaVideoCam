package com.coordinadora.pruebavideocam.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.coordinadora.pruebavideocam.database.dao.GlobalDao
import com.coordinadora.pruebavideocam.database.entity.Globals

@Database([Globals::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun globalDao(): GlobalDao?
}