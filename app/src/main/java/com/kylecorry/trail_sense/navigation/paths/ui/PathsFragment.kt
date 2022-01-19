package com.kylecorry.trail_sense.navigation.paths.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPathsBinding
import com.kylecorry.trail_sense.databinding.ListItemPlainIconMenuBinding
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.pathsort.*
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.ui.commands.*
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService

class PathsFragment : BoundFragment<FragmentPathsBinding>() {

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

        sort = prefs.navigation.pathSort

        listView =
            ListView(binding.waypointsList, R.layout.list_item_plain_icon_menu) { itemView, item ->
                drawPathListItem(ListItemPlainIconMenuBinding.bind(itemView), item)
            }

        listView.addLineSeparator()

        pathService.getLivePaths().observe(viewLifecycleOwner) { paths ->
            onPathsChanged(paths)
        }

        binding.pathsTitle.rightQuickAction.setOnClickListener {
            val defaultSort = prefs.navigation.pathSort
            Pickers.menu(
                it, listOf(
                    getString(R.string.sort_by, getSortString(defaultSort)),
                    getString(R.string.import_gpx)
                )
            ) { selected ->
                when (selected) {
                    0 -> changeSort()
                    1 -> importPaths()
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
    ): FragmentPathsBinding {
        return FragmentPathsBinding.inflate(layoutInflater, container, false)
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
                    PathAction.Merge -> merge(path)
                    PathAction.Show -> showPath(path)
                    PathAction.Rename -> renamePath(path)
                    PathAction.Keep -> keepPath(path)
                    PathAction.ToggleVisibility -> togglePathVisibility(path)
                    PathAction.Simplify -> simplifyPath(path)
                }
            }
        itemStrategy.display(itemBinding, item)
    }

    private fun simplifyPath(path: Path) {
        val command = SimplifyPathCommand(requireContext(), lifecycleScope, pathService)
        command.execute(path)
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
        val command = TogglePathVisibilityCommand(requireContext(), lifecycleScope, pathService)
        command.execute(path)
    }

    private fun renamePath(path: Path) {
        val command = RenamePathCommand(requireContext(), lifecycleScope, pathService)
        command.execute(path)
    }

    private fun keepPath(path: Path) {
        val command = KeepPathCommand(requireContext(), lifecycleScope, pathService)
        command.execute(path)
    }

    private fun showPath(path: Path) {
        val command = ViewPathCommand(findNavController())
        command.execute(path)
    }

    private fun importPaths() {
        val command = ImportPathsCommand(
            requireContext(),
            lifecycleScope,
            gpxService,
            pathService,
            prefs.navigation
        )
        command.execute()
    }

    private fun exportPath(path: Path) {
        val command = ExportPathCommand(requireContext(), lifecycleScope, gpxService, pathService)
        command.execute(path)
    }

    private fun deletePath(path: Path) {
        val command = DeletePathCommand(requireContext(), lifecycleScope, pathService)
        command.execute(path)
    }

    private fun merge(path: Path) {
        val command = MergePathCommand(requireContext(), lifecycleScope, paths, pathService)
        command.execute(path)
    }

}