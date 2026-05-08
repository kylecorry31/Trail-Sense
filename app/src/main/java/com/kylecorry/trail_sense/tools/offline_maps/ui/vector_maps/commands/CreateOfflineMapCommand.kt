package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.MapFileTypeUtils
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.attribution.OfflineMapAttributionExtractorFactory
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.bounds.OfflineMapBoundsCalculatorFactory
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence.OfflineMapFileRepo
import java.time.Instant
import java.util.UUID

class CreateOfflineMapCommand(
    private val intentResolver: IntentResultRetriever,
    private val context: Context,
    private val parentId: Long? = null
) :
    CoroutineCommand {

    private val repo = getAppService<OfflineMapFileRepo>()
    private val uriPicker by lazy { IntentUriPicker(intentResolver, context) }
    private val files = getAppService<FileSubsystem>()

    override suspend fun execute() {
        val uri = uriPicker.open(listOf("*/*")) ?: return

        val name = CoroutinePickers.text(
            context,
            context.getString(R.string.name),
            hint = context.getString(R.string.name),
            default = files.getFileName(uri, withExtension = false, fallbackToPathName = false)
        )?.trim()?.takeIf { it.isNotBlank() } ?: return

        if (MapFileTypeUtils.getType(uri) == null) {
            Alerts.toast(context, context.getString(R.string.unsupported_offline_map_file))
            return
        }

        Alerts.withLoading(context, context.getString(R.string.importing_map)) {
            val saved = save(uri, name)
            if (!saved) {
                Alerts.toast(context, context.getString(R.string.offline_map_import_failed))
            }
        }
    }

    private suspend fun save(uri: Uri, name: String): Boolean {
        val type = MapFileTypeUtils.getType(uri) ?: return false
        val extension = MapFileTypeUtils.getExtension(type)
        val saved = files.copyToLocal(uri, OFFLINE_MAPS_DIRECTORY, "${UUID.randomUUID()}.$extension")
            ?: return false
        val mapFile = OfflineMapFile(
            0,
            name,
            type,
            files.getLocalPath(saved),
            saved.length(),
            Instant.now(),
            OfflineMapBoundsCalculatorFactory().getBoundsCalculator(type).getBounds(saved),
            OfflineMapAttributionExtractorFactory().getAttributionExtractor(type)
                .getAttribution(saved),
            visible = true,
            parentId = parentId
        )
        repo.add(mapFile)
        return true
    }

    companion object {
        private const val OFFLINE_MAPS_DIRECTORY = "offline_maps"
    }
}
