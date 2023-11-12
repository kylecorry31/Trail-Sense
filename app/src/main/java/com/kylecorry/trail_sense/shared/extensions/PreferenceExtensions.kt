package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.sol.units.Coordinate

fun IPreferences.putOrRemoveFloat(key: String, value: Float?) {
    if (value == null) {
        remove(key)
    } else {
        putFloat(key, value)
    }
}

fun IPreferences.putOrRemoveCoordinate(key: String, value: Coordinate?) {
    if (value == null) {
        remove(key)
    } else {
        putCoordinate(key, value)
    }
}

fun IPreferences.putIntArray(key: String, value: List<Int>) {
    putString(key, value.joinToString(","))
}

fun IPreferences.getIntArray(key: String): List<Int>? {
    return getString(key)?.split(",")?.mapNotNull { it.toIntOrNull() }
}

fun IPreferences.putLongArray(key: String, value: List<Long>) {
    putString(key, value.joinToString(","))
}

fun IPreferences.getLongArray(key: String): List<Long>? {
    return getString(key)?.split(",")?.mapNotNull { it.toLongOrNull() }
}