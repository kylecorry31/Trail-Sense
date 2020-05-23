package com.kylecorry.trail_sense.shared

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.infrastructure.BeaconDB
import com.kylecorry.trail_sense.navigation.ui.BeaconListFragment
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
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

fun Float.roundNearest(nearest: Float): Float {
    return (this / nearest).roundToInt() * nearest
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

fun Duration.formatHM(): String {
    val hours = this.toHours()
    val minutes = this.toMinutes() % 60

    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
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

fun View.getCenterX(): Float {
    return (left + right) / 2f
}

fun View.getCenterY(): Float {
    return (top + bottom) / 2f
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