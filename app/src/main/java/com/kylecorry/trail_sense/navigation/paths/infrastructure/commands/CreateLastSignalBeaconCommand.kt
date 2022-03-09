package com.kylecorry.trail_sense.navigation.paths.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.onIO

class CreateLastSignalBeaconCommand(private val context: Context) {

    private val beaconService = BeaconService(context)
    private val formatter = FormatService.getInstance(context)

    suspend fun execute(point: PathPoint) {
        point.cellSignal ?: return
        onIO {
            beaconService.add(Beacon(
                0L,
                context.getString(
                    R.string.last_signal_beacon_name,
                    formatter.formatCellNetwork(
                        CellNetwork.values()
                            .first { it.id == point.cellSignal.network.id }
                    ),
                    formatter.formatQuality(point.cellSignal.quality)
                ),
                point.coordinate,
                false,
                elevation = point.elevation,
                temporary = true,
                owner = BeaconOwner.CellSignal,
                color = AppColor.Orange.color
            ))
        }
    }
}