package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlinx.coroutines.runBlocking

class IsCurrentPathSpecification(private val context: Context) : Specification<Long>() {
    override fun isSatisfiedBy(value: Long): Boolean {
        val prefs = UserPreferences(context)
        if (!prefs.backtrackEnabled || (prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack)) return false
        val current = runBlocking {
            PathService.getInstance(context).getBacktrackPathId()
        }
        return current == value
    }
}