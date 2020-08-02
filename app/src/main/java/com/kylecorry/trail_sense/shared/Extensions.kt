package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.kylecorry.trail_sense.R
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.roundToInt

inline fun FragmentManager.doTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

fun Double.roundPlaces(places: Int): Double {
    return (this * 10.0.pow(places)).roundToInt() / 10.0.pow(places)
}

fun Float.roundPlaces(places: Int): Float {
    return (this * 10f.pow(places)).roundToInt() / 10f.pow(places)
}

fun Instant.toZonedDateTime(): ZonedDateTime {
    return ZonedDateTime.ofInstant(this, ZoneId.systemDefault())
}

fun LocalDateTime.toDisplayFormat(ctx: Context): String {
    val prefs = UserPreferences(ctx)
    val use24Hr = prefs.use24HourTime

    return if (use24Hr) {
        this.format(DateTimeFormatter.ofPattern("H:mm"))
    } else {
        this.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
}

fun LocalTime.toDisplayFormat(ctx: Context, hourOnly: Boolean = false): String {
    val prefs = UserPreferences(ctx)
    val use24Hr = prefs.use24HourTime

    return if (hourOnly) {
        if (use24Hr) {
            this.format(DateTimeFormatter.ofPattern("H"))
        } else {
            this.format(DateTimeFormatter.ofPattern("h a"))
        }
    } else {
        if (use24Hr) {
            this.format(DateTimeFormatter.ofPattern("H:mm"))
        } else {
            this.format(DateTimeFormatter.ofPattern("h:mm a"))
        }
    }
}

fun Duration.formatHM(short: Boolean = false): String {
    val hours = this.toHours()
    val minutes = this.toMinutes() % 60

    return if (short){
        when (hours) {
            0L -> "${minutes}m"
            else -> "${hours}h"
        }
    } else {
        when {
            hours == 0L -> "${minutes}m"
            minutes == 0L -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }
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

fun Fragment.switchToFragment(fragment: Fragment, holderId: Int = R.id.fragment_holder, addToBackStack: Boolean = false) {
    parentFragmentManager.doTransaction {
        if (addToBackStack) {
            this.addToBackStack(null)
        }
        this.replace(
            holderId,
            fragment
        )
    }
}

fun Float.toDegrees(): Float {
    return Math.toDegrees(this.toDouble()).toFloat()
}

fun LocalDateTime.roundNearestMinute(minutes: Long): LocalDateTime {
    val minute = this.minute
    val newMinute = (minute / minutes) * minutes

    val diff = newMinute - minute
    return this.plusMinutes(diff)
}