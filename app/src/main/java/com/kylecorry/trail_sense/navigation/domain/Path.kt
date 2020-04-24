package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.trail_sense.shared.Coordinate

data class Path(val name: String, val wayPoints: List<Coordinate>){
    fun reversed(): Path {
        return Path(name, wayPoints.reversed())
    }
}