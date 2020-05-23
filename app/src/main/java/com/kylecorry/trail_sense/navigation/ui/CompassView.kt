package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageView
import com.kylecorry.trail_sense.shared.alignToVector

class CompassView(private val compass: ImageView, private val beaconIndicators: List<ImageView>, private val azimuthIndicator: ImageView) :
    ICompassView {

    override var visibility: Int
        get() = compass.visibility
        set(value){
            compass.visibility = value
            azimuthIndicator.visibility = value
            if (azimuthIndicator.height == 0){
                azimuthIndicator.visibility = View.INVISIBLE
            }
        }

    override var azimuth: Float = 0f
        set(value) {
            compass.rotation = -value
            if (azimuthIndicator.height != 0){
                azimuthIndicator.visibility = visibility
            }
            alignToVector(compass, azimuthIndicator, compass.width / 2f + azimuthIndicator.height / 4f, 90f)
            field = value
        }

    override var beacons: List<Float> = listOf()
        set(value) {
            beaconIndicators.forEachIndexed { index, indicator ->
                if (index < beacons.size){
                    showBeacon(indicator, beacons[index])
                } else {
                    hideBeacon(indicator)
                }
            }
            field = value
        }

    private fun showBeacon(indicator: ImageView, bearing: Float){
        val adjBearing = -azimuth - 90 + bearing
        indicator.visibility = visibility
        displayDestinationBearing(indicator, adjBearing)
    }

    private fun hideBeacon(indicator: ImageView){
        indicator.visibility = View.INVISIBLE
    }

    /**
     * Displays the destination bearing indicator around the compass
     * @param bearing the bearing in degrees to display the indicator at
     */
    private fun displayDestinationBearing(beaconIndicator: ImageView, bearing: Float){
        alignToVector(compass, beaconIndicator, compass.width / 2f + beaconIndicator.height / 4f, bearing)
        beaconIndicator.rotation = bearing - 90
    }

}