package com.kylecorry.trail_sense.shared.andromeda_temp

interface TimeProvider {
    fun elapsedRealtime(): Long
    fun currentTimeMillis(): Long
}
