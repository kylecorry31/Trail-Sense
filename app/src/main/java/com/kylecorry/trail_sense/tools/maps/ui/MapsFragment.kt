package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapsBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.maps.infrastructure.commands.PrintMapCommand
import com.kylecorry.trail_sense.tools.maps.ui.commands.DeleteMapCommand
import com.kylecorry.trail_sense.tools.maps.ui.commands.RenameMapCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MapsFragment : BoundFragment<FragmentMapsBinding>() {

    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val mapService by lazy { MapService.getInstance(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private var mapId = 0L
    private var map: PhotoMap? = null
    private var currentFragment: Fragment? = null

    private val exportService by lazy { FragmentMapExportService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapsBinding {
        return FragmentMapsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recenterBtn.isVisible = false

        inBackground {
            loadMap()
        }

        binding.recenterBtn.setOnClickListener {
            val fragment = currentFragment
            if (fragment != null && fragment is ViewMapFragment) {
                fragment.recenter()
            }
        }

        binding.menuBtn.setOnClickListener {
            val fragment = currentFragment
            val isMapView = fragment != null && fragment is ViewMapFragment

            val actions = listOf(
                MapContextualAction.Calibrate to if (isMapView) getString(R.string.calibrate) else null,
                MapContextualAction.Guide to getString(R.string.tool_user_guide_title),
                MapContextualAction.Rename to getString(R.string.rename),
                MapContextualAction.ChangeProjection to if (isMapView) getString(R.string.change_map_projection) else null,
                MapContextualAction.Measure to if (isMapView) getString(R.string.measure) else null,
                MapContextualAction.CreatePath to if (isMapView) getString(R.string.create_path) else null,
                MapContextualAction.Export to if (isMapView) getString(R.string.export) else null,
                MapContextualAction.Print to if (isMapView && Printer.canPrint()) getString(R.string.print) else null,
                MapContextualAction.Delete to getString(R.string.delete)
            )


            Pickers.menu(
                it,
                actions.map { action -> action.second }
            ) { index ->
                when (actions[index].first) {
                    MapContextualAction.Calibrate -> calibrate()
                    MapContextualAction.Guide -> openGuide()
                    MapContextualAction.Rename -> rename()
                    MapContextualAction.ChangeProjection -> changeProjection()
                    MapContextualAction.Measure, MapContextualAction.CreatePath -> measure()
                    MapContextualAction.Export -> export()
                    MapContextualAction.Print -> print()
                    MapContextualAction.Delete -> delete()
                }
                true
            }
        }

    }

    private fun openGuide() {
        UserGuideUtils.openGuide(this, R.raw.importing_maps)
    }

    private fun calibrate() {
        val fragment = currentFragment
        if (fragment != null && fragment is ViewMapFragment) {
            fragment.calibrateMap()
        }
    }

    private fun delete() {
        inBackground {
            map?.let {
                DeleteMapCommand(requireContext(), mapService).execute(it)
                findNavController().popBackStack()
            }
        }
    }

    private fun print() {
        val command = PrintMapCommand(requireContext())
        inBackground(BackgroundMinimumState.Created) {
            map?.let {
                command.execute(it)
            }
        }
    }

    private fun export() {
        inBackground {
            map?.let {
                mapRepo.getMap(it.id)?.let { updated ->
                    exportService.export(updated)
                }
            }
        }
    }

    private fun measure() {
        val fragment = currentFragment
        if (fragment != null && fragment is ViewMapFragment) {
            fragment.startDistanceMeasurement()
        }
    }

    private fun rename() {
        inBackground {
            map?.let {
                mapRepo.getMap(it.id)?.let { updated ->
                    RenameMapCommand(requireContext(), mapService).execute(updated)
                    map = mapRepo.getMap(updated.id)
                    binding.mapName.text = map?.name
                }
            }
        }
    }

    private fun changeProjection() {
        val projections = MapProjectionType.values()
        val projectionNames = projections.map { formatter.formatMapProjection(it) }
        Pickers.item(
            requireContext(),
            getString(R.string.change_map_projection),
            projectionNames,
            projections.indexOf(map?.metadata?.projection)
        ) {
            if (it != null) {
                map?.let { m ->
                    val newProjection = projections[it]
                    inBackground {
                        withContext(Dispatchers.IO) {
                            val updated = mapRepo.getMap(m.id)!!
                            map = mapService.setProjection(updated, newProjection)
                        }
                        withContext(Dispatchers.Main) {
                            val fragment = currentFragment
                            if (fragment != null && fragment is ViewMapFragment) {
                                fragment.reloadMap()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadMap() {
        withContext(Dispatchers.IO) {
            map = mapRepo.getMap(mapId)
        }
        withContext(Dispatchers.Main) {
            map?.let {
                onMapLoad(it)
            }
        }
    }

    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        binding.mapName.text = map.name
        val fragmentManager = parentFragmentManager
        val transaction = fragmentManager.beginTransaction()
        val fragment = when {
            !map.calibration.warped -> {
                WarpMapFragment().apply {
                    arguments = bundleOf("mapId" to mapId)
                }.also {
                    it.setOnCompleteListener {
                        inBackground {
                            loadMap()
                        }
                    }
                }
            }

            !map.calibration.rotated -> {
                RotateMapFragment().apply {
                    arguments = bundleOf("mapId" to mapId)
                }.also {
                    it.setOnCompleteListener {
                        inBackground {
                            loadMap()
                        }
                    }
                }
            }

            else -> {
                binding.recenterBtn.isVisible = true
                ViewMapFragment().apply {
                    arguments = bundleOf("mapId" to mapId)
                }
            }
        }
        currentFragment = fragment
        transaction.replace(binding.mapFragment.id, fragment)
            .addToBackStack(null).commit()
    }
}