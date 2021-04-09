package com.kylecorry.trail_sense.tools.maps.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapListBinding
import com.kylecorry.trail_sense.databinding.ListItemMapBinding
import com.kylecorry.trail_sense.shared.BoundFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapRegion
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.persistence.LocalFileService
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MapListFragment : BoundFragment<FragmentMapListBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val fileService by lazy { LocalFileService(requireContext()) }
    private val localFileService by lazy { LocalFileService(requireContext()) }
    private val cache by lazy { Cache(requireContext()) }

    private lateinit var mapList: ListView<Map>
    private var maps: List<Map> = listOf()

    private var boundMap = mutableMapOf<Long, MapRegion>()
    private var bitmaps = mutableMapOf<Long, Bitmap>()

    private var mapName = ""

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapListBinding {
        return FragmentMapListBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (cache.getBoolean("tool_maps_experimental_disclaimer_shown") != true) {
            UiUtils.alertWithCancel(
                requireContext(),
                getString(R.string.experimental),
                "Offline Maps is an experimental feature, please only use this to test it out at this point. Feel free to share your feedback on this feature and note that there is still a lot to be done before this will be non-experimental.",
                getString(R.string.tool_user_guide_title),
                getString(R.string.dialog_ok)
            ) { cancelled ->
                cache.putBoolean("tool_maps_experimental_disclaimer_shown", true)
                if (!cancelled) {
                    UserGuideUtils.openGuide(this, R.raw.importing_maps)
                }
            }
        }

        binding.addBtn.setOnClickListener {
            CustomUiUtils.pickText(
                requireContext(),
                getString(R.string.create_map),
                getString(R.string.create_map_description),
                null,
                hint = getString(R.string.name_hint)
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
            mapItemBinding.name.text = map.name
            mapItemBinding.description.text = if (onMap) getString(R.string.on_map) else ""
            mapItemBinding.root.setOnClickListener {
                findNavController().navigate(
                    R.id.action_mapList_to_maps,
                    bundleOf("mapId" to map.id)
                )
            }
        }

        mapList.addLineSeparator()

        mapRepo.getMaps().observe(viewLifecycleOwner, {
            maps = it
            // TODO: Show loading indicator
            maps.forEach {
                val file = fileService.getFile(it.filename, false)

                val size = CustomUiUtils.getBitmapSize(file.path)
                val bounds = it.boundary(size.first.toFloat(), size.second.toFloat())
                if (bounds != null) {
                    val onMap = bounds.contains(gps.location)
                    val distance = gps.location.distanceTo(bounds.center)

                    if (onMap || distance < 5000) {
                        val bitmap = CustomUiUtils.decodeBitmapScaled(
                            file.path,
                            CustomUiUtils.dp(requireContext(), 64f).toInt(),
                            CustomUiUtils.dp(requireContext(), 64f).toInt()
                        )
                        bitmaps[it.id] = bitmap
                    }

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
        val requestFileIntent = IntentUtils.pickFile(
            "image/*",
            getString(R.string.select_map_image)
        )
        startActivityForResult(requestFileIntent, REQUEST_CODE_SELECT_MAP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_MAP && resultCode == Activity.RESULT_OK) {
            data?.data?.also { returnUri ->
                mapFromUri(returnUri)
            }
        }
    }

    private fun mapFromUri(uri: Uri) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val stream = try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    requireContext().contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    null
                }
                stream ?: return@withContext
                val bitmap = BitmapFactory.decodeStream(stream)
                val filename = "maps/" + UUID.randomUUID().toString() + ".jpg"
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    FileOutputStream(localFileService.getFile(filename)).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                } catch (e: IOException) {
                }

                @Suppress("BlockingMethodInNonBlockingContext")
                stream.close()

                // TODO: Ask for map name
                mapRepo.addMap(Map(0, mapName, filename, listOf()))
            }

        }
    }

    companion object {
        const val REQUEST_CODE_SELECT_MAP = 11
    }

}