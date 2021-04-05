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
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapRegion
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trailsensecore.infrastructure.persistence.ExternalFileService
import com.kylecorry.trailsensecore.infrastructure.persistence.LocalFileService
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
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

        binding.addBtn.setOnClickListener {
            createMap()
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

    private fun createMap(){
        val requestFileIntent = IntentUtils.pickFile(
            "image/*",
            getString(R.string.select_map_image)
        )
        startActivityForResult(requestFileIntent, REQUEST_CODE_SELECT_MAP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_MAP && resultCode == Activity.RESULT_OK){
            data?.data?.also { returnUri ->
                mapFromUri(returnUri)
            }
        }
    }

    private fun mapFromUri(uri: Uri){
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
                println(bitmap)
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

                mapRepo.addMap(Map(0, "Untitled", filename, listOf()))
            }

        }
    }

    companion object {
        const val REQUEST_CODE_SELECT_MAP = 11
    }

}