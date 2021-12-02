package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.science.oceanography.OceanographyService
import com.kylecorry.sol.science.oceanography.TideFrequency
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTideListBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainMenuBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TideListFragment: BoundFragment<FragmentTideListBinding>() {

    private lateinit var listView: ListView<TideEntity>
    private val formatService by lazy { FormatService(requireContext()) }
    private val oceanographyService = OceanographyService()
    private val tideRepo by lazy { TideRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(false) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTideListBinding {
        return FragmentTideListBinding.inflate(layoutInflater, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = ListView(binding.tideList, R.layout.list_item_plain_menu){ listItemView, tide ->
            val itemBinding = ListItemPlainMenuBinding.bind(listItemView)
            itemBinding.title.text = getTideTitle(tide)
            itemBinding.description.text = getTideTypeName(oceanographyService.getTideType(tide.reference, TideFrequency.Semidiurnal))
            itemBinding.root.setOnClickListener {
                tryOrNothing {
                    selectTide(tide)
                }
            }
            
            itemBinding.menuBtn.setOnClickListener {
                Pickers.menu(it, R.menu.tide_menu){ action ->
                    when (action){
                        R.id.action_tide_delete -> {
                            deleteTide(tide)
                        }
                        R.id.action_tide_edit -> {
                            editTide(tide)
                        }
                    }
                    true
                }
            }
        }

        listView.addLineSeparator()

        tideRepo.getTides().observe(viewLifecycleOwner, {
            listView.setData(it.sortedBy { tide ->
                tide.coordinate?.distanceTo(gps.location) ?: Float.POSITIVE_INFINITY
            })
            binding.tidesEmptyText.isVisible = it.isEmpty()
        })

        binding.addBtn.setOnClickListener {
            createTide()
        }
    }

    private fun deleteTide(tide: TideEntity){
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_tide_prompt),
            getTideTitle(tide)
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        tideRepo.deleteTide(tide)
                    }
                }
            }
        }
    }

    private fun editTide(tide: TideEntity){
        findNavController().navigate(R.id.action_tideList_to_createTide, bundleOf("edit_tide_id" to tide.id))
    }

    private fun createTide(){
        findNavController().navigate(R.id.action_tideList_to_createTide)
    }

    private fun selectTide(tide: TideEntity){
        prefs.tides.lastTide = tide.id
        findNavController().popBackStack()
    }

    private fun getTideTitle(tide: TideEntity): String {
        return tide.name ?: if (tide.coordinate != null) formatService.formatLocation(tide.coordinate!!) else getString(android.R.string.untitled)
    }

    private fun getTideTypeName(tideType: TideType): String {
        return when (tideType) {
            TideType.High -> getString(R.string.high_tide)
            TideType.Low -> getString(R.string.low_tide)
            TideType.Half -> getString(R.string.half_tide)
        }
    }

}