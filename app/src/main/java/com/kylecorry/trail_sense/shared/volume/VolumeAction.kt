package com.kylecorry.trail_sense.shared.volume

interface VolumeAction {
    fun onButtonPress(isUpButton: Boolean): Boolean
    fun onButtonRelease(isUpButton: Boolean): Boolean
}