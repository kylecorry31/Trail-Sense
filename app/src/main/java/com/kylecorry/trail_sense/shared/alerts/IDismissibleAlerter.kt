package com.kylecorry.trail_sense.shared.alerts

interface IDismissibleAlerter : IAlerter {
    /**
     * Dismiss the alert
     */
    fun dismiss()
}