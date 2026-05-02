package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plugin_registrations")
data class PluginRegistrationEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_id")
    val packageId: String,
    @ColumnInfo(name = "version_code")
    val versionCode: Long,
    @ColumnInfo(name = "payload")
    val payload: ByteArray
)
