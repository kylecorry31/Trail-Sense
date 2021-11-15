package com.kylecorry.trail_sense.navigation.paths.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPathPointsListBinding
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.paths.PathPoint

class PathPointsListFragment : BoundBottomSheetDialogFragment<FragmentPathPointsListBinding>() {

    private var points: List<PathPoint> = emptyList()
    private var list: ListView<PathPoint>? = null
    private val formatService by lazy { FormatService(requireContext()) }
    var onCreateBeaconListener: (point: PathPoint) -> Unit = {}
    var onDeletePointListener: (point: PathPoint) -> Unit = {}
    var onNavigateToPointListener: (point: PathPoint) -> Unit = {}
    var onViewPointListener: (point: PathPoint) -> Unit = {}

    fun setPoints(points: List<PathPoint>) {
        list?.setData(points)
        this.points = points
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list = ListView(binding.pathPointsList, R.layout.list_item_waypoint) { view, point ->
            val binding = ListItemWaypointBinding.bind(view)
            drawWaypointListItem(binding, point)
        }
        list?.addLineSeparator()
        list?.setData(points)
    }

    private fun drawWaypointListItem(itemBinding: ListItemWaypointBinding, item: PathPoint) {
        val itemStrategy = WaypointListItem(
            requireContext(),
            formatService,
            {
                onCreateBeaconListener(it)
                dismiss()
            },
            { onDeletePointListener(it) },
            {
                onNavigateToPointListener(it)
                dismiss()
            },
            {
                onViewPointListener(it)
                dismiss()
            }
        )

        itemStrategy.display(itemBinding, item)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPathPointsListBinding {
        return FragmentPathPointsListBinding.inflate(layoutInflater, container, false)
    }
}