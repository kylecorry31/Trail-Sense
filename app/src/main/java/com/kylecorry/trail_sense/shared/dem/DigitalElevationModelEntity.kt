package com.kylecorry.trail_sense.shared.dem

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.abs

@Entity(tableName = "dem")
data class DigitalElevationModelEntity(
    @ColumnInfo(name = "resolution") val resolution: Int,
    @ColumnInfo(name = "compression_method") val compressionMethod: String,
    @ColumnInfo(name = "version") val version: String,
    @ColumnInfo(name = "filename") val filename: String,
    @ColumnInfo(name = "width") val width: Int,
    @ColumnInfo(name = "height") val height: Int,
    @ColumnInfo(name = "a") val a: Double,
    @ColumnInfo(name = "b") val b: Double,
    @ColumnInfo(name = "north") val north: Double,
    @ColumnInfo(name = "south") val south: Double,
    @ColumnInfo(name = "east") val east: Double,
    @ColumnInfo(name = "west") val west: Double,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    val trueResolution: Double
        get() {
            val degrees = abs(west - east)
            return if (degrees == 0.0) {
                0.0
            } else {
                degrees / width * 3600
            }
        }
}