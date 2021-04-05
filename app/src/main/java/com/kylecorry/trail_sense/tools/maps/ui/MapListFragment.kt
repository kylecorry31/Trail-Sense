package com.kylecorry.trail_sense.tools.maps.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapRegion
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trailsensecore.infrastructure.persistence.LocalFileService
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapListFragment : BoundFragment<FragmentMapListBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val fileService by lazy { LocalFileService(requireContext()) }

    private lateinit var mapList: ListView<Map>
    private var maps: List<Map> = listOf()

    private var boundMap = mutableMapOf<Long, MapRegion>()
    private var bitmaps = mutableMapOf<Long, Bitmap>()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapListBinding {
        return FragmentMapListBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                gps.read()
            }
            withContext(Dispatchers.Main) {
                maps = maps.sortedBy {
                    !(boundMap[it.id]?.contains(gps.location) ?: false)
                    // TODO: Distance to center
                }
                mapList.setData(maps)
            }
        }

        mapList = ListView(binding.mapList, R.layout.list_item_map) { itemView: View, map: Map ->
            val mapItemBinding = ListItemMapBinding.bind(itemView)
            val onMap = boundMap[map.id]?.contains(gps.location) ?: false
            mapItemBinding.mapImg.setImageBitmap(bitmaps[map.id])
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
            maps.forEach {
                val file = fileService.getFile(it.filename, false)
                val bitmap = BitmapFactory.decodeFile(file.path)
                val bounds = it.boundary(bitmap.width.toFloat(), bitmap.height.toFloat())
                if (bounds != null) {
                    boundMap[it.id] = bounds
                }
                bitmaps[it.id] = bitmap
            }

            maps = maps.sortedBy {
                !(boundMap[it.id]?.contains(gps.location) ?: false)
                // TODO: Distance to center
            }

            mapList.setData(it)
        })
    }

}