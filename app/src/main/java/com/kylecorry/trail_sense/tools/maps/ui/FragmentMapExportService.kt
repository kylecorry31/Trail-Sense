package com.kylecorry.trail_sense.tools.maps.ui

import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.io.ExternalUriService
import com.kylecorry.trail_sense.shared.io.FragmentUriPicker
import com.kylecorry.trail_sense.shared.io.MapExportService
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.commands.ExportMapCommand
import kotlinx.coroutines.launch

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

    fun export(map: Map) {
        fragment.lifecycleScope.launch {
            command.execute(map)
            fragment.toast(fragment.getString(R.string.map_exported))
        }
    }

}