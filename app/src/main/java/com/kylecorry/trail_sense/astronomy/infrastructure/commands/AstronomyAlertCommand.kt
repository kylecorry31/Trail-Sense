package com.kylecorry.trail_sense.astronomy.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.commands.generic.ComposedCommand
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem

class AstronomyAlertCommand(private val context: Context) : CoroutineCommand {
    override suspend fun execute() = onDefault {
        val location = getLocation()

        if (location == null) {
            return@onDefault
        }

        val command = createComposedCommand()

        command.execute(location)
    }

    private suspend fun getLocation(): Coordinate? {
        val locationSubsystem = LocationSubsystem.getInstance(context)
        return locationSubsystem.location
    }

    private fun createComposedCommand(): ComposedCommand {
        val lunarEclipseAlertCommand = LunarEclipseAlertCommand(context)
        val solarEclipseAlertCommand = SolarEclipseAlertCommand(context)
        val meteorShowerAlertCommand = MeteorShowerAlertCommand(context)

        return ComposedCommand(
            lunarEclipseAlertCommand,
            solarEclipseAlertCommand,
            meteorShowerAlertCommand
        )
    }
}
