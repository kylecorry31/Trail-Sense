package com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DeleteBeaconCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val service: BeaconService,
    private val onDeleted: () -> Unit
) {

    fun execute(beacon: Beacon) {
        Alerts.dialog(
            context,
            context.getString(R.string.delete),
            beacon.name
        ) { cancelled ->
            if (!cancelled) {
                scope.launch {
                    onIO {
                        service.delete(beacon)
                    }
                    onMain {
                        onDeleted()
                    }
                }
            }
        }
    }

}