package com.kylecorry.trail_sense.tools.backtrack.ui

import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding

interface BacktrackListItemStrategy {
    fun display(itemBinding: ListItemWaypointBinding, item: FragmentBacktrack.BacktrackListItem)
}