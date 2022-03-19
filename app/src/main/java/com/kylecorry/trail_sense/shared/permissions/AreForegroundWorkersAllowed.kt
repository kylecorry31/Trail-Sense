package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import android.os.Build
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.core.system.Android
import com.kylecorry.andromeda.permissions.Permissions

class AreForegroundWorkersAllowed : Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        return Android.sdk < Build.VERSION_CODES.S || Permissions.isIgnoringBatteryOptimizations(
            value
        )
    }
}