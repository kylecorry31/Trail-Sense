package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_id")
    val packageId: String,
    @ColumnInfo(name = "signature")
    val signature: String
)
