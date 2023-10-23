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