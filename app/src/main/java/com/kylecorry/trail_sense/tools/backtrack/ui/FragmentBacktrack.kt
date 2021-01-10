package com.kylecorry.trail_sense.tools.backtrack.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
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
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.toZonedDateTime
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trailsensecore.domain.navigation.Beacon
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
    ): View? {
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
                itemBinding.waypointCoordinates.text =
                    formatService.formatLocation(waypoint.coordinate)
                val date = waypoint.createdInstant.toZonedDateTime()
                val time = date.toLocalTime()
                itemBinding.waypointTime.text = getString(
                    R.string.waypoint_time_format, formatService.formatDayOfWeek(date),
                    formatService.formatTime(time, false)
                )

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
                        withContext(Dispatchers.IO){
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

                        withContext(Dispatchers.Main){
                            findNavController().navigate(R.id.action_fragmentBacktrack_to_action_navigation, bundleOf("destination" to newTempId))
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
        if (wasEnabled) {
            binding.startBtn.setImageResource(R.drawable.ic_baseline_stop_24)
        } else {
            binding.startBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }

        binding.startBtn.setOnClickListener {
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

    private fun deleteWaypoint(waypointEntity: WaypointEntity) {
        // TODO: Prompt user
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                waypointRepo.deleteWaypoint(waypointEntity)
            }
        }
    }

    private fun createBeacon(waypoint: WaypointEntity) {
        val date = waypoint.createdInstant.toZonedDateTime()
        val time = date.toLocalTime()
        val bundle = bundleOf(
            "initial_location" to MyNamedCoordinate(
                waypoint.coordinate,
                getString(
                    R.string.waypoint_beacon_title_template,
                    formatService.formatDate(
                        date,
                        includeWeekDay = false
                    ), formatService.formatTime(time, showSeconds = false)
                )
            )
        )
        findNavController().navigate(R.id.place_beacon, bundle)
    }


}