package com.kylecorry.trail_sense.tools.map.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentOfflineMapListBinding
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.infrastructure.MapFileTypeUtils
import com.kylecorry.trail_sense.tools.map.infrastructure.bounds.OfflineMapBoundsCalculatorFactory
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo
import java.time.Instant
import java.util.UUID

class OfflineMapListFragment : BoundFragment<FragmentOfflineMapListBinding>() {

    private val repo = getAppService<OfflineMapFileRepo>()
    private val uriPicker by lazy { IntentUriPicker(this, requireContext()) }
    private val files = getAppService<FileSubsystem>()
    private val formatter = getAppService<FormatService>()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentOfflineMapListBinding {
        return FragmentOfflineMapListBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.emptyView = binding.emptyText
        binding.addBtn.setOnClickListener {
            addOfflineMap()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        inBackground {
            val maps = repo.getAllSync().sortedBy { it.name }
            binding.list.setItems(maps.map { it.toListItem() })
        }
    }

    private fun OfflineMapFile.toListItem(): ListItem {
        return ListItem(
            id,
            name,
            icon = ResourceListIcon(
                R.drawable.ic_file,
                Resources.androidTextColorSecondary(requireContext())
            ),
            subtitle = formatter.join(
                formatter.formatOfflineMapFileTypeName(type),
                formatter.formatFileSize(sizeBytes),
                separator = FormatService.Separator.Dot
            ),
            trailingIcon = ResourceListIcon(
                if (visible) {
                    R.drawable.ic_visible
                } else {
                    R.drawable.ic_not_visible
                },
                Resources.androidTextColorSecondary(requireContext()),
                onClick = {
                    toggleVisible(this)
                }
            )
        )
    }

    private fun toggleVisible(map: OfflineMapFile) {
        inBackground {
            repo.add(map.copy(visible = !map.visible))
            onMain {
                refresh()
            }
        }
    }

    private fun addOfflineMap() {
        inBackground(BackgroundMinimumState.Created) {
            val uri = uriPicker.open(listOf("*/*")) ?: return@inBackground

            val name = CoroutinePickers.text(
                requireContext(),
                getString(R.string.name),
                hint = getString(R.string.name),
                default = files.getFileName(uri, withExtension = false, fallbackToPathName = false)
            )?.trim()?.takeIf { it.isNotBlank() } ?: return@inBackground

            if (MapFileTypeUtils.getType(uri) == null) {
                toast(getString(R.string.unsupported_offline_map_file))
                return@inBackground
            }

            Alerts.withLoading(requireContext(), getString(R.string.importing_map)) {
                val saved = save(uri, name)
                if (!saved) {
                    toast(getString(R.string.offline_map_import_failed))
                }
            }

            onMain {
                refresh()
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
            visible = true
        )
        repo.add(mapFile)
        return true
    }


    companion object {
        private const val OFFLINE_MAPS_DIRECTORY = "offline_maps"
    }
}
