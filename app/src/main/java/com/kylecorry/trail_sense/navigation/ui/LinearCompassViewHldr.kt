package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageView
import com.kylecorry.trailsensecore.domain.math.deltaAngle
import com.kylecorry.trailsensecore.infrastructure.system.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class LinearCompassViewHldr(private val compass: LinearCompassView, private val beaconIndicators: List<ImageView>) :
    ICompassView {

    override var visibility: Int
        get() = compass.visibility
        set(value){
            compass.visibility = value
        }

    override var azimuth: Float = 0f
        set(value) {
            compass.setDegrees(value)
            field = value
        }

    override var beacons: List<Float> = listOf()
        set(value) {
            beaconIndicators.forEachIndexed { index, indicator ->
                if (index < beacons.size){
                    showBeacon(indicator, beacons[index], index <= 1)
                } else {
                    hideBeacon(indicator)
                }
            }
            field = value
        }

    override fun setOnClickListener(fn: () -> Unit) {
        compass.setOnClickListener { fn.invoke() }
    }

    override fun setIndicators(indicators: List<BearingIndicator>) {
        TODO("Not yet implemented")
    }

    private fun showBeacon(indicator: ImageView, bearing: Float, isSunOrMoon: Boolean){
        indicator.rotation = 0f

        val delta = deltaAngle(azimuth.roundToInt().toFloat(), bearing.roundToInt().toFloat())
        val margin = if (isSunOrMoon) indicator.height / 2f else 0f

        when {
            delta < -90 -> {
                align(indicator,
                    VerticalConstraint(compass, VerticalConstraintType.Top, margin),
                    HorizontalConstraint(compass, HorizontalConstraintType.Left),
                    null,
                    null
                )
                indicator.rotation = -90f
                if (isSunOrMoon){
                    indicator.rotation = 0f
                }
            }
            delta > 90 -> {
                align(indicator,
                    VerticalConstraint(compass, VerticalConstraintType.Top, margin),
                    null,
                    null,
                    HorizontalConstraint(compass, HorizontalConstraintType.Right)
                )
                indicator.rotation = 90f
                if (isSunOrMoon){
                    indicator.rotation = 0f
                }
            }
            else -> {
                val min = (azimuth - 90f).roundToInt().toFloat()
                val max = (azimuth + 90f).roundToInt().toFloat()
                val deltaMin = deltaAngle(bearing, min).absoluteValue / (max - min)
                align(indicator,
                    VerticalConstraint(compass, VerticalConstraintType.Top, margin),
                    HorizontalConstraint(compass, HorizontalConstraintType.Left),
                    null,
                    HorizontalConstraint(compass, HorizontalConstraintType.Right),
                    0.5f,
                    deltaMin
                )
            }
        }

        indicator.visibility = View.VISIBLE
    }

    private fun hideBeacon(indicator: ImageView){
        indicator.visibility = View.INVISIBLE
    }
}