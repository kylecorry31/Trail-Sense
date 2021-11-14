package com.kylecorry.trail_sense.tools.backtrack.ui.commands

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.paths.Path


class ViewPathCommand(
    private val navController: NavController
) : IPathCommand {

    override fun execute(path: Path) {
        navController.navigate(R.id.action_backtrack_to_path, bundleOf("path_id" to path.id))
    }
}