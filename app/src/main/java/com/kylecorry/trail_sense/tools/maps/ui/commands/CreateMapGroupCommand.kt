package com.kylecorry.trail_sense.tools.maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.tools.maps.domain.MapGroup
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapService

class CreateMapGroupCommand(
    private val context: Context,
    private val mapService: MapService
) : CoroutineCommand<Long?> {
    override suspend fun execute(value: Long?) {
        val name = onMain {
            CoroutinePickers.text(context, context.getString(R.string.name))
        } ?: return
        mapService.add(MapGroup(0, name, value))
    }
}