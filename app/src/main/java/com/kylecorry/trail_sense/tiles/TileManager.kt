package com.kylecorry.trail_sense.tiles

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.sense.SensorChecker

class TileManager {

    fun setTilesEnabled(context: Context, enabled: Boolean) {
        val sensorChecker = SensorChecker(context)
        val hasBarometer = sensorChecker.hasBarometer()
        val hasPedometer = sensorChecker.hasSensor(Sensor.TYPE_STEP_COUNTER)
        Package.setComponentEnabled(
            context,
            "com.kylecorry.trail_sense.tiles.BacktrackTile",
            enabled
        )

        val pedometerTileEnabled = enabled && hasPedometer
        Package.setComponentEnabled(
            context,
            "com.kylecorry.trail_sense.tiles.PedometerTile",
            pedometerTileEnabled
        )

        val weatherTileEnabled = enabled && hasBarometer
        Package.setComponentEnabled(
            context,
            "com.kylecorry.trail_sense.tiles.WeatherMonitorTile",
            weatherTileEnabled
        )
    }

}