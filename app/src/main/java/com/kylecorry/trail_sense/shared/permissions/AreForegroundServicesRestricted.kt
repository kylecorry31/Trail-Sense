package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import android.os.Build
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.core.system.Android

class AreForegroundServicesRestricted: Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        return Android.sdk >= Build.VERSION_CODES.S && IsBatteryUsageRestricted().isSatisfiedBy(value)
    }
}