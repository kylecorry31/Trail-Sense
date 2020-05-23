package com.kylecorry.trail_sense.shared

import android.view.View
import com.kylecorry.trail_sense.shared.math.cosDegrees
import com.kylecorry.trail_sense.shared.math.sinDegrees

enum class Alignment {
    StartToStart,
    StartToEnd,
    EndToStart,
    EndToEnd,
    Center
}

fun alignTo(to: View, viewToAlign: View, vertical: Alignment = Alignment.Center, horizontal: Alignment = Alignment.Center,  verticalOffset: Float = 0f, horizontalOffset: Float = 0f){
    when(vertical){
        Alignment.StartToStart -> viewToAlign.y = to.y
        Alignment.StartToEnd -> viewToAlign.y = to.y + to.height
        Alignment.EndToStart -> viewToAlign.y = to.y - viewToAlign.height
        Alignment.EndToEnd -> viewToAlign.y = to.y - viewToAlign.height + to.height
        Alignment.Center -> viewToAlign.y = to.y + to.height / 2f - viewToAlign.height / 2f
    }

    when(horizontal){
        Alignment.StartToStart -> viewToAlign.x = to.x
        Alignment.StartToEnd -> viewToAlign.x = to.x + to.width
        Alignment.EndToStart -> viewToAlign.x = to.x - viewToAlign.width
        Alignment.EndToEnd -> viewToAlign.x = to.x - viewToAlign.width + to.width
        Alignment.Center -> viewToAlign.x = to.x + to.width / 2f - viewToAlign.width / 2f
    }

    viewToAlign.x += horizontalOffset
    viewToAlign.y += verticalOffset
}

fun alignToVector(to: View, viewToAlign: View, radius: Float, angle: Float){
    alignTo(to, viewToAlign, Alignment.Center, Alignment.Center)
    viewToAlign.x -= cosDegrees(angle.toDouble()).toFloat() * radius
    viewToAlign.y -= sinDegrees(angle.toDouble()).toFloat() * radius
}