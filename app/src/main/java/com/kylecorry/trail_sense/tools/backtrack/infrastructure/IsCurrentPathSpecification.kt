package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences

class IsCurrentPathSpecification(private val context: Context) : Specification<Long>() {
    override fun isSatisfiedBy(pathId: Long): Boolean {
        val prefs = UserPreferences(context)
        val cache = Preferences(context)
        if (!prefs.backtrackEnabled || (prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack)) return false
        val current = cache.getLong(context.getString(R.string.pref_last_backtrack_path_id))
        return current == pathId
    }
}