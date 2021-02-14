package com.kylecorry.trail_sense.tools.backtrack.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBacktrackBinding
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.network.CellNetwork
import com.kylecorry.trailsensecore.domain.units.Quality
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class FragmentBacktrack : Fragment() {

    private val waypointRepo by lazy { WaypointRepo.getInstance(requireContext()) }
    private var _binding: FragmentBacktrackBinding? = null
    private val binding get() = _binding!!
    private lateinit var waypointsLiveData: LiveData<List<WaypointEntity>>
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }

    private var wasEnabled = false

    private lateinit var listView: ListView<WaypointEntity>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBacktrackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView =
            ListView(binding.waypointsList, R.layout.list_item_waypoint) { waypointView, waypoint ->
                val itemBinding = ListItemWaypointBinding.bind(waypointView)
                val timeAgo = Duration.between(waypoint.createdInstant, Instant.now())
                itemBinding.waypointCoordinates.text = getString(R.string.time_ago, timeAgo.formatHM(true))
                val date = waypoint.createdInstant.toZonedDateTime()
                val time = date.toLocalTime()
                itemBinding.waypointTime.text = getString(
                    R.string.waypoint_time_format, formatService.formatDayOfWeek(date),
                    formatService.formatTime(time, false)
                )

                if (prefs.backtrackSaveCellHistory) {
                    itemBinding.signalStrength.setStatusText(getCellTypeString(waypoint.cellNetwork))
                    itemBinding.signalStrength.setImageResource(getCellQualityImage(waypoint.cellQuality))
                    itemBinding.signalStrength.setForegroundTint(Color.BLACK)
                    itemBinding.signalStrength.setBackgroundTint(
                        CustomUiUtils.getQualityColor(
                            requireContext(),
                            waypoint.cellQuality
                        )
                    )
                    itemBinding.signalStrength.visibility = View.VISIBLE
                } else {
                    itemBinding.signalStrength.visibility = View.GONE
                }

                val menuListener = PopupMenu.OnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_waypoint_create_beacon -> {
                            createBeacon(waypoint)
                        }
                        R.id.action_waypoint_delete -> {
                            deleteWaypoint(waypoint)
                        }
                    }
                    true
                }

                itemBinding.waypointMenuBtn.setOnClickListener {
                    val popup = PopupMenu(it.context, it)
                    val inflater = popup.menuInflater
                    inflater.inflate(R.menu.waypoint_item_menu, popup.menu)
                    popup.setOnMenuItemClickListener(menuListener)
                    popup.show()
                }

                itemBinding.root.setOnClickListener {
                    lifecycleScope.launch {
                        var newTempId = 0L
                        withContext(Dispatchers.IO) {
                            val tempBeaconId = beaconRepo.getTemporaryBeacon()?.id ?: 0L
                            val beacon = Beacon(
                                tempBeaconId,
                                getString(
                                    R.string.waypoint_beacon_title_template,
                                    formatService.formatDate(
                                        date,
                                        includeWeekDay = false
                                    ), formatService.formatTime(time, showSeconds = false)
                                ),
                                waypoint.coordinate,
                                visible = false,
                                elevation = waypoint.altitude,
                                temporary = true
                            )
                            beaconRepo.addBeacon(BeaconEntity.from(beacon))

                            newTempId = beaconRepo.getTemporaryBeacon()?.id ?: 0L
                        }

                        withContext(Dispatchers.Main) {
                            findNavController().navigate(
                                R.id.action_fragmentBacktrack_to_action_navigation,
                                bundleOf("destination" to newTempId)
                            )
                        }

                    }

                }

            }

        listView.addLineSeparator()

        waypointsLiveData = waypointRepo.getWaypoints()
        waypointsLiveData.observe(viewLifecycleOwner) { waypoints ->
            val filteredWaypoints =
                waypoints.filter { it.createdInstant > Instant.now().minus(Duration.ofDays(2)) }
                    .sortedByDescending { it.createdOn }

            listView.setData(filteredWaypoints)

            if (filteredWaypoints.isEmpty()) {
                binding.waypointsEmptyText.visibility = View.VISIBLE
            } else {
                binding.waypointsEmptyText.visibility = View.INVISIBLE
            }
        }

        wasEnabled = prefs.backtrackEnabled
        if (wasEnabled && !(prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack)) {
            binding.startBtn.setImageResource(R.drawable.ic_baseline_stop_24)
        } else {
            binding.startBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }

        binding.startBtn.setOnClickListener {
            if (prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack){
                UiUtils.shortToast(requireContext(), getString(R.string.backtrack_disabled_low_power_toast))
            } else {
                prefs.backtrackEnabled = !wasEnabled
                if (!wasEnabled) {
                    binding.startBtn.setImageResource(R.drawable.ic_baseline_stop_24)
                    BacktrackScheduler.start(requireContext())
                } else {
                    binding.startBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                    BacktrackScheduler.stop(requireContext())
                }
                wasEnabled = !wasEnabled
            }
        }
    }

    private fun deleteWaypoint(waypointEntity: WaypointEntity) {
        UiUtils.alertWithCancel(
            requireContext(),
            getString(R.string.delete_waypoint_prompt),
            getWaypointTitle(waypointEntity),
            getString(R.string.dialog_ok),
            getString(R.string.dialog_cancel)
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        waypointRepo.deleteWaypoint(waypointEntity)
                    }
                }
            }
        }
    }

    private fun createBeacon(waypoint: WaypointEntity) {
        val bundle = bundleOf(
            "initial_location" to MyNamedCoordinate(
                waypoint.coordinate,
                getWaypointTitle(waypoint)
            )
        )
        findNavController().navigate(R.id.place_beacon, bundle)
    }

    private fun getWaypointTitle(waypoint: WaypointEntity): String {
        val date = waypoint.createdInstant.toZonedDateTime()
        val time = date.toLocalTime()
        return getString(
            R.string.waypoint_beacon_title_template,
            formatService.formatDate(
                date,
                includeWeekDay = false
            ), formatService.formatTime(time, showSeconds = false)
        )
    }

    @DrawableRes
    private fun getCellQualityImage(quality: Quality?): Int {
        return when(quality){
            Quality.Poor -> R.drawable.signal_cellular_1
            Quality.Moderate -> R.drawable.signal_cellular_2
            Quality.Good -> R.drawable.signal_cellular_3
            else -> R.drawable.signal_cellular_outline
        }
    }

    private fun getCellTypeString(cellType: CellNetwork?): String {
        return when (cellType){
            CellNetwork.Nr -> getString(R.string.network_5g)
            CellNetwork.Lte -> getString(R.string.network_4g)
            CellNetwork.Cdma, CellNetwork.Gsm -> getString(R.string.network_2g)
            CellNetwork.Wcdma -> getString(R.string.network_3g)
            else -> getString(R.string.network_no_signal)
        }
    }


}