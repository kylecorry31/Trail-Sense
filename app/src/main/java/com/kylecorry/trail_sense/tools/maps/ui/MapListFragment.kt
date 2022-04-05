package com.kylecorry.trail_sense.tools.maps.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pdf.GeospatialPDFParser
import com.kylecorry.andromeda.pdf.PDFRenderer
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapListBinding
import com.kylecorry.trail_sense.databinding.ListItemMapBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.FileSaver
import com.kylecorry.trail_sense.shared.io.FragmentUriPicker
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.infrastructure.ImageSaver
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.maps.infrastructure.reduce.HighQualityMapReducer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

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
    private var bitmaps = mutableMapOf<Long, Bitmap>()
    private var fileSizes = mutableMapOf<Long, Long>()

    private var mapName = ""

    private val uriPicker = FragmentUriPicker(this)

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
                    mapFromUri(mapIntentUri)
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
                    createMap()
                }
            }
        }

        mapList = ListView(binding.mapList, R.layout.list_item_map) { itemView: View, map: Map ->
            val mapItemBinding = ListItemMapBinding.bind(itemView)
            val onMap = boundMap[map.id]?.contains(gps.location) ?: false
            if (bitmaps.containsKey(map.id)) {
                mapItemBinding.mapImg.setImageBitmap(bitmaps[map.id])
            } else {
                mapItemBinding.mapImg.setImageResource(R.drawable.maps)
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
                    val onMap = bounds.contains(gps.location)
                    val distance = gps.location.distanceTo(bounds.center)

                    if (onMap || distance < 5000) {
                        // This can fail if the map's path changes while loading
                        tryOrNothing {
                            val bitmap = BitmapUtils.decodeBitmapScaled(
                                file.path,
                                Resources.dp(requireContext(), 64f).toInt(),
                                Resources.dp(requireContext(), 64f).toInt()
                            )
                            bitmaps[it.id] = bitmap
                        }
                    }

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

    private fun createMap() {
        runInBackground {
            val uri = uriPicker.open(listOf("image/*", "application/pdf"))
            uri?.let { mapFromUri(it) }
        }
    }

    private fun mapFromUri(uri: Uri) {
        binding.addBtn.isEnabled = false
        lifecycleScope.launch {
            val loading = withContext(Dispatchers.Main){
                Alerts.loading(requireContext(), getString(R.string.importing_map))
            }

            withContext(Dispatchers.IO) {
                val type = requireContext().contentResolver.getType(uri)
                var calibration1: MapCalibrationPoint? = null
                var calibration2: MapCalibrationPoint? = null
                var projection = MapProjectionType.CylindricalEquidistant

                val extension = if (type == "application/pdf"){
                    "webp"
                } else {
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
                }

                val filename = "maps/" + UUID.randomUUID().toString() + "." + extension

                @Suppress("BlockingMethodInNonBlockingContext")
                if (type == "application/pdf") {
                    val parser = GeospatialPDFParser()
                    val metadata = requireContext().contentResolver.openInputStream(uri)?.use {
                        parser.parse(it)
                    }

                    val scale = Screen.dpi(requireContext()) / 72
                    val bp = PDFRenderer().toBitmap(requireContext(), uri, scale = scale)
                    if (bp == null) {
                        withContext(Dispatchers.Main) {
                            Alerts.toast(
                                requireContext(),
                                getString(R.string.error_importing_map)
                            )
                            loading.dismiss()
                            binding.addBtn.isEnabled = true
                        }
                        return@withContext
                    }

                    if (metadata != null && metadata.points.size >= 4){
                        val points = listOf(metadata.points[1], metadata.points[3]).map {
                            MapCalibrationPoint(it.second, PercentCoordinate(scale * it.first.x / bp.width, scale * it.first.y / bp.height))
                        }
                        calibration1 = points[0]
                        calibration2 = points[1]
                    }

                    val projectionName = metadata?.projection?.projection

                    if (projectionName != null && projectionName.contains("mercator", true)){
                        projection = MapProjectionType.Mercator
                    }

                    try {
                        copyToLocalStorage(bp, filename)
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            Alerts.toast(
                                requireContext(),
                                getString(R.string.error_importing_map)
                            )
                            loading.dismiss()
                            binding.addBtn.isEnabled = true
                        }
                        return@withContext
                    }
                } else {
                    val stream = try {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        requireContext().contentResolver.openInputStream(uri)
                    } catch (e: Exception) {
                        null
                    }
                    if (stream == null) {
                        withContext(Dispatchers.Main) {
                            Alerts.toast(
                                requireContext(),
                                getString(R.string.error_importing_map)
                            )
                            loading.dismiss()
                            binding.addBtn.isEnabled = true
                        }
                        return@withContext
                    }
                    copyToLocalStorage(stream, filename)
                }

                val calibrationPoints = listOfNotNull(calibration1, calibration2)
                val map = Map(
                    0,
                    mapName,
                    filename,
                    calibrationPoints,
                    warped = calibrationPoints.isNotEmpty(),
                    rotated = calibrationPoints.isNotEmpty(),
                    projection = projection
                )
                val id = mapRepo.addMap(map)

                if (prefs.navigation.autoReduceMaps){
                    val reducer = HighQualityMapReducer(requireContext())
                    reducer.reduce(map.copy(id = id))
                }

                withContext(Dispatchers.Main) {
                    if (calibration1 != null) {
                        Alerts.toast(
                            requireContext(),
                            getString(R.string.map_auto_calibrated)
                        )
                    }
                    loading.dismiss()
                    binding.addBtn.isEnabled = true
                    findNavController().navigate(
                        R.id.action_mapList_to_maps,
                        bundleOf("mapId" to id)
                    )
                }
            }

        }
    }


    private fun copyToLocalStorage(bitmap: Bitmap, filename: String) {
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            FileOutputStream(LocalFiles.getFile(requireContext(), filename)).use { out ->
                ImageSaver().save(bitmap, out)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun copyToLocalStorage(stream: InputStream, filename: String) {
        val saver = FileSaver()
        saver.save(stream, LocalFiles.getFile(requireContext(), filename))
    }


}
