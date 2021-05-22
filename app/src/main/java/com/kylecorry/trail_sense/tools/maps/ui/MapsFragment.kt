package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapsBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MapsFragment: BoundFragment<FragmentMapsBinding>() {

    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }

    private var mapId = 0L
    private var map: Map? = null
    private var currentFragment: Fragment? = null

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

        lifecycleScope.launch {
           loadMap()
        }

        binding.recenterBtn.setOnClickListener {
            val fragment = currentFragment
            if (fragment != null && fragment is ViewMapFragment) {
                fragment.recenter()
            }
        }

        binding.menuBtn.setOnClickListener {
            UiUtils.openMenu(it, R.menu.map_menu) {
                when (it) {
                    R.id.action_map_delete -> {
                        UiUtils.alertWithCancel(
                            requireContext(),
                            getString(R.string.delete_map),
                            map?.name ?: "",
                            getString(R.string.dialog_ok),
                            getString(R.string.dialog_cancel)
                        ) { cancelled ->
                            if (!cancelled) {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        map?.let {
                                            mapRepo.deleteMap(it)
                                        }
                                    }
                                    withContext(Dispatchers.IO) {
                                        requireActivity().onBackPressed()
                                    }
                                }
                            }
                        }
                    }
                    R.id.action_map_guide -> {
                        UserGuideUtils.openGuide(this, R.raw.importing_maps)
                    }
                    R.id.action_map_rename -> {
                        CustomUiUtils.pickText(
                            requireContext(),
                            getString(R.string.create_map),
                            getString(R.string.create_map_description),
                            map?.name,
                            hint = getString(R.string.name_hint)
                        ) {
                            if (it != null) {
                                map = map?.copy(name = it)
                                binding.mapName.text = it
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        map?.let {
                                            mapRepo.addMap(it)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    R.id.action_map_calibrate -> {
                        val fragment = currentFragment
                        if (fragment != null && fragment is ViewMapFragment) {
                            fragment.calibrateMap()
                        }
                    }
                    else -> {
                    }
                }
                true
            }
        }

    }

    private suspend fun loadMap(){
        withContext(Dispatchers.IO) {
            map = mapRepo.getMap(mapId)
        }
        withContext(Dispatchers.Main) {
            map?.let {
                onMapLoad(it)
            }
        }
    }

    private fun onMapLoad(map: Map) {
        this.map = map
        binding.mapName.text = map.name
        val fragmentManager = parentFragmentManager
        val transaction = fragmentManager.beginTransaction()
        val fragment = when {
            !map.warped -> {
                WarpMapFragment().apply {
                    arguments = bundleOf("mapId" to mapId)
                }.also {
                    it.setOnCompleteListener {
                        lifecycleScope.launch {
                            loadMap()
                        }
                    }
                }
            }
            !map.rotated -> {
                RotateMapFragment().apply {
                    arguments = bundleOf("mapId" to mapId)
                }.also {
                    it.setOnCompleteListener {
                        lifecycleScope.launch {
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