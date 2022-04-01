package com.kylecorry.trail_sense.shared.alerts

interface IValueAlerter<T> {

    /**
     * Show an alert
     */
    fun alert(data: T)
}