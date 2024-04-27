package com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.commands.generic.ComposedCommand
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem

class AstronomyAlertCommand(private val context: Context) : CoroutineCommand {
    override suspend fun execute() = onDefault {
        val location = LocationSubsystem.getInstance(context).location

        if (location == Coordinate.zero) {
            return@onDefault
        }

        val command = ComposedCommand(
            LunarEclipseAlertCommand(context),
            SolarEclipseAlertCommand(context),
            MeteorShowerAlertCommand(context)
        )

        command.execute(location)
    }

    companion object {
        const val NOTIFICATION_CHANNEL = "astronomy_alerts"
        const val NOTIFICATION_GROUP_ASTRONOMY_ALERTS = "trail_sense_astronomy_alerts"
    }
}