package com.kylecorry.trail_sense.shared.tiles

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.sense.Sensors

class TileManager {

    fun setTilesEnabled(context: Context, enabled: Boolean) {
        val hasBarometer = Sensors.hasBarometer(context)
        val hasPedometer = Sensors.hasSensor(context, Sensor.TYPE_STEP_COUNTER)
        Package.setComponentEnabled(
            context,
            "com.kylecorry.trail_sense.tools.paths.tiles.BacktrackTile",
            enabled
        )

        val pedometerTileEnabled = enabled && hasPedometer
        Package.setComponentEnabled(
            context,
            "com.kylecorry.trail_sense.tools.pedometer.tiles.PedometerTile",
            pedometerTileEnabled
        )

        val weatherTileEnabled = enabled && hasBarometer
        Package.setComponentEnabled(
            context,
            "com.kylecorry.trail_sense.tools.weather.tiles.WeatherMonitorTile",
            weatherTileEnabled
        )
    }

}