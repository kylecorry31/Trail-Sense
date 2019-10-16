package com.kylecorry.survival_aid.navigator

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.survival_aid.MainActivity
import com.kylecorry.survival_aid.R
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class NavigatorFragment : Fragment(), Observer {

    private lateinit var compass: Compass
    private lateinit var gps: GPS

    private val DESTINATION_ARRIVED_THRESHOLD = 50 // m

    private lateinit var azimuthTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var needleImg: ImageView
    private lateinit var destinationStar: ImageView
    private lateinit var locationTxt: TextView
    private lateinit var navigationTxt: TextView
    private lateinit var navigationBtn: FloatingActionButton
    private lateinit var locationBtn: FloatingActionButton

    private var destination: Coordinate? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_navigator, container, false)

        // Load the destination from the intent if available
//        if (intent.hasExtra(INTENT_DESTINATION_LNG)){
//            val lat = intent.getFloatExtra(INTENT_DESTINATION_LAT, 0f)
//            val lng = intent.getFloatExtra(INTENT_DESTINATION_LNG, 0f)
//            destination = Coordinate(lat, lng)
//        }


        compass = Compass(context!!)
        gps = GPS(context!!)
        azimuthTxt = view.findViewById(R.id.compass_azimuth)
        directionTxt = view.findViewById(R.id.compass_direction)
        needleImg = view.findViewById(R.id.needle)
        destinationStar = view.findViewById(R.id.destination_star)
        locationTxt = view.findViewById(R.id.location)
        navigationTxt = view.findViewById(R.id.navigation)
        navigationBtn = view.findViewById(R.id.navigateBtn)
        locationBtn = view.findViewById(R.id.locationBtn)

        locationBtn.setOnClickListener {
            gps.updateLocation()
        }

        navigationBtn.setOnClickListener {
            // Open the navigation select screen
            // Allows user to choose destination from list or add a destination to the list
            if (destination == null){
                startNavigation(Coordinate(0f, 0f))
            } else {
                stopNavigation()
            }

        }
        return view
    }

    override fun onResume() {
        super.onResume()
        compass.start()
        compass.addObserver(this)
        gps.addObserver(this)
        gps.updateLocation()
        val dest = destination
        if (dest != null) startNavigation(dest)
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
        navigationBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_cancel))
        updateNavigation()
    }

    /**
     * Stop navigating
     */
    private fun stopNavigation(){
        gps.stop()
        navigationTxt.text = ""
        destinationStar.visibility = View.INVISIBLE
        navigationBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_navigation))
        this.destination = null
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == compass) updateCompass()
        if (o == gps) updateLocation()
    }

    /**
     * Update the compass
     */
    private fun updateCompass(){
        val azimuthValue = compass.azimuth
        val azimuth = (azimuthValue.roundToInt() % 360).toString().padStart(3, ' ')
        val direction = compass.direction.symbol.toUpperCase().padEnd(2, ' ')
        azimuthTxt.text = "${azimuth}°"
        directionTxt.text = direction
        needleImg.rotation = -azimuthValue
        updateNavigation()
    }

    /**
     * Update the navigation
     */
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
            Toast.makeText(context!!, getString(R.string.arrived), Toast.LENGTH_LONG).show()
            stopNavigation()
            return
        }

        val adjBearing = -azimuth - 90 + bearing
        val imgCenterX = needleImg.x + needleImg.width / 2f
        val imgCenterY = needleImg.y + needleImg.height / 2f
        val offsetX = -destinationStar.width / 2f
        val offsetY = -destinationStar.height / 2f
        val radius = needleImg.width / 2f + 30
        destinationStar.x = imgCenterX + offsetX + radius * cos(Math.toRadians(adjBearing.toDouble())).toFloat()
        destinationStar.y = imgCenterY + offsetY + radius * sin(Math.toRadians(adjBearing.toDouble())).toFloat()
        destinationStar.rotation = adjBearing + 90 // Make the star always rotated tangent to the compass
        navigationTxt.text = "Destination:    ${bearing.roundToInt()}°    -    ${LocationMath.distanceToReadableString(distance, UnitSystem.IMPERIAL)}"
    }

    /**
     * Update the current location
     */
    private fun updateLocation(){
        val location = gps.location
        if (location == null){
            locationTxt.text = getString(R.string.location_unknown)
            return
        }

        updateNavigation()

        locationTxt.text = "${gps.location?.latitude}, ${gps.location?.longitude}"
    }
}
