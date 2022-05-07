package com.kylecorry.trail_sense.navigation.paths.ui.commands

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.Path


class ViewPathCommand(
    private val navController: NavController
) : IPathCommand {

    override fun execute(path: Path) {
        execute(path.id)
    }

    fun execute(id: Long) {
        navController.navigate(R.id.action_backtrack_to_path, bundleOf("path_id" to id))
    }
}