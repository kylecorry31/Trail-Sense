package com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.cell

import android.content.Context
import com.kylecorry.trail_sense.shared.commands.CoroutineValueCommand
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

class NavigateToNearestCellSignal(context: Context) : CoroutineValueCommand<Boolean> {

    private val lookup = CellSignalLookup(context)
    private val navigator = Navigator.getInstance(context)
    private val location = LocationSubsystem.getInstance(context)
    private val beacons = BeaconService(context)

    override suspend fun execute(): Boolean {
        val nearest = lookup.getNearestCellSignalBeacon(location.location)
        return if (nearest != null) {
            val id = beacons.add(nearest)
            navigator.navigateTo(id)
            true
        } else {
            false
        }
    }
}