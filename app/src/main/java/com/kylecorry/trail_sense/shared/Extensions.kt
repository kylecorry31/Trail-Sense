package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.units.Distance
import java.time.*
import java.time.format.DateTimeFormatter

fun LocalDateTime.toDisplayFormat(ctx: Context): String {
    val formatService = FormatServiceV2(ctx)
    return formatService.formatTime(toLocalTime(), false)
}

// TODO: Replace this with format service
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

fun Distance.times(value: Float): Distance {
    return Distance(distance * value, units)
}

fun Fragment.requireMainActivity(): MainActivity {
    return requireActivity() as MainActivity
}

fun Fragment.requireBottomNavigation(): BottomNavigationView {
    return requireActivity().findViewById(R.id.bottom_navigation)
}