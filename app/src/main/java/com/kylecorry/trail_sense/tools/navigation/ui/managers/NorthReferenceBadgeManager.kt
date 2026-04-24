package com.kylecorry.trail_sense.tools.navigation.ui.managers

import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.NorthReferenceBadge
import java.time.Duration

class NorthReferenceBadgeManager {

    private val prefs = getAppService<UserPreferences>()

    private var lastView: NorthReferenceBadge? = null

    private val northReferenceHideTimer = CoroutineTimer {
        lastView?.showLabel = false
    }

    fun resume(badge: NorthReferenceBadge) {
        badge.showDetailsOnClick = true
        badge.useTrueNorth = prefs.compass.useTrueNorth
        badge.showLabel = true
        lastView = badge
        northReferenceHideTimer.once(Duration.ofSeconds(5))
    }

    fun pause() {
        northReferenceHideTimer.stop()
        lastView = null
    }

}
