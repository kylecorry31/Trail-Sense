package com.kylecorry.trail_sense.tools.backtrack.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.filterSatisfied
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBacktrackBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainIconMenuBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.tools.backtrack.domain.PathGPXConverter
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.IsValidBacktrackPointSpecification
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class FragmentBacktrack : BoundFragment<FragmentBacktrackBinding>() {

    private val waypointRepo by lazy { WaypointRepo.getInstance(requireContext()) }
    private lateinit var waypointsLiveData: LiveData<List<WaypointEntity>>
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val geoService = GeologyService()

    private val gpxService by lazy {
        IOFactory().createGpxService(this)
    }

    private var pathIds: List<Long> = emptyList()

    private var wasEnabled = false

    private lateinit var listView: ListView<List<PathPoint>>


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView =
            ListView(binding.waypointsList, R.layout.list_item_plain_icon_menu) { itemView, item ->
                drawPathListItem(ListItemPlainIconMenuBinding.bind(itemView), item)
            }

        listView.addLineSeparator()

        waypointsLiveData = waypointRepo.getWaypoints()
        waypointsLiveData.observe(viewLifecycleOwner) { waypoints ->
            onWaypointsChanged(waypoints.map { it.toPathPoint() })
        }

        wasEnabled = prefs.backtrackEnabled
        if (wasEnabled && !(prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack)) {
            binding.startBtn.setImageResource(R.drawable.ic_baseline_stop_24)
        } else {
            binding.startBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }

        binding.startBtn.setOnClickListener {
            if (prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack) {
                Alerts.toast(
                    requireContext(),
                    getString(R.string.backtrack_disabled_low_power_toast)
                )
            } else {
                prefs.backtrackEnabled = !wasEnabled
                if (!wasEnabled) {
                    binding.startBtn.setImageResource(R.drawable.ic_baseline_stop_24)
                    BacktrackScheduler.start(requireContext(), true)
                } else {
                    binding.startBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                    BacktrackScheduler.stop(requireContext())
                }
                wasEnabled = !wasEnabled
            }
        }

        scheduleUpdates(INTERVAL_1_FPS)
    }

    override fun onUpdate() {
        wasEnabled = BacktrackScheduler.isOn(requireContext())
        if (wasEnabled && !(prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack)) {
            binding.startBtn.setImageResource(R.drawable.ic_baseline_stop_24)
        } else {
            binding.startBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBacktrackBinding {
        return FragmentBacktrackBinding.inflate(layoutInflater, container, false)
    }

    private fun onWaypointsChanged(waypoints: List<PathPoint>) {
        val filteredWaypoints = filterCurrentWaypoints(waypoints)
        val groupedWaypoints =
            groupWaypointsByPath(filteredWaypoints).toList().sortedByDescending { it.first }
        pathIds = groupedWaypoints.map { it.first }
        val listItems = groupedWaypoints.map { it.second }
        listView.setData(listItems)

        if (filteredWaypoints.isEmpty()) {
            binding.waypointsEmptyText.visibility = View.VISIBLE
        } else {
            binding.waypointsEmptyText.visibility = View.INVISIBLE
        }
    }

    private fun drawPathListItem(itemBinding: ListItemPlainIconMenuBinding, item: List<PathPoint>) {
        val itemStrategy =
            PathListItem(
                requireContext(),
                formatService,
                prefs,
                geoService,
                { deletePath(it) },
                { mergePreviousPath(it) },
                { showPath(it) },
                { exportPath(it) }
            )
        itemStrategy.display(itemBinding, item)
    }

    private fun filterCurrentWaypoints(waypoints: List<PathPoint>): List<PathPoint> {
        return waypoints.filterSatisfied(IsValidBacktrackPointSpecification(prefs.navigation.backtrackHistory))
    }

    private fun groupWaypointsByPath(waypoints: List<PathPoint>): Map<Long, List<PathPoint>> {
        return waypoints.groupBy { it.pathId }
    }

    private fun showPath(path: List<PathPoint>) {
        val pathId = path.firstOrNull()?.pathId ?: return
        findNavController().navigate(R.id.action_backtrack_to_path, bundleOf("path_id" to pathId))
    }

    private fun exportPath(path: List<PathPoint>) {
        runInBackground {
            val gpx = PathGPXConverter().toGPX(path)
            val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
            val success = gpxService.export(gpx, exportFile)
            withContext(Dispatchers.Main) {
                if (success) {
                    Alerts.toast(
                        requireContext(),
                        getString(R.string.path_exported)
                    )
                } else {
                    Alerts.toast(
                        requireContext(),
                        getString(R.string.export_path_error)
                    )
                }
            }
        }
    }

    private fun deletePath(path: List<PathPoint>) {
        val pathId = path.firstOrNull()?.pathId ?: return
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_path),
            resources.getQuantityString(R.plurals.waypoints_to_be_deleted, path.size, path.size)
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        waypointRepo.deletePath(pathId)
                    }
                }
            }
        }
    }

    private fun mergePreviousPath(path: List<PathPoint>) {
        val current = path.first().pathId
        val previous = pathIds.filter { it < current }.maxOrNull()

        if (previous == null) {
            Alerts.toast(requireContext(), getString(R.string.no_previous_path))
            return
        }

        Alerts.dialog(
            requireContext(),
            getString(R.string.merge_previous_path_title)
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        waypointRepo.moveToPath(previous, current)
                    }
                }
            }
        }
    }

}