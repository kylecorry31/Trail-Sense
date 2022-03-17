package com.kylecorry.trail_sense.shared.navigation

class NullAppNavigation: IAppNavigation {
    override fun navigate(actionId: Int, params: List<Pair<String, Any?>>) {
        // Do nothing
    }
}