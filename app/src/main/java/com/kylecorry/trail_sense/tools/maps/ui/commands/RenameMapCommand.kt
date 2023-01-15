package com.kylecorry.trail_sense.tools.maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.maps.domain.IMap
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapGroup
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapService

class RenameMapCommand(private val context: Context, private val service: MapService) :
    CoroutineCommand<IMap> {
    override suspend fun execute(value: IMap) {
        val newName = CoroutinePickers.text(context, context.getString(R.string.name), default = value.name) ?: return
        if (value is MapGroup) {
            service.add(value.copy(name = newName))
        } else if (value is Map) {
            service.add(value.copy(name = newName))
        }
    }
}