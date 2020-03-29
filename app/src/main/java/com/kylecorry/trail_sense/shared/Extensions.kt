package com.kylecorry.trail_sense.shared

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import java.time.*
import kotlin.math.pow
import kotlin.math.roundToInt

inline fun FragmentManager.doTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

fun Float.roundPlaces(places: Int): Float {
    val newFloat = (this * 10.0.pow(places)).roundToInt() / 10.0.pow(places)
    return newFloat.toFloat()
}

fun Double.roundPlaces(places: Int): Double {
    return (this * 10.0.pow(places)).roundToInt() / 10.0.pow(places)
}

fun Double.toRadians(): Double {
    return Math.toRadians(this)
}

fun Float.roundNearest(value: Float): Float {
    return (this / value).roundToInt() * value
}

fun Instant.toZonedDateTime(): ZonedDateTime {
    return ZonedDateTime.ofInstant(this, ZoneId.systemDefault())
}

fun LocalDateTime.toZonedInstant(): Instant {
    return ZonedDateTime.of(this, ZoneId.systemDefault()).toInstant()
}

fun LocalDateTime.toZonedDateTime(): ZonedDateTime {
    return ZonedDateTime.of(this, ZoneId.systemDefault())
}

fun ZonedDateTime.toUTCLocal(): LocalDateTime {
    return LocalDateTime.ofInstant(this.toInstant(), ZoneId.of("UTC"))
}

fun List<Float>.median(): Float {
    if (this.isEmpty()) return 0f

    val sortedList = this.sortedBy { it }
    return sortedList[this.size / 2]
}

fun List<Double>.median(): Double {
    if (this.isEmpty()) return 0.0

    val sortedList = this.sortedBy { it }
    return sortedList[this.size / 2]
}

fun Int.toHexColor(): String {
    return String.format("#%06X", 0xFFFFFF and this)
}