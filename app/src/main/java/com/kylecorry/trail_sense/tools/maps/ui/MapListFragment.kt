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
import com.kylecorry.trail_sense.shared.io.FileSaver
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.maps.infrastructure.PDFUtils
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

    private lateinit var mapList: ListView<Map>
    private var maps: List<Map> = listOf()

    private var boundMap = mutableMapOf<Long, CoordinateBounds>()
    private var bitmaps = mutableMapOf<Long, Bitmap>()
    private var fileSizes = mutableMapOf<Long, Long>()

    private var mapName = ""

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

        mapRepo.getMaps().observe(viewLifecycleOwner, {
            maps = it
            // TODO: Show loading indicator
            maps.forEach {
                val file = LocalFiles.getFile(requireContext(), it.filename, false)

                val size = BitmapUtils.getBitmapSize(file.path)
                val bounds = it.boundary(size.first.toFloat(), size.second.toFloat())
                if (bounds != null) {
                    val onMap = bounds.contains(gps.location)
                    val distance = gps.location.distanceTo(bounds.center)

                    if (onMap || distance < 5000) {
                        val bitmap = BitmapUtils.decodeBitmapScaled(
                            file.path,
                            Resources.dp(requireContext(), 64f).toInt(),
                            Resources.dp(requireContext(), 64f).toInt()
                        )
                        bitmaps[it.id] = bitmap
                    }

                    fileSizes[it.id] = file.length()
                    boundMap[it.id] = bounds
                }
            }

            maps = maps.sortedBy {
                val bounds = boundMap[it.id] ?: return@sortedBy Float.MAX_VALUE
                val onMap = bounds.contains(gps.location)
                (if (onMap) 0f else 100000f) + gps.location.distanceTo(bounds.center)
            }

            mapList.setData(maps)
        })
    }

    private fun createMap() {
        pickFile(
            listOf("image/*", "application/pdf"),
            getString(R.string.select_map_image)
        ) {
            it?.also { returnUri ->
                mapFromUri(returnUri)
            }
        }
    }

    private fun mapFromUri(uri: Uri) {
        binding.mapLoading.isVisible = true
        binding.addBtn.isEnabled = false
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val type = requireContext().contentResolver.getType(uri)
                var calibration1: MapCalibrationPoint? = null
                var calibration2: MapCalibrationPoint? = null

                val extension = if (type == "application/pdf"){
                    "jpg"
                } else {
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
                }

                println(extension)

                val filename = "maps/" + UUID.randomUUID().toString() + "." + extension

                if (type == "application/pdf") {
                    val geopoints = PDFUtils.getGeospatialCalibration(requireContext(), uri)
                    if (geopoints.size >= 2) {
                        calibration1 = geopoints[0]
                        calibration2 = geopoints[1]
                    }
                    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                    val bp = PDFUtils.asBitmap(requireContext(), uri)
                    if (bp == null) {
                        withContext(Dispatchers.Main) {
                            Alerts.toast(
                                requireContext(),
                                getString(R.string.error_importing_map)
                            )
                            binding.mapLoading.isVisible = false
                            binding.addBtn.isEnabled = true
                        }
                        return@withContext
                    }

                    try {
                        copyToLocalStorage(bp, filename)
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            Alerts.toast(
                                requireContext(),
                                getString(R.string.error_importing_map)
                            )
                            binding.mapLoading.isVisible = false
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
                            binding.mapLoading.isVisible = false
                            binding.addBtn.isEnabled = true
                        }
                        return@withContext
                    }
                    copyToLocalStorage(stream, filename)
                }

                val calibrationPoints = listOfNotNull(calibration1, calibration2)
                val id = mapRepo.addMap(
                    Map(
                        0,
                        mapName,
                        filename,
                        calibrationPoints,
                        warped = calibrationPoints.isNotEmpty(),
                        rotated = calibrationPoints.isNotEmpty()
                    )
                )

                withContext(Dispatchers.Main) {
                    if (calibration1 != null) {
                        Alerts.toast(
                            requireContext(),
                            getString(R.string.map_auto_calibrated)
                        )
                    }
                    binding.mapLoading.isVisible = true
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
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
