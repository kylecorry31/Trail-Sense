package com.kylecorry.trail_sense.tools.navigation.domain

enum class PathNavigationMode(val isReversed: Boolean, val isFullLoop: Boolean) {
    TO_END(false, false),
    REVERSED_TO_END(true, false),
    FULL_LOOP(false, true),
    REVERSED_FULL_LOOP(true, true)
}
