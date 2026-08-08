package com.kylecorry.trail_sense.tools.offline_maps.ui.trail_maps

import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.TrailMapExportService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.commands.ExportTrailMapCommand

class FragmentTrailMapExportService(private val fragment: AndromedaFragment) {
    private val uriPicker = IntentUriPicker(fragment, fragment.requireContext())
    private val loading = AlertLoadingIndicator(
        fragment.requireContext(),
        fragment.getString(R.string.exporting_map)
    )
    private val exporter = TrailMapExportService(
        fragment.requireContext(),
        uriPicker
    )

    private val command = ExportTrailMapCommand(exporter, loading)

    fun export(map: TrailMap) {
        fragment.inBackground(BackgroundMinimumState.Created) {
            val success = command.execute(map)
            if (success) {
                fragment.toast(fragment.getString(R.string.map_exported))
            } else {
                fragment.toast(fragment.getString(R.string.map_export_error))
            }
        }
    }
}
