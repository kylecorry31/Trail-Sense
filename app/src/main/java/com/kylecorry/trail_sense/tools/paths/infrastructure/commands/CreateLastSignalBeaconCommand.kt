package com.kylecorry.trail_sense.tools.paths.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

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
                        CellNetwork.entries
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