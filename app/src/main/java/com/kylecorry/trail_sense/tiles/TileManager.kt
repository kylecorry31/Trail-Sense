package com.kylecorry.trail_sense.tiles

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils

class TileManager {

    fun setTilesEnabled(context: Context, enabled: Boolean) {
        val sensorChecker = SensorChecker(context)
        val hasBarometer = sensorChecker.hasBarometer()
        val hasPedometer = sensorChecker.hasSensor(Sensor.TYPE_STEP_COUNTER)
        PackageUtils.setComponentEnabled(
            context,
            "com.kylecorry.trail_sense.tiles.BacktrackTile",
            enabled
        )

        val pedometerTileEnabled = enabled && hasPedometer
        PackageUtils.setComponentEnabled(
            context,
            "com.kylecorry.trail_sense.tiles.PedometerTile",
            pedometerTileEnabled
        )

        val weatherTileEnabled = enabled && hasBarometer
        PackageUtils.setComponentEnabled(
            context,
            "com.kylecorry.trail_sense.tiles.WeatherMonitorTile",
            weatherTileEnabled
        )
    }

}