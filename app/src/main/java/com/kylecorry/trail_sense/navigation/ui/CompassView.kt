package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageView
import com.kylecorry.trail_sense.shared.Alignment
import com.kylecorry.trail_sense.shared.alignTo
import com.kylecorry.trail_sense.shared.alignToVector
import kotlin.math.cos
import kotlin.math.sin

class CompassView(private val compass: ImageView, private val beaconIndicators: List<ImageView>, private val azimuthIndicator: ImageView) :
    ICompassView {

    override var visibility: Int
        get() = compass.visibility
        set(value){
            compass.visibility = value
            azimuthIndicator.visibility = value
        }

    override var azimuth: Float = 0f
        set(value) {
            compass.rotation = -value
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
        val imgCenterX = compass.x + compass.width / 2f
        val imgCenterY = compass.y + compass.height / 2f
        val radius = compass.width / 2f + 30
        displayDestinationBearing(indicator, adjBearing, imgCenterX, imgCenterY, radius)
    }

    private fun hideBeacon(indicator: ImageView){
        indicator.visibility = View.INVISIBLE
    }

    /**
     * Displays the destination bearing indicator around the compass
     * @param bearing the bearing in degrees to display the indicator at
     * @param centerX the center X position of the compass
     * @param centerY the center Y position of the compass
     * @param radius the radius to display the indicator at
     */
    private fun displayDestinationBearing(beaconIndicator: ImageView, bearing: Float, centerX: Float, centerY: Float, radius: Float){
        // Calculate the anchor offset
        val offsetX = -beaconIndicator.width / 2f
        val offsetY = -beaconIndicator.height / 2f

        // Update the position of the indicator
        beaconIndicator.x = centerX + offsetX + radius * cos(Math.toRadians(bearing.toDouble())).toFloat()
        beaconIndicator.y = centerY + offsetY + radius * sin(Math.toRadians(bearing.toDouble())).toFloat()

        // Make the indicator always rotated tangent to the compass
        beaconIndicator.rotation = bearing + 90
    }

}