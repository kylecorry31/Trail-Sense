package com.kylecorry.trail_sense.shared.alerts

interface IValueAlerter<T> {

    fun alert(value: T)
}
