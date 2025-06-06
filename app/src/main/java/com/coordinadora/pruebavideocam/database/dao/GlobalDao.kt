package com.coordinadora.pruebavideocam.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.coordinadora.pruebavideocam.database.entity.Globals

@Dao
interface GlobalDao {
    @Query("SELECT * FROM Globals")
    fun getAll(): List<Globals>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg global: Globals)

    @Delete
    fun delete(global: Globals)

    @Update
    fun update(global: Globals)

    @Query("SELECT value FROM Globals WHERE keyId = :key LIMIT 1")
    fun getValueRaw(key: String?): String?

    fun getValue(key: String?) : String{
        return getValueRaw(key) ?: ""
    }
}
