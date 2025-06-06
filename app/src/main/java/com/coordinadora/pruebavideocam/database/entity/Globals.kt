package com.coordinadora.pruebavideocam.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Globals {
    @PrimaryKey
    var keyId: String

    @ColumnInfo(name = "value")
    var value: String? = null


    constructor(keyId: String, value: String?) {
        this.keyId = keyId
        this.value = value
    }
}
