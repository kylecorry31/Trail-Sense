package com.kylecorry.survival_aid.navigation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.kylecorry.survival_aid.R
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class NavigationActivity : AppCompatActivity(), Observer {

    private lateinit var compass: Compass
    private lateinit var gps: GPS

    private val DESTINATION_ARRIVED_THRESHOLD = 50 // m

    private lateinit var azimuthTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var needleImg: ImageView
    private lateinit var destinationStar: ImageView
    private lateinit var locationTxt: TextView
    private lateinit var navigationTxt: TextView

    private var destination: Coordinate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bearing)
        compass = Compass(this)
        gps = GPS(this)
        azimuthTxt = findViewById(R.id.compass_azimuth)
        directionTxt = findViewById(R.id.compass_direction)
        needleImg = findViewById(R.id.needle)
        destinationStar = findViewById(R.id.destination_star)
        locationTxt = findViewById(R.id.location)
        navigationTxt = findViewById(R.id.navigation)
    }

    override fun onResume() {
        super.onResume()
        compass.start()
        compass.addObserver(this)
        gps.addObserver(this)
        updateCompass()
        updateLocation()
    }

    override fun onPause() {
        super.onPause()
        stopNavigation()
        compass.stop()
        compass.deleteObserver(this)
        gps.deleteObserver(this)
    }

    /**
     * Start navigating to a destination
     */
    private fun startNavigation(destination: Coordinate){
        this.destination = destination
        gps.start()
        destinationStar.visibility = View.VISIBLE
        updateNavigation()
    }

    /**
     * Stop navigating
     */
    private fun stopNavigation(){
        gps.stop()
        navigationTxt.text = ""
        destinationStar.visibility = View.INVISIBLE
        this.destination = null
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == compass) updateCompass()
        if (o == gps) updateLocation()
    }

    private fun updateCompass(){
        val azimuthValue = compass.azimuth
        val azimuth = (azimuthValue.roundToInt() % 360).toString().padStart(3, ' ')
        val direction = compass.direction.symbol.toUpperCase().padEnd(2, ' ')
        azimuthTxt.text = "${azimuth}°"
        directionTxt.text = direction
        needleImg.rotation = -azimuthValue
        updateNavigation()
    }

    private fun updateNavigation(){
        val dest = destination
        val location = gps.location
        val azimuth = compass.azimuth

        location ?: return
        dest ?: return

        val distance = LocationMath.getDistance(location, dest)
        val bearing = LocationMath.getBearing(location, dest)

        if (distance <= DESTINATION_ARRIVED_THRESHOLD){
            // Arrived at the destination
            Toast.makeText(this, "Arrived", Toast.LENGTH_LONG).show()
            stopNavigation()
            return
        }

        val adjBearing = -azimuth - 90 + bearing
        val imgCenterX = needleImg.x + needleImg.width / 2f
        val imgCenterY = needleImg.y + needleImg.height / 2f
        val offsetX = -destinationStar.width / 2f
        val offsetY = -destinationStar.height / 2f
        val radius = needleImg.width / 2f + 64
        destinationStar.x = imgCenterX + offsetX + radius * cos(Math.toRadians(adjBearing.toDouble())).toFloat()
        destinationStar.y = imgCenterY + offsetY + radius * sin(Math.toRadians(adjBearing.toDouble())).toFloat()
        navigationTxt.text = "Destination:    ${bearing.roundToInt()}°    -    ${LocationMath.distanceToReadableString(distance, UnitSystem.IMPERIAL)}"
    }

    private fun updateLocation(){
        val location = gps.location
        if (location == null){
            locationTxt.text = getString(R.string.location_unknown)
            return
        }

        updateNavigation()

        locationTxt.text = "${gps.location?.latitude}, ${gps.location?.longitude}"
    }

    companion object {
        fun newIntent(ctx: Context): Intent {
            return Intent(ctx, NavigationActivity::class.java)
        }
    }

}
