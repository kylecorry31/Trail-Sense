package com.kylecorry.trail_sense.tools.maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.tools.maps.domain.IMap
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapService

class DeleteMapCommand(
    private val context: Context,
    private val mapService: MapService
) : CoroutineCommand<IMap> {
    override suspend fun execute(value: IMap) {
        val shouldDelete = onMain {
            !CoroutineAlerts.dialog(
                context,
                context.getString(R.string.delete),
                if (value is PhotoMap) value.name else context.getString(
                    R.string.delete_map_group_message,
                    value.name
                )
            )
        }

        if (!shouldDelete) {
            return
        }

        mapService.delete(value)
    }
}