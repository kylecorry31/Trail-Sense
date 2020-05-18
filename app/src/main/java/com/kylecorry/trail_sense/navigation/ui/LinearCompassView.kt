package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageView
import com.redinput.compassview.CompassView

class LinearCompassView(private val compass: CompassView, private val beaconIndicator: ImageView) :
    ICompassView {

    override var visibility: Int
        get() = compass.visibility
        set(value){
            compass.visibility = value
            if (beacon != null) {
                beaconIndicator.visibility = value
            }
        }

    override var azimuth: Float = 0f
        set(value) {
            compass.setDegrees(value)
            field = value
        }

    override var beacon: Float? = 0f
        set(value) {
            if (value == null){
                hideBeacon()
            } else {
                // Show
            }
            field = value
        }

    private fun showBeacon(bearing: Float){
//        val adjBearing = -lastAzimuth - 90 + bearing
//        beaconIndicator.visibility = visibility
//        val imgCenterX = compass.x + compass.width / 2f
//        val imgCenterY = compass.y + compass.height / 2f
//        val radius = compass.width / 2f + 30
//        displayDestinationBearing(adjBearing, imgCenterX, imgCenterY, radius)
//        hasBeacon = true
    }

    private fun hideBeacon(){
        beaconIndicator.visibility = View.INVISIBLE
    }
}