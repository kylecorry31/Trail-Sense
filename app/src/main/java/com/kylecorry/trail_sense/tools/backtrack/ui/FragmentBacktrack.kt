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
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBacktrackBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainIconMenuBinding
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.backtrack.domain.PathGPXConverter
import com.kylecorry.trail_sense.tools.backtrack.domain.pathsort.*
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

    private val gps by lazy {
        SensorService(requireContext()).getGPS(false)
    }

    private val gpxService by lazy {
        IOFactory().createGpxService(this)
    }

    private var wasEnabled = false

    private var paths = emptyList<Path>()
    private var sort = PathSortMethod.MostRecent

    private lateinit var listView: ListView<Path>


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

        binding.menuButton.setOnClickListener {
            val defaultSort = prefs.navigation.pathSort
            Pickers.menu(
                it, listOf(
                    getString(R.string.sort_by, defaultSort.name)
                )
            ) { selected ->
                when (selected) {
                    0 -> changeSort()
                }
                true
            }
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

    private fun onPathsChanged(paths: List<Path>) {
        this.paths = sortPaths(paths)
        listView.setData(this.paths)

        if (paths.isEmpty()) {
            binding.waypointsEmptyText.visibility = View.VISIBLE
        } else {
            binding.waypointsEmptyText.visibility = View.INVISIBLE
        }
    }

    private fun changeSort() {
        val sortOptions = PathSortMethod.values()
        Pickers.item(
            requireContext(),
            getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.navigation.pathSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.navigation.pathSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: PathSortMethod): String {
        return when (sortMethod) {
            PathSortMethod.MostRecent -> getString(R.string.most_recent)
            PathSortMethod.Longest -> getString(R.string.longest)
            PathSortMethod.Shortest -> getString(R.string.shortest)
            PathSortMethod.Closest -> getString(R.string.closest)
            PathSortMethod.Name -> getString(R.string.name)
        }
    }

    private fun onSortChanged() {
        paths = sortPaths(paths)
        listView.setData(paths)
    }

    private fun drawPathListItem(itemBinding: ListItemPlainIconMenuBinding, item: Path) {
        val itemStrategy =
            PathListItem(
                requireContext(),
                formatService,
                prefs
            ) { path, action ->
                when (action) {
                    PathAction.Export -> exportPath(path)
                    PathAction.Delete -> deletePath(path)
                    PathAction.Merge -> mergePreviousPath(path)
                    PathAction.Show -> showPath(path)
                    PathAction.Rename -> renamePath(path)
                    PathAction.Keep -> keepPath(path)
                    PathAction.ToggleVisibility -> togglePathVisibility(path)
                }
            }
        itemStrategy.display(itemBinding, item)
    }

    private fun sortPaths(paths: List<Path>): List<Path> {
        val strategy = when (sort) {
            PathSortMethod.MostRecent -> MostRecentPathSortStrategy()
            PathSortMethod.Longest -> LongestPathSortStrategy()
            PathSortMethod.Shortest -> ShortestPathSortStrategy()
            PathSortMethod.Closest -> ClosestPathSortStrategy(gps.location)
            PathSortMethod.Name -> NamePathSortStrategy()
        }
        return strategy.sort(paths)
    }

    private fun togglePathVisibility(path: Path) {
        runInBackground {
            withContext(Dispatchers.IO) {
                pathService.addPath(path.copy(style = path.style.copy(visible = !path.style.visible)))
            }
        }
    }

    private fun renamePath(path: Path) {
        Pickers.text(
            requireContext(),
            getString(R.string.rename),
            default = path.name,
            hint = getString(R.string.name)
        ) {
            if (it != null) {
                runInBackground {
                    withContext(Dispatchers.IO) {
                        pathService.addPath(path.copy(name = if (it.isBlank()) null else it))
                    }
                }

            }
        }
    }

    private fun keepPath(path: Path) {
        runInBackground {
            withContext(Dispatchers.IO) {
                pathService.addPath(path.copy(temporary = false))
            }
        }
    }

    private fun showPath(path: Path) {
        findNavController().navigate(R.id.action_backtrack_to_path, bundleOf("path_id" to path.id))
    }

    private fun exportPath(path: Path) {
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

    private fun deletePath(path: Path) {
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

    private fun mergePreviousPath(path: Path) {
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