package com.kylecorry.trail_sense.tools.backtrack.ui.commands

import android.content.Context
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.IBeaconRepo
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.beacons.BeaconOwner
import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.paths.PathPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class NavigateToPointCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val navController: NavController,
    private val beaconRepo: IBeaconRepo = BeaconRepo.getInstance(context)
) : IPathPointCommand {

    override fun execute(path: Path, point: PathPoint) {
        lifecycleScope.launch {
            val newTempId = withContext(Dispatchers.IO) {
                val tempBeaconId =
                    beaconRepo.getTemporaryBeacon(BeaconOwner.Path)?.id ?: 0L
                val beacon = Beacon(
                    tempBeaconId,
                    path.name ?: context.getString(R.string.waypoint),
                    point.coordinate,
                    visible = false,
                    elevation = point.elevation,
                    temporary = true,
                    color = path.style.color,
                    owner = BeaconOwner.Path
                )
                beaconRepo.addBeacon(BeaconEntity.from(beacon))
            }

            withContext(Dispatchers.Main) {
                navController.navigate(
                    R.id.action_navigation,
                    bundleOf("destination" to newTempId)
                )
            }
        }
    }
}