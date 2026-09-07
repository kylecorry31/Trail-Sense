package com.kylecorry.trail_sense.shared.andromeda_temp

import android.os.SystemClock

class SystemTimeProvider : TimeProvider {
    override fun elapsedRealtime(): Long {
        return SystemClock.elapsedRealtime()
    }

    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
