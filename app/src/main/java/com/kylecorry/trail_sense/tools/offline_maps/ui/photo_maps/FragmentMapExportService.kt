package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.io.ExternalUriService
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapExportService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.ExportMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.TrailMapExportService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.commands.ExportTrailMapCommand

class FragmentMapExportService(private val fragment: AndromedaFragment) {
    private val uriPicker = IntentUriPicker(fragment, fragment.requireContext())
    private val loading = AlertLoadingIndicator(
        fragment.requireContext(),
        fragment.getString(R.string.exporting_map)
    )
    private val photoMapExporter = MapExportService(
        fragment.requireContext(),
        uriPicker,
        ExternalUriService(fragment.requireContext())
    )
    private val trailMapExporter = TrailMapExportService(
        fragment.requireContext(),
        uriPicker
    )

    private val photoMapCommand = ExportMapCommand(photoMapExporter, loading)
    private val trailMapCommand = ExportTrailMapCommand(trailMapExporter, loading)

    fun export(map: OfflineMap) {
        fragment.inBackground(BackgroundMinimumState.Created) {
            val success = when (map) {
                is PhotoMap -> photoMapCommand.execute(map)
                is TrailMap -> trailMapCommand.execute(map)
                else -> false
            }
            if (success) {
                fragment.toast(fragment.getString(R.string.map_exported))
            } else {
                fragment.toast(fragment.getString(R.string.map_export_error))
            }
        }
    }
}
