package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTideListBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainMenuBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TideListFragment: BoundFragment<FragmentTideListBinding>() {

    private lateinit var listView: ListView<TideTable>
    private val formatService by lazy { FormatService(requireContext()) }
    private val tideRepo by lazy { TideTableRepo.getInstance(requireContext()) }
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
//            itemBinding.description.text = getTideTypeName(oceanographyService.getTideType(tide.reference, TideFrequency.Semidiurnal))
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

        refreshTides()

        binding.addBtn.setOnClickListener {
            createTide()
        }
    }

    private fun deleteTide(tide: TideTable){
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_tide_prompt),
            getTideTitle(tide)
        ) { cancelled ->
            if (!cancelled) {
                runInBackground {
                    withContext(Dispatchers.IO) {
                        tideRepo.deleteTideTable(tide)
                    }

                    refreshTides()
                }
            }
        }
    }

    private fun editTide(tide: TideTable){
        findNavController().navigate(R.id.action_tideList_to_createTide, bundleOf("edit_tide_id" to tide.id))
    }

    private fun createTide(){
        findNavController().navigate(R.id.action_tideList_to_createTide)
    }

    private fun selectTide(tide: TideTable){
        prefs.tides.lastTide = tide.id
        findNavController().popBackStack()
    }

    private fun getTideTitle(tide: TideTable): String {
        return tide.name ?: if (tide.location != null) formatService.formatLocation(tide.location!!) else getString(android.R.string.untitled)
    }

    private fun refreshTides(){
        runInBackground {
            val tides = withContext(Dispatchers.IO){
                tideRepo.getTideTables()
            }

            withContext(Dispatchers.Main){
                if (isBound){
                    listView.setData(tides.sortedBy { tide ->
                        tide.location?.distanceTo(gps.location) ?: Float.POSITIVE_INFINITY
                    })
                    binding.tidesEmptyText.isVisible = tides.isEmpty()
                }
            }
        }
    }

    private fun getTideTypeName(tideType: TideType?): String {
        return when (tideType) {
            TideType.High -> getString(R.string.high_tide)
            TideType.Low -> getString(R.string.low_tide)
            null -> getString(R.string.half_tide)
        }
    }

}