package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useFlow
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.lists.bind
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.FloatingActionButtonMenu
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.OfflineMapFileGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.OfflineMapFileService
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands.CreateOfflineMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands.CreateOfflineMapFileGroupCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands.DeleteOfflineMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands.EditOfflineMapAttributionCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands.MoveOfflineMapFileCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands.OfflineMapCleanupCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands.RenameOfflineMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands.ShowOfflineMapsDisclaimerCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands.ToggleOfflineMapVisibilityCommand

class OfflineMapListFragment : TrailSenseReactiveFragment(R.layout.fragment_offline_map_list) {

    private var backPressedCallback: OnBackPressedCallback? = null

    override fun update() {
        val context = useAndroidContext()
        val repo = useService<MapRepo>()
        val service = useService<OfflineMapFileService>()
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
            repo.getVectorMapFlow()
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
                (it as MapGroup?)?.name ?: getString(R.string.vector_maps)
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

        useEffectWithCleanup(manager) {
            val navController = findNavController()
            val listener = onBackPressed {
                if (!manager.up()) {
                    remove()
                    navController.popBackStack()
                }
            }
            backPressedCallback = listener
            updateBackPressedCallback()

            return@useEffectWithCleanup {
                listener.remove()
                if (backPressedCallback == listener) {
                    backPressedCallback = null
                }
            }
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

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        updateBackPressedCallback()
    }

    override fun onResume() {
        super.onResume()
        updateBackPressedCallback()
    }

    private fun updateBackPressedCallback() {
        backPressedCallback?.isEnabled = !isHidden
    }

    private fun sortFiles(files: List<IMap>): List<IMap> {
        return files.sortedWith(
            compareBy<IMap> { !it.isGroup }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun handleListItemAction(
        map: OfflineMapFile,
        action: OfflineMapFileAction,
        manager: GroupListManager<IMap>
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
        group: MapGroup,
        action: OfflineMapFileGroupAction,
        service: OfflineMapFileService,
        manager: GroupListManager<IMap>
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
        map: IMap,
        manager: GroupListManager<IMap>
    ) {
        inBackground {
            RenameOfflineMapCommand(requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun editAttribution(
        map: OfflineMapFile,
        manager: GroupListManager<IMap>
    ) {
        inBackground {
            EditOfflineMapAttributionCommand(requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun move(
        map: IMap,
        manager: GroupListManager<IMap>
    ) {
        inBackground {
            MoveOfflineMapFileCommand(requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun toggleVisible(
        map: IMap,
        manager: GroupListManager<IMap>
    ) {
        inBackground {
            ToggleOfflineMapVisibilityCommand().execute(map)
            manager.refresh()
        }
    }

    private fun setGroupVisibility(
        group: MapGroup,
        visible: Boolean,
        service: OfflineMapFileService,
        manager: GroupListManager<IMap>
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
        map: IMap,
        manager: GroupListManager<IMap>
    ) {
        inBackground {
            DeleteOfflineMapCommand(requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun handleCreateMenuAction(
        itemId: Int,
        manager: GroupListManager<IMap>
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
