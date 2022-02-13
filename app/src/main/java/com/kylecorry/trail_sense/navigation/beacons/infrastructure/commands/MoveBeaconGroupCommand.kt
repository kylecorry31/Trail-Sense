package com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MoveBeaconGroupCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val service: BeaconService,
    private val onMoved: () -> Unit
) {

    fun execute(group: BeaconGroup) {
        CustomUiUtils.pickBeaconGroup(
            context,
            null,
            context.getString(R.string.move)
        ) { cancelled, groupId ->
            if (cancelled) return@pickBeaconGroup
            scope.launch {
                val groupName = onIO {
                    service.add(group.copy(parentId = groupId))
                    if (groupId == null) {
                        context.getString(R.string.no_group)
                    } else {
                        service.getGroup(groupId)?.name ?: ""
                    }
                }

                onMain {
                    Alerts.toast(
                        context,
                        context.getString(R.string.moved_to, groupName)
                    )
                    onMoved()
                }
            }
        }
    }

}