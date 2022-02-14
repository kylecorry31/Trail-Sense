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

class RenameBeaconGroupCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val service: BeaconService,
    private val onRenamed: () -> Unit
) {

    fun execute(group: BeaconGroup) {
        Pickers.text(
            context,
            context.getString(R.string.group),
            null,
            group.name,
            context.getString(R.string.name)
        ) {
            if (it != null) {
                scope.launch {
                    onIO {
                        service.add(group.copy(name = it))
                    }
                    onMain {
                        onRenamed()
                    }
                }
            }
        }
    }

}