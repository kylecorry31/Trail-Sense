package com.kylecorry.trail_sense.tools.maps.ui

import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.io.ExternalUriService
import com.kylecorry.trail_sense.shared.io.FragmentUriPicker
import com.kylecorry.trail_sense.shared.io.MapExportService
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.commands.ExportMapCommand

class FragmentMapExportService(private val fragment: AndromedaFragment) {
    private val uriPicker = FragmentUriPicker(fragment)
    private val loading = AlertLoadingIndicator(
        fragment.requireContext(),
        fragment.getString(R.string.exporting_map)
    )
    private val exporter = MapExportService(
        fragment.requireContext(),
        uriPicker,
        ExternalUriService(fragment.requireContext())
    )

    private val command = ExportMapCommand(exporter, loading)

    fun export(map: PhotoMap) {
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