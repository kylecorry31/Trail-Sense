package com.kylecorry.trail_sense.tools.offline_maps.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.loading.AlertLoadingIndicator
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentOfflineMapListBinding
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.lists.bind
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.ClosestMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.MapSortMethod
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.MostRecentMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.NameMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateBlankMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateMapFromCameraCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateMapFromFileCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.CreateMapFromUriCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create.ICreateMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.MapCleanupCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.PrintMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce.HighQualityMapReducer
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.CreateMapGroupCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.DeleteMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.EditOfflineMapAttributionCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.MoveMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.RenameMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.ResizeMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.ShowMapsDisclaimerCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.ToggleVisibilityMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.IMapMapper
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.MapAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.MapGroupAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.mappers.VectorMapAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.FragmentMapExportService

class OfflineMapListFragment : BoundFragment<FragmentOfflineMapListBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val mapService by lazy { MapService.Companion.getInstance(requireContext()) }
    private val mapLoader by lazy { MapGroupLoader(mapService.loader) }
    private lateinit var manager: GroupListManager<IMap>
    private lateinit var mapper: IMapMapper

    private var sort = MapSortMethod.Closest

    private var lastRoot: IMap? = null
    private var backPressedCallback: OnBackPressedCallback? = null

    private val uriPicker by lazy { IntentUriPicker(this, requireContext()) }
    private val mapImportingIndicator by lazy {
        AlertLoadingIndicator(
            requireContext(),
            getString(R.string.importing_map)
        )
    }
    private val exportService by lazy { FragmentMapExportService(this) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentOfflineMapListBinding {
        return FragmentOfflineMapListBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manager = GroupListManager(
            lifecycleScope,
            mapLoader,
            lastRoot,
            this::sortMaps
        )

        mapper = IMapMapper(
            gps,
            requireContext(),
            this,
            this::onPhotoMapAction,
            this::onVectorMapAction,
            this::onMapGroupAction
        )

        val mapIntentUri =
            BundleCompat.getParcelable(arguments ?: Bundle(), "map_intent_uri", Uri::class.java)
        arguments?.remove("map_intent_uri")
        if (mapIntentUri != null) {
            createMap(
                CreateMapFromUriCommand(
                    requireContext(),
                    mapIntentUri,
                    mapImportingIndicator
                )
            )
        }

        ShowMapsDisclaimerCommand(this).execute()

        binding.mapList.emptyView = binding.mapEmptyText

        binding.mapListTitle.leftButton.setOnClickListener {
            UserGuideUtils.showGuide(this, R.raw.guide_tool_offline_maps)
        }

        sort = prefs.photoMaps.mapSort
        binding.mapListTitle.rightButton.setOnClickListener {
            Pickers.menu(
                it, listOf(
                    getString(R.string.sort_by, getSortString(sort))
                )
            ) { selected ->
                when (selected) {
                    0 -> changeSort()
                }
                true
            }
        }

        manager.bind(binding.searchbox)
        manager.bind(binding.mapList, binding.mapListTitle.title, mapper) {
            (it as MapGroup?)?.name ?: getString(R.string.offline_maps)
        }

        backPressedCallback = onBackPressed {
            if (!manager.up()) {
                remove()
                findNavController().navigateUp()
            }
        }
        updateBackPressedCallback()

        setupMapCreateMenu()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        updateBackPressedCallback()
    }

    override fun onResume() {
        super.onResume()
        updateBackPressedCallback()
        manager.refresh()
        inBackground {
            val mapsDeleted = MapCleanupCommand(requireContext()).execute()
            if (mapsDeleted) {
                manager.refresh()
            }
        }
    }

    private fun changeSort() {
        val sortOptions = MapSortMethod.values()
        Pickers.item(
            requireContext(),
            getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.photoMaps.mapSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.photoMaps.mapSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: MapSortMethod): String {
        return when (sortMethod) {
            MapSortMethod.MostRecent -> getString(R.string.most_recent)
            MapSortMethod.Closest -> getString(R.string.closest)
            MapSortMethod.Name -> getString(R.string.name)
        }
    }

    private fun onSortChanged() {
        manager.refresh(true)
    }

    private suspend fun sortMaps(maps: List<IMap>): List<IMap> = onDefault {
        val strategy = when (sort) {
            MapSortMethod.Closest -> ClosestMapSortStrategy(gps.location, mapService.loader)
            MapSortMethod.MostRecent -> MostRecentMapSortStrategy(mapService.loader)
            MapSortMethod.Name -> NameMapSortStrategy()
        }

        strategy.sort(maps)
    }

    private fun onMapGroupAction(group: MapGroup, action: MapGroupAction) {
        when (action) {
            MapGroupAction.View -> view(group)
            MapGroupAction.Delete -> delete(group)
            MapGroupAction.Rename -> rename(group)
            MapGroupAction.Move -> move(group)
            MapGroupAction.ShowAll -> setGroupVisibility(group, true)
            MapGroupAction.HideAll -> setGroupVisibility(group, false)
        }
    }

    private fun setGroupVisibility(group: MapGroup, visible: Boolean) {
        inBackground {
            Alerts.withLoading(requireContext(), getString(R.string.loading)) {
                val maps = mapService.loader.getChildren(group.id, null)

                // Photo Maps
                maps
                    .asSequence()
                    .filterIsInstance<PhotoMap>()
                    .filter { it.visible != visible }
                    .forEach { mapService.add(it.copy(visible = visible)) }

                // Vector Maps
                maps
                    .asSequence()
                    .filterIsInstance<VectorMap>()
                    .filter { it.visible != visible }
                    .forEach { mapService.add(it.copy(visible = visible)) }

                onMain {
                    manager.refresh()
                }
            }
        }
    }

    private fun onPhotoMapAction(map: PhotoMap, action: MapAction) {
        when (action) {
            MapAction.View -> view(map)
            MapAction.Delete -> delete(map)
            MapAction.Export -> export(map)
            MapAction.Print -> print(map)
            MapAction.Resize -> resize(map)
            MapAction.Rename -> rename(map)
            MapAction.Move -> move(map)
            MapAction.ToggleVisibility -> toggleVisibility(map)
        }
    }

    private fun onVectorMapAction(map: VectorMap, action: VectorMapAction) {
        when (action) {
            VectorMapAction.View -> view(map)
            VectorMapAction.Rename -> rename(map)
            VectorMapAction.EditAttribution -> editAttribution(map)
            VectorMapAction.Delete -> delete(map)
            VectorMapAction.Move -> move(map)
            VectorMapAction.ToggleVisibility -> toggleVisibility(map)
        }
    }

    private fun resize(map: PhotoMap) {
        inBackground {
            ResizeMapCommand(requireContext(), mapImportingIndicator).execute(map)
            manager.refresh()
        }
    }

    private fun print(map: PhotoMap) {
        inBackground(BackgroundMinimumState.Created) {
            PrintMapCommand(requireContext()).execute(map)
        }
    }

    private fun export(map: PhotoMap) {
        exportService.export(map)
    }

    private fun rename(map: IMap) {
        inBackground {
            RenameMapCommand(requireContext(), mapService).execute(map)
            manager.refresh()
        }
    }

    private fun editAttribution(map: VectorMap) {
        inBackground {
            EditOfflineMapAttributionCommand(requireContext()).execute(map)
            manager.refresh()
        }
    }

    private fun move(map: IMap) {
        inBackground {
            MoveMapCommand(requireContext(), mapService).execute(map)
            manager.refresh()
        }
    }

    private fun toggleVisibility(map: IMap) {
        inBackground {
            ToggleVisibilityMapCommand(mapService).execute(map)
            manager.refresh()
        }
    }

    private fun delete(map: IMap) {
        inBackground {
            DeleteMapCommand(requireContext(), mapService).execute(map)
            manager.refresh()
        }
    }

    private fun view(map: IMap) {
        when (map) {
            is MapGroup -> manager.open(map.id)
            is PhotoMap -> findNavController().navigate(
                R.id.action_mapList_to_maps,
                Bundle().apply {
                    putLong("mapId", map.id)
                }
            )

            is VectorMap -> findNavController().navigateWithAnimation(
                R.id.offlineMapViewFragment,
                Bundle().apply {
                    putLong("offline_map_file_id", map.id)
                }
            )
        }
    }

    private fun setupMapCreateMenu() {
        binding.addMenu.setOverlay(binding.overlayMask)
        binding.addMenu.fab = binding.addBtn
        binding.addMenu.hideOnMenuOptionSelected = true
        binding.addMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_import_map_file -> {
                    createMap(
                        CreateMapFromFileCommand(
                            requireContext(),
                            uriPicker,
                            mapImportingIndicator
                        )
                    )
                }

                R.id.action_import_map_camera -> {
                    createMap(
                        CreateMapFromCameraCommand(
                            this,
                            mapImportingIndicator
                        )
                    )
                }

                R.id.action_create_map_group -> {
                    createMapGroup()
                }

                R.id.action_create_blank_map -> {
                    createMap(
                        CreateBlankMapCommand(
                            requireContext(),
                            mapImportingIndicator
                        )
                    )
                }
            }
            true
        }
    }

    private fun createMapGroup() {
        inBackground {
            CreateMapGroupCommand(requireContext(), mapService).execute(manager.root?.id)
            manager.refresh()
        }
    }

    override fun onPause() {
        super.onPause()
        tryOrNothing {
            lastRoot = manager.root
        }
    }

    private fun updateBackPressedCallback() {
        backPressedCallback?.isEnabled = !isHidden
    }

    private fun createMap(command: ICreateMapCommand) {
        inBackground(BackgroundMinimumState.Created) {
            binding.addBtn.isEnabled = false

            val map = command.execute()?.let {
                when (it) {
                    is PhotoMap -> it.copy(parentId = manager.root?.id)
                    is VectorMap -> it.copy(parentId = manager.root?.id)
                    else -> null
                }
            }

            if (map == null) {
                toast(getString(R.string.error_importing_map))
                binding.addBtn.isEnabled = true
                return@inBackground
            }

            if (map.parentId != null) {
                onIO {
                    mapService.add(map)
                }
            }

            if (map is PhotoMap) {
                val isPdfMap = map.hasPdf(requireContext())
                if ((isPdfMap && prefs.photoMaps.autoReducePdfMaps) || (!isPdfMap && prefs.photoMaps.autoReducePhotoMaps)) {
                    mapImportingIndicator.show()
                    val reducer = HighQualityMapReducer(requireContext())
                    reducer.reduce(map)
                    mapImportingIndicator.hide()
                }

                if (map.calibration.calibrationPoints.isNotEmpty()) {
                    toast(getString(R.string.map_auto_calibrated))
                }
            }

            binding.addBtn.isEnabled = true
            manager.refresh(true)
            when (map) {
                is PhotoMap -> findNavController().navigate(
                    R.id.action_mapList_to_maps,
                    Bundle().apply {
                        putLong("mapId", map.id)
                    }
                )
            }

        }
    }


}
