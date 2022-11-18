package com.kylecorry.trail_sense.astronomy.infrastructure.commands

import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.commands.generic.ComposedCommand
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.readAll
import java.time.Duration

class AstronomyAlertCommand(private val context: Context) : CoroutineCommand {
    override suspend fun execute() {
        val gps = SensorService(context).getGPS(true)

        readAll(listOf(gps), timeout = Duration.ofSeconds(10), forceStopOnCompletion = true)

        val location = gps.location

        if (location == Coordinate.zero) {
            return
        }

        val command = ComposedCommand(
            LunarEclipseAlertCommand(context),
            MeteorShowerAlertCommand(context)
        )

        command.execute(location)
    }
}