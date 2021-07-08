package com.kylecorry.trail_sense.shared

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.units.Distance
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.roundToInt

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

fun LocalDateTime.toEpochMillis(): Long {
    return this.toZonedDateTime().toEpochSecond() * 1000
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

fun Distance.dividedBy(value: Float): Distance {
    return Distance(distance / value, units)
}

fun Distance.times(value: Float): Distance {
    return Distance(distance * value, units)
}

@ColorInt
fun gray(value: Int): Int {
    return Color.rgb(value, value, value)
}

fun Fragment.requireMainActivity(): MainActivity {
    return requireActivity() as MainActivity
}

fun Fragment.requireBottomNavigation(): BottomNavigationView {
    return requireActivity().findViewById(R.id.bottom_navigation)
}