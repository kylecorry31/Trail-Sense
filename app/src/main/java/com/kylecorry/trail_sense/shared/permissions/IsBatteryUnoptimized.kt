package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.permissions.Permissions

class IsBatteryUnoptimized : Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        return Permissions.isIgnoringBatteryOptimizations(value)
    }
}