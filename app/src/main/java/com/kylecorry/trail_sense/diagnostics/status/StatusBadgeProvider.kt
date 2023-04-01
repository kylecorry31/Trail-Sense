package com.kylecorry.trail_sense.diagnostics.status

interface StatusBadgeProvider {
    fun getBadge(): StatusBadge
}