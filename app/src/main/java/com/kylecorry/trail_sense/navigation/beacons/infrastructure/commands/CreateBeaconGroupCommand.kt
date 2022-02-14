package com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CreateBeaconGroupCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val service: BeaconService,
    private val onCreated: () -> Unit
) {

    fun execute(parent: Long?) {
        Pickers.text(
            context,
            context.getString(R.string.group),
            null,
            null,
            context.getString(R.string.name)
        ) {
            if (it != null) {
                scope.launch {
                    onIO {
                        service.add(BeaconGroup(0, it, parent))
                    }

                    onMain {
                        onCreated()
                    }
                }
            }
        }
    }

}