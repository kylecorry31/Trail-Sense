package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.shared.views.CanvasView
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.math.cosDegrees
import com.kylecorry.trailsensecore.domain.math.sinDegrees
import com.kylecorry.trailsensecore.domain.math.wrap
import com.kylecorry.trailsensecore.domain.pixels.PixelCoordinate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class HeightMapView: CanvasView {

    private var myLocation: Coordinate? = null
    private val prefs by lazy { UserPreferences(context) }
    private val metersPerPixel by lazy { prefs.navigation.maxBeaconDistance / width }

    private var minAltitude = 0f
    private var maxAltitude = 200f

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        runEveryCycle = false
    }

    override fun setup() {
    }

    override fun draw() {
        background(color(127))
        val myLocation = myLocation ?: return

        for (location in heightMap){
            val pixel = coordinateToPixel(myLocation, location.key)
            if (pixel.x < 0 || pixel.x > width || pixel.y < 0 || pixel.y > height){
                continue
            }
            val altitudeColor = (510 * ((location.value - minAltitude) / (maxAltitude - minAltitude))).roundToInt()
            noFill()
            stroke(color(max(altitudeColor - 255, 0), 0, 255 - altitudeColor % 255))
            strokeWeight(10f)
            strokeCap(StrokeCap.Square)
            point(pixel.x, pixel.y)
        }
    }

    private fun coordinateToPixel(myLocation: Coordinate, coordinate: Coordinate): PixelCoordinate {
        val distance = myLocation.distanceTo(coordinate)
        val bearing = myLocation.bearingTo(coordinate)
        val angle = wrap(-(bearing.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return PixelCoordinate(width / 2f + xDiff, height / 2f - yDiff)
    }

    fun setMyLocation(location: Coordinate, altitude: Float){
        val truncatedLocation = Coordinate(location.latitude.roundPlaces(4), location.longitude.roundPlaces(4))
        heightMap[truncatedLocation] = altitude
        myLocation = location
        minAltitude = min(altitude, minAltitude)
        maxAltitude = max(altitude, maxAltitude)
        invalidate()
    }


    companion object {
        private val heightMap = mutableMapOf<Coordinate, Float>()
    }


}