package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class RenameMapCommand(private val context: Context, private val service: MapService) :
    CoroutineCommand<IMap> {
    override suspend fun execute(value: IMap) {
        val newName =
            CoroutinePickers.text(context, context.getString(R.string.name), default = value.name)
                ?: return
        when (value) {
            is MapGroup -> service.add(value.copy(name = newName))
            is PhotoMap -> service.add(value.copy(name = newName))
            is OfflineMapFile -> service.add(value.copy(name = newName))
        }
    }
}
