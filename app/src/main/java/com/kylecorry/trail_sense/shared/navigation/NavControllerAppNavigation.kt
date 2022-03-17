package com.kylecorry.trail_sense.shared.navigation

import androidx.core.os.bundleOf
import androidx.navigation.NavController

class NavControllerAppNavigation(private val controller: NavController) : IAppNavigation {
    override fun navigate(actionId: Int, params: List<Pair<String, Any?>>) {
        controller.navigate(actionId, bundleOf(pairs = params.toTypedArray()))
    }
}