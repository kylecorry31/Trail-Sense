package com.kylecorry.trail_sense.tools.maps.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapListBinding
import com.kylecorry.trail_sense.databinding.ListItemMapBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.AlertLoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FragmentUriPicker
import com.kylecorry.trail_sense.shared.io.ImageThumbnailManager
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.maps.infrastructure.create.CreateMapFromFileCommand
import com.kylecorry.trail_sense.tools.maps.infrastructure.create.CreateMapFromUriCommand
import com.kylecorry.trail_sense.tools.maps.infrastructure.create.ICreateMapCommand
import com.kylecorry.trail_sense.tools.maps.infrastructure.reduce.HighQualityMapReducer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapListFragment : BoundFragment<FragmentMapListBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val cache by lazy { Preferences(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private lateinit var mapList: ListView<Map>
    private var maps: List<Map> = listOf()

    private var boundMap = mutableMapOf<Long, CoordinateBounds>()
    private var fileSizes = mutableMapOf<Long, Long>()

    private var mapName = ""

    private val uriPicker = FragmentUriPicker(this)
    private val mapImportingIndicator by lazy {
        AlertLoadingIndicator(
            requireContext(),
            getString(R.string.importing_map)
        )
    }

    private val thumbnailManager = ImageThumbnailManager()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapListBinding {
        return FragmentMapListBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapIntentUri: Uri? = arguments?.getParcelable("map_intent_uri")
        arguments?.remove("map_intent_uri")
        if (mapIntentUri != null) {
            Pickers.text(
                requireContext(),
                getString(R.string.create_map),
                getString(R.string.create_map_description),
                null,
                hint = getString(R.string.name)
            ) {
                if (it != null) {
                    mapName = it
                    createMap(
                        CreateMapFromUriCommand(
                            requireContext(),
                            mapRepo,
                            mapIntentUri,
                            mapImportingIndicator
                        )
                    )
                }
            }
        }

        if (cache.getBoolean("tool_maps_experimental_disclaimer_shown") != true) {
            Alerts.dialog(
                requireContext(),
                getString(R.string.experimental),
                "Offline Maps is an experimental feature, please only use this to test it out at this point. Feel free to share your feedback on this feature and note that there is still a lot to be done before this will be non-experimental.",
                okText = getString(R.string.tool_user_guide_title),
                cancelText = getString(android.R.string.ok)
            ) { cancelled ->
                cache.putBoolean("tool_maps_experimental_disclaimer_shown", true)
                if (!cancelled) {
                    UserGuideUtils.openGuide(this, R.raw.importing_maps)
                }
            }
        }

        binding.addBtn.setOnClickListener {
            Pickers.text(
                requireContext(),
                getString(R.string.create_map),
                getString(R.string.create_map_description),
                null,
                hint = getString(R.string.name)
            ) {
                if (it != null) {
                    mapName = it
                    createMap(
                        CreateMapFromFileCommand(
                            requireContext(),
                            uriPicker,
                            mapRepo,
                            mapImportingIndicator
                        )
                    )
                }
            }
        }

        mapList = ListView(binding.mapList, R.layout.list_item_map) { itemView: View, map: Map ->
            val mapItemBinding = ListItemMapBinding.bind(itemView)
            val onMap = boundMap[map.id]?.contains(gps.location) ?: false
            tryOrNothing {
                thumbnailManager.setImage(lifecycleScope, mapItemBinding.mapImg) {
                    try {
                        loadMapThumbnail(map)
                    } catch (e: Exception){
                        null
                    }
                }
            }
            mapItemBinding.fileSize.text = formatService.formatFileSize(fileSizes[map.id] ?: 0)
            mapItemBinding.name.text = map.name
            mapItemBinding.description.text = if (onMap) getString(R.string.on_map) else ""
            mapItemBinding.description.isVisible = onMap
            mapItemBinding.root.setOnClickListener {
                tryOrNothing {
                    findNavController().navigate(
                        R.id.action_mapList_to_maps,
                        bundleOf("mapId" to map.id)
                    )
                }
            }
            mapItemBinding.menuBtn.setOnClickListener {
                Pickers.menu(it, R.menu.map_list_item_menu) {
                    when (it) {
                        R.id.action_map_delete -> {
                            Alerts.dialog(
                                requireContext(),
                                getString(R.string.delete_map),
                                map.name
                            ) { cancelled ->
                                if (!cancelled) {
                                    lifecycleScope.launch {
                                        withContext(Dispatchers.IO) {
                                            mapRepo.deleteMap(map)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    true
                }
            }
        }

        mapList.addLineSeparator()

        mapRepo.getMaps().observe(viewLifecycleOwner) {
            maps = it
            // TODO: Show loading indicator
            maps.forEach {
                val file = LocalFiles.getFile(requireContext(), it.filename, false)

                val size = BitmapUtils.getBitmapSize(file.path)
                val width = if (it.rotation == 90 || it.rotation == 270) {
                    size.second
                } else {
                    size.first
                }
                val height = if (it.rotation == 90 || it.rotation == 270) {
                    size.first
                } else {
                    size.second
                }
                val bounds = it.boundary(width.toFloat(), height.toFloat())
                if (bounds != null) {
                    boundMap[it.id] = bounds
                }

                tryOrNothing {
                    fileSizes[it.id] = file.length()
                }
            }

            maps = maps.sortedBy {
                val bounds = boundMap[it.id] ?: return@sortedBy Float.MAX_VALUE
                val onMap = bounds.contains(gps.location)
                (if (onMap) 0f else 100000f) + gps.location.distanceTo(bounds.center)
            }

            mapList.setData(maps)
        }
    }

    private fun loadMapThumbnail(map: Map): Bitmap {
        val file = LocalFiles.getFile(requireContext(), map.filename, false)
        val size = Resources.dp(requireContext(), 48f).toInt()
        return BitmapUtils.decodeBitmapScaled(file.path, size, size)
    }

    override fun onPause() {
        super.onPause()
        thumbnailManager.clear()
    }

    private fun createMap(command: ICreateMapCommand) {
        runInBackground {
            binding.addBtn.isEnabled = false

            val map = command.execute()?.copy(name = mapName)

            if (map == null) {
                toast(getString(R.string.error_importing_map))
                binding.addBtn.isEnabled = true
                return@runInBackground
            }

            if (mapName.isNotBlank()) {
                onIO {
                    mapRepo.addMap(map)
                }
            }

            if (prefs.navigation.autoReduceMaps) {
                mapImportingIndicator.show()
                val reducer = HighQualityMapReducer(requireContext())
                reducer.reduce(map)
                mapImportingIndicator.hide()
            }

            if (map.calibrationPoints.isNotEmpty()) {
                toast(getString(R.string.map_auto_calibrated))
            }

            binding.addBtn.isEnabled = true
            findNavController().navigate(
                R.id.action_mapList_to_maps,
                bundleOf("mapId" to map.id)
            )

        }
    }


}
