package com.kylecorry.trail_sense.shared

interface StatusChecker {
    fun isEnabled(): Boolean
    fun isAvailable(): Boolean
}