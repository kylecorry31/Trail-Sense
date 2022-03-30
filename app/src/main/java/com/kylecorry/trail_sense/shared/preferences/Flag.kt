package com.kylecorry.trail_sense.shared.preferences

interface Flag {

    fun set(shown: Boolean)

    fun get(): Boolean

}