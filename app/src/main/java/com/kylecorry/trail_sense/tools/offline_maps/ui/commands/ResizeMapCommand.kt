package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapResolution

class ResizeMapCommand(
    private val context: Context,
    private val loadingIndicator: ILoadingIndicator
) : CoroutineCommand<PhotoMap> {
    private val service = getAppService<OfflineMapService>()

    override suspend fun execute(value: PhotoMap) {
        val resolution = onMain {
            CoroutinePickers.item(
                context,
                context.getString(R.string.change_resolution),
                listOf(
                    context.getString(R.string.low),
                    context.getString(R.string.moderate),
                    context.getString(R.string.high)
                )
            )
        } ?: return

        onMain {
            loadingIndicator.show()
        }

        onIO {
            val mapResolution = when (resolution) {
                0 -> PhotoMapResolution.Low
                1 -> PhotoMapResolution.Medium
                else -> PhotoMapResolution.High
            }
            service.reduce(value, mapResolution)
        }

        onMain {
            loadingIndicator.hide()
        }

    }
}
