package com.kylecorry.trail_sense.navigation.infrastructure.export

import android.content.Context
import com.kylecorry.trail_sense.BuildConfig
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.infrastructure.json.JsonConvert
import com.kylecorry.trailsensecore.infrastructure.persistence.LocalFileService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import java.time.Instant

class BeaconExporter(private val context: Context) {

    private val localFileService by lazy { LocalFileService(context) }

    fun export(beacons: List<Beacon>, groups: List<BeaconGroup>) {
        val exportDto = BeaconExport(beacons.map { it.copy(visible = true, temporary = false) }, groups)
        val json = JsonConvert.toJson(exportDto)
        val exportFile = "$EXPORT_DIRECTORY/beacons-${Instant.now().epochSecond}.trailsense"
        localFileService.delete("exports", true)
        localFileService.write(exportFile, json, false)
        val uri = localFileService.getUri(exportFile, AUTHORITY, false)
        IntentUtils.file(
            context,
            uri,
            AUTHORITY,
            context.getString(R.string.export_beacons),
            "text/plain"
        )
        // TODO: Delete file after sending it if possible
    }

    fun export(beacon: Beacon) {
        export(listOf(beacon.copy(beaconGroupId = null, visible = true, temporary = false)), listOf())
    }

    companion object {
        private const val EXPORT_DIRECTORY = "exports"
        private const val AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"
    }

}