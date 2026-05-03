package com.kylecorry.trail_sense.tools.map.ui

import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useFlow
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo
import com.kylecorry.trail_sense.tools.map.ui.commands.CreateOfflineMapCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.DeleteOfflineMapCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.RenameOfflineMapCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.ToggleOfflineMapVisibilityCommand

class OfflineMapListFragment : TrailSenseReactiveFragment(R.layout.fragment_offline_map_list) {

    override fun update() {
        val listView = useView<AndromedaListView>(R.id.list)
        val emptyView = useView<View>(R.id.empty_text)
        val addButton = useView<FloatingActionButton>(R.id.add_btn)

        val context = useAndroidContext()
        val repo = useService<OfflineMapFileRepo>()
        val listItemMapper = useMemo(context) {
            OfflineMapFileListItemMapper(requireContext(), ::handleListItemAction)
        }
        val mapFlow = useMemo(repo) {
            repo.getAll()
        }
        val maps = useFlow(mapFlow)

        useEffect(listView, listItemMapper, maps) {
            maps?.let { listView.setItems(it.sortedBy { map -> map.name }, listItemMapper) }
        }

        useEffect(listView, emptyView) {
            listView.emptyView = emptyView
        }

        useEffect(addButton) {
            addButton.setOnClickListener { addOfflineMap() }
        }

    }

    private fun handleListItemAction(map: OfflineMapFile, action: OfflineMapFileAction) {
        when (action) {
            OfflineMapFileAction.Rename -> rename(map)
            OfflineMapFileAction.Delete -> delete(map)
            OfflineMapFileAction.ToggleVisibility -> toggleVisible(map)
        }
    }

    private fun rename(map: OfflineMapFile) {
        inBackground {
            RenameOfflineMapCommand(requireContext()).execute(map)
        }
    }

    private fun toggleVisible(map: OfflineMapFile) {
        inBackground {
            ToggleOfflineMapVisibilityCommand().execute(map)
        }
    }

    private fun delete(map: OfflineMapFile) {
        inBackground {
            DeleteOfflineMapCommand(requireContext()).execute(map)
        }
    }

    private fun addOfflineMap() {
        inBackground(BackgroundMinimumState.Created) {
            CreateOfflineMapCommand(this@OfflineMapListFragment, requireContext()).execute()
        }
    }
}
