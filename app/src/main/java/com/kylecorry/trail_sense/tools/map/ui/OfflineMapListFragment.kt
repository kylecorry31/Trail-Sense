package com.kylecorry.trail_sense.tools.map.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useFlow
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useBackPressedCallback
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.lists.bind
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.FloatingActionButtonMenu
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.map.domain.IOfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileGroup
import com.kylecorry.trail_sense.tools.map.infrastructure.OfflineMapFileGroupLoader
import com.kylecorry.trail_sense.tools.map.infrastructure.OfflineMapFileService
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo
import com.kylecorry.trail_sense.tools.map.ui.commands.CreateOfflineMapCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.CreateOfflineMapFileGroupCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.DeleteOfflineMapCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.EditOfflineMapAttributionCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.MoveOfflineMapFileCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.OfflineMapCleanupCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.RenameOfflineMapCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.ShowOfflineMapsDisclaimerCommand
import com.kylecorry.trail_sense.tools.map.ui.commands.ToggleOfflineMapVisibilityCommand

class OfflineMapListFragment : TrailSenseReactiveFragment(R.layout.fragment_offline_map_list) {

    override fun update() {
        val context = useAndroidContext()
        val repo = useService<OfflineMapFileRepo>()
        val service = useMemo { getAppService<OfflineMapFileService>() }
        val loader = useMemo(service) { OfflineMapFileGroupLoader(service.loader) }
        val listView = useView<AndromedaListView>(R.id.list)
        val emptyView = useView<View>(R.id.empty_text)
        val title = useView<Toolbar>(R.id.title)
        val search = useView<SearchView>(R.id.searchbox)
        val addButton = useView<FloatingActionButton>(R.id.add_btn)
        val addMenu = useView<FloatingActionButtonMenu>(R.id.add_menu)
        val overlay = useView<View>(R.id.overlay_mask)

        val manager = useMemo(loader) {
            GroupListManager(
                lifecycleScope,
                loader,
                null,
                this::sortFiles
            )
        }

        val listItemMapper = useMemo(context, manager, service) {
            IOfflineMapFileListItemMapper(
                requireContext(),
                { map, action ->
                    handleListItemAction(map, action, manager)
                },
                { group, action ->
                    handleGroupAction(group, action, service, manager)
                }
            )
        }

        val mapFlow = useMemo(repo) {
            repo.getAll()
        }
        val maps = useFlow(mapFlow)

        useEffect(listView, emptyView) {
            listView.emptyView = emptyView
        }

        useEffect(manager, search) {
            manager.bind(search)
        }

        useEffect(manager, listView, title, listItemMapper) {
            manager.bind(listView, title.title, listItemMapper) {
                (it as OfflineMapFileGroup?)?.name ?: getString(R.string.offline_maps)
            }
            manager.refresh()
        }

        useEffect(maps) {
            if (maps != null) {
                manager.refresh()
            }
        }

        useEffect(lifecycleHookTrigger.onResume()) {
            manager.refresh()
        }

        useBackPressedCallback(manager) {
            manager.up()
        }

        useEffect(addButton, addMenu, overlay) {
            addMenu.setOverlay(overlay)
            addMenu.fab = addButton
            addMenu.hideOnMenuOptionSelected = true
            addMenu.setOnMenuItemClickListener { menuItem ->
                handleCreateMenuAction(menuItem.itemId, manager)
                true
            }
        }

        useEffect(context) {
            ShowOfflineMapsDisclaimerCommand(context).execute()
        }

        useBackgroundEffect(lifecycleHookTrigger.onResume()) {
            OfflineMapCleanupCommand().execute()
        }
    }

    private fun sortFiles(files: List<IOfflineMapFile>): List<IOfflineMapFile> {
        return files.sortedWith(
            compareBy<IOfflineMapFile> { !it.isGroup }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun handleListItemAction(
        map: OfflineMapFile,
        action: OfflineMapFileAction,
        manager: GroupListManager<IOfflineMapFile>
    ) {
        when (action) {
            OfflineMapFileAction.View -> view(map)
            OfflineMapFileAction.Rename -> rename(map, manager)
            OfflineMapFileAction.EditAttribution -> editAttribution(map, manager)
            OfflineMapFileAction.Delete -> delete(map, manager)
            OfflineMapFileAction.Move -> move(map, manager)
            OfflineMapFileAction.ToggleVisibility -> toggleVisible(map, manager)
        }
    }

    private fun handleGroupAction(
        group: OfflineMapFileGroup,
        action: OfflineMapFileGroupAction,
        service: OfflineMapFileService,
        manager: GroupListManager<IOfflineMapFile>
    ) {
        when (action) {
            OfflineMapFileGroupAction.View -> manager.open(group.id)
            OfflineMapFileGroupAction.Rename -> rename(group, manager)
            OfflineMapFileGroupAction.Delete -> delete(group, manager)
            OfflineMapFileGroupAction.Move -> move(group, manager)
            OfflineMapFileGroupAction.ShowAll -> setGroupVisibility(group, true, service, manager)
            OfflineMapFileGroupAction.HideAll -> setGroupVisibility(group, false, service, manager)
        }
    }

    private fun view(map: OfflineMapFile) {
        findNavController().navigateWithAnimation(
            R.id.offlineMapViewFragment,
            Bundle().apply {
                putLong("offline_map_file_id", map.id)
            }
        )
    }

    private fun rename(
        map: IOfflineMapFile,
        manager: GroupListManager<IOfflineMapFile>
    ) {
        inBackground {
            RenameOfflineMapCommand(requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun editAttribution(
        map: OfflineMapFile,
        manager: GroupListManager<IOfflineMapFile>
    ) {
        inBackground {
            EditOfflineMapAttributionCommand(requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun move(
        map: IOfflineMapFile,
        manager: GroupListManager<IOfflineMapFile>
    ) {
        inBackground {
            MoveOfflineMapFileCommand(requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun toggleVisible(
        map: IOfflineMapFile,
        manager: GroupListManager<IOfflineMapFile>
    ) {
        inBackground {
            ToggleOfflineMapVisibilityCommand().execute(map)
            manager.refresh()
        }
    }

    private fun setGroupVisibility(
        group: OfflineMapFileGroup,
        visible: Boolean,
        service: OfflineMapFileService,
        manager: GroupListManager<IOfflineMapFile>
    ) {
        inBackground {
            Alerts.withLoading(requireContext(), getString(R.string.loading)) {
                val maps = onIO {
                    service.loader.getChildren(group.id, null)
                        .filterIsInstance<OfflineMapFile>()
                }

                onIO {
                    maps
                        .asSequence()
                        .filter { it.visible != visible }
                        .forEach { service.add(it.copy(visible = visible)) }
                }

                onMain {
                    manager.refresh()
                }
            }
        }
    }

    private fun delete(
        map: IOfflineMapFile,
        manager: GroupListManager<IOfflineMapFile>
    ) {
        inBackground {
            DeleteOfflineMapCommand(requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun handleCreateMenuAction(
        itemId: Int,
        manager: GroupListManager<IOfflineMapFile>
    ) {
        when (itemId) {
            R.id.action_create_map_group -> inBackground {
                CreateOfflineMapFileGroupCommand(requireContext()).execute(manager.root?.id)
                manager.refresh()
            }

            R.id.action_import_map_file -> inBackground(BackgroundMinimumState.Created) {
                CreateOfflineMapCommand(
                    this@OfflineMapListFragment,
                    requireContext(),
                    manager.root?.id
                ).execute()
                manager.refresh()
            }
        }
    }
}
