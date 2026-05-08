package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

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
import com.kylecorry.trail_sense.databinding.FragmentPhotoMapListBinding
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.lists.bind
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort.ClosestMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort.MapSortMethod
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort.MostRecentMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort.NameMapSortStrategy
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.MapCleanupCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.PrintMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.create.CreateBlankMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.create.CreateMapFromCameraCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.create.CreateMapFromFileCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.create.CreateMapFromUriCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.create.ICreateMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce.HighQualityMapReducer
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.commands.CreateMapGroupCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.commands.DeleteMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.commands.MoveMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.commands.RenameMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.commands.ResizeMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.commands.ShowMapsDisclaimerCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.commands.ToggleVisibilityMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.mappers.IMapMapper
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.mappers.MapAction
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.mappers.MapGroupAction

class PhotoMapListFragment : BoundFragment<FragmentPhotoMapListBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val mapService by lazy { MapService.getInstance(requireContext()) }
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
    ): FragmentPhotoMapListBinding {
        return FragmentPhotoMapListBinding.inflate(layoutInflater, container, false)
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
            this::onMapAction,
            this::onMapGroupAction
        )

        val mapIntentUri =
            BundleCompat.getParcelable(arguments ?: Bundle(), "map_intent_uri", Uri::class.java)
        arguments?.remove("map_intent_uri")
        if (mapIntentUri != null) {
            createMap(
                CreateMapFromUriCommand(
                    requireContext(),
                    mapRepo,
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
            (it as MapGroup?)?.name ?: getString(R.string.photo_maps)
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
                val maps = onIO {
                    mapService.loader.getChildren(group.id, null).filterIsInstance<PhotoMap>()
                }

                onIO {
                    maps
                        .asSequence()
                        .filter { it.visible != visible }
                        .forEach { mapService.add(it.copy(visible = visible)) }
                }

                onMain {
                    manager.refresh()
                }
            }
        }
    }

    private fun onMapAction(map: PhotoMap, action: MapAction) {
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
        if (map is MapGroup) {
            manager.open(map.id)
        } else {
            findNavController().navigate(
                R.id.action_mapList_to_maps,
                Bundle().apply {
                    putLong("mapId", map.id)
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
                            mapRepo,
                            mapImportingIndicator
                        )
                    )
                }

                R.id.action_import_map_camera -> {
                    createMap(
                        CreateMapFromCameraCommand(
                            this,
                            mapRepo,
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

            val map = command.execute()?.copy(parentId = manager.root?.id)

            if (map == null) {
                toast(getString(R.string.error_importing_map))
                binding.addBtn.isEnabled = true
                return@inBackground
            }

            if (map.parentId != null) {
                onIO {
                    mapRepo.addMap(map)
                }
            }

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

            binding.addBtn.isEnabled = true
            manager.refresh(true)
            findNavController().navigate(
                R.id.action_mapList_to_maps,
                Bundle().apply {
                    putLong("mapId", map.id)
                }
            )

        }
    }


}
