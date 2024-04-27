package com.kylecorry.trail_sense.shared.volume

interface VolumeAction {
    fun onButtonPress(): Boolean
    fun onButtonRelease(): Boolean
}