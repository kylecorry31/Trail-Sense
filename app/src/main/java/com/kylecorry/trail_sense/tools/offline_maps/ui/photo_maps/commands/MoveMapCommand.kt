package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapPickers
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapService

class MoveMapCommand(private val context: Context, private val service: MapService) :
    CoroutineCommand<IMap> {
    override suspend fun execute(value: IMap) {
        val results = MapPickers.pickGroup(
            context,
            null,
            context.getString(R.string.move),
            initialGroup = value.parentId
        ) {
            it.filter {
                if (value is MapGroup) {
                    it.id != value.id
                } else {
                    true
                }
            }
        }

        if (results.first) {
            return
        }

        if (value is MapGroup) {
            service.add(value.copy(parentId = results.second?.id))
        } else if (value is PhotoMap) {
            service.add(value.copy(parentId = results.second?.id))
        }

        val groupName = results.second?.name ?: context.getString(R.string.no_group)

        onMain {
            Alerts.toast(
                context,
                context.getString(R.string.moved_to, groupName)
            )
        }
    }
}
