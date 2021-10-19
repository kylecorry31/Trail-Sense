package com.kylecorry.trail_sense.tools.backtrack.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBacktrackBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainIconMenuBinding
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.paths.Path2
import com.kylecorry.trail_sense.tools.backtrack.domain.PathGPXConverter
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class FragmentBacktrack : BoundFragment<FragmentBacktrackBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val pathService by lazy {
        PathService.getInstance(requireContext())
    }

    private val gpxService by lazy {
        IOFactory().createGpxService(this)
    }

    private var wasEnabled = false

    private var paths = emptyList<Path2>()

    private lateinit var listView: ListView<Path2>


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView =
            ListView(binding.waypointsList, R.layout.list_item_plain_icon_menu) { itemView, item ->
                drawPathListItem(ListItemPlainIconMenuBinding.bind(itemView), item)
            }

        listView.addLineSeparator()

        pathService.getLivePaths().observe(viewLifecycleOwner) { paths ->
            onPathsChanged(paths)
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

    private fun onPathsChanged(paths: List<Path2>) {
        this.paths = paths.sortedByDescending { it.id }
        listView.setData(this.paths)

        if (paths.isEmpty()) {
            binding.waypointsEmptyText.visibility = View.VISIBLE
        } else {
            binding.waypointsEmptyText.visibility = View.INVISIBLE
        }
    }

    private fun drawPathListItem(itemBinding: ListItemPlainIconMenuBinding, item: Path2) {
        val itemStrategy =
            PathListItem(
                requireContext(),
                formatService,
                prefs,
                { deletePath(it) },
                { mergePreviousPath(it) },
                { showPath(it) },
                { exportPath(it) }
            )
        itemStrategy.display(itemBinding, item)
    }

    private fun showPath(path: Path2) {
        findNavController().navigate(R.id.action_backtrack_to_path, bundleOf("path_id" to path.id))
    }

    private fun exportPath(path: Path2) {
        runInBackground {
            val waypoints = pathService.getWaypoints(path.id)
            val gpx = PathGPXConverter().toGPX(waypoints)
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

    private fun deletePath(path: Path2) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_path),
            resources.getQuantityString(
                R.plurals.waypoints_to_be_deleted,
                path.metadata.waypoints,
                path.metadata.waypoints
            )
        ) { cancelled ->
            if (!cancelled) {
                runInBackground {
                    withContext(Dispatchers.IO) {
                        pathService.deletePath(path)
                    }
                }
            }
        }
    }

    private fun mergePreviousPath(path: Path2) {
        val previousPath = paths.filter { it.id < path.id }.maxByOrNull { it.id }
        if (previousPath == null) {
            Alerts.toast(requireContext(), getString(R.string.no_previous_path))
            return
        }

        Alerts.dialog(
            requireContext(),
            getString(R.string.merge_previous_path_title)
        ) { cancelled ->
            if (!cancelled) {
                runInBackground {
                    withContext(Dispatchers.IO) {
                        val waypoints = pathService.getWaypoints(previousPath.id)
                        pathService.moveWaypointsToPath(waypoints, path.id)
                        pathService.deletePath(previousPath)
                    }
                }
            }
        }
    }

}