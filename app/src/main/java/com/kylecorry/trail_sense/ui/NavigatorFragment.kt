package com.kylecorry.trail_sense.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.doTransaction
import com.kylecorry.trail_sense.navigation.Navigator
import com.kylecorry.trail_sense.navigation.Beacon
import com.kylecorry.trail_sense.navigation.BeaconDB
import com.kylecorry.trail_sense.shared.normalizeAngle
import com.kylecorry.trail_sense.shared.sensors.compass.Compass
import com.kylecorry.trail_sense.shared.sensors.gps.GPS
import com.kylecorry.trail_sense.navigation.LocationMath
import com.kylecorry.trail_sense.navigation.DeclinationCalculator
import com.kylecorry.trail_sense.shared.sensors.altimeter.BarometricAltimeter
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class NavigatorFragment(private val initialDestination: Beacon? = null) : Fragment(), Observer {

    private lateinit var compass: Compass
    private lateinit var gps: GPS
    private lateinit var navigator: Navigator
    private lateinit var barometer: BarometricAltimeter

    private var units = "meters"
    private var useTrueNorth = false
    private var useBarometricAltitude = false

    // UI Fields
    private lateinit var azimuthTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var needleImg: ImageView
    private lateinit var destinationStar: ImageView
    private lateinit var locationTxt: TextView
    private lateinit var navigationTxt: TextView
    private lateinit var beaconBtn: FloatingActionButton
    private lateinit var locationBtn: FloatingActionButton
    private lateinit var altitudeTxt: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_navigator, container, false)

        compass = Compass(context!!)
        gps = GPS(context!!)
        barometer = BarometricAltimeter(context!!)
        navigator = Navigator()
        if (initialDestination != null){
            navigator.destination = initialDestination
        }

        // Assign the UI fields
        azimuthTxt = view.findViewById(R.id.compass_azimuth)
        directionTxt = view.findViewById(R.id.compass_direction)
        needleImg = view.findViewById(R.id.needle)
        destinationStar = view.findViewById(R.id.destination_star)
        locationTxt = view.findViewById(R.id.location)
        navigationTxt = view.findViewById(R.id.navigation)
        beaconBtn = view.findViewById(R.id.beaconBtn)
        locationBtn = view.findViewById(R.id.locationBtn)
        altitudeTxt = view.findViewById(R.id.altitude)

        locationBtn.setOnClickListener {
            gps.updateLocation {
                gps.updateLocation()
            }
        }

        beaconBtn.setOnClickListener {
            // Open the navigation select screen
            // Allows user to choose destination from list or add a destination to the list
            if (!navigator.hasDestination){
                fragmentManager?.doTransaction {
                    this.addToBackStack(null)
                    this.replace(R.id.fragment_holder,
                        BeaconListFragment(
                            BeaconDB(context!!), gps
                        )
                    )
                }
            } else {
                navigator.destination = null
            }

        }
        return view
    }

    override fun onResume() {
        super.onResume()
        // Observer the sensors
        compass.addObserver(this)
        gps.addObserver(this)
        navigator.addObserver(this)
        barometer.addObserver(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        useTrueNorth = prefs.getBoolean(getString(R.string.pref_use_true_north), false)
        useBarometricAltitude = prefs.getString(getString(R.string.pref_altitude_mode), "gps") == "barometer"
        units = prefs.getString(getString(R.string.pref_distance_units), "meters") ?: "meters"

        if (useTrueNorth){
            compass.declination = DeclinationCalculator()
                .calculateDeclination(gps.location, gps.altitude)
        } else {
            compass.declination = 0f
        }

        if (useBarometricAltitude){
            barometer.start()
            barometer.setAltitude(gps.altitude.value)
            gps.updateLocation {
                barometer.setAltitude(gps.altitude.value)
            }
        } else {
            gps.start()
        }

        compass.start()

        // Update the UI
        updateNavigator()
        updateCompassUI()
        updateLocationUI()
    }

    override fun onPause() {
        super.onPause()
        // Stop the low level sensors
        compass.stop()
        gps.stop()
        barometer.stop()

        // Remove the observers
        compass.deleteObserver(this)
        gps.deleteObserver(this)
        navigator.deleteObserver(this)
        barometer.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == compass) updateCompassUI()
        if (o == gps) updateLocationUI()
        if (o == navigator) updateNavigator()
        if (o == barometer) updateLocationUI()
    }

    /**
     * Update the navigator
     */
    private fun updateNavigator(){
        if (navigator.hasDestination) {
            // Navigating
            gps.start()
            beaconBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_cancel))
            updateNavigationUI()
        } else {
            // Not navigating
            if (useBarometricAltitude) {
                gps.stop()
            }
            beaconBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_beacon))
            updateNavigationUI()
        }
    }

    /**
     * Update the compass
     */
    private fun updateCompassUI(){
        // Update the text boxes
        val azimuth = (compass.azimuth.value.roundToInt() % 360).toString().padStart(3, ' ')
        val direction = compass.direction.symbol.toUpperCase(Locale.getDefault()).padEnd(2, ' ')
        azimuthTxt.text = "${azimuth}°"
        directionTxt.text = direction

        // Rotate the compass
        needleImg.rotation = -compass.azimuth.value

        // Update the navigation
        updateNavigationUI()
    }

    /**
     * Update the navigation
     */
    private fun updateNavigationUI(){
        // Determine if the navigator is navigating
        if (!navigator.hasDestination){
            // Hide the navigation indicators
            destinationStar.visibility = View.INVISIBLE
            navigationTxt.text = ""
            return
        }

        val declination = DeclinationCalculator()
            .calculateDeclination(gps.location, gps.altitude)

        // Display the indicator
        destinationStar.visibility = View.VISIBLE

        // Retrieve the current location and azimuth
        val location = gps.location
        val azimuth = compass.azimuth

        // Get the distance to the bearing
        val distance = navigator.getDistance(location)
        var bearing = navigator.getBearing(location)

        // The bearing is already in true north format, convert that to magnetic north
        if (!useTrueNorth) bearing -= declination
        bearing = normalizeAngle(bearing)

        // Display the direction indicator
        val adjBearing = -azimuth.value - 90 + bearing
        val imgCenterX = needleImg.x + needleImg.width / 2f
        val imgCenterY = needleImg.y + needleImg.height / 2f
        val radius = needleImg.width / 2f + 30
        displayDestinationBearing(adjBearing, imgCenterX, imgCenterY, radius)

        // Update the direction text
        navigationTxt.text = "${navigator.getDestinationName()}:    ${bearing.roundToInt()}°    -    ${LocationMath.distanceToReadableString(distance, units)}"
    }

    /**
     * Displays the destination bearing indicator around the compass
     * @param bearing the bearing in degrees to display the indicator at
     * @param centerX the center X position of the compass
     * @param centerY the center Y position of the compass
     * @param radius the radius to display the indicator at
     */
    private fun displayDestinationBearing(bearing: Float, centerX: Float, centerY: Float, radius: Float){
        // Calculate the anchor offset
        val offsetX = -destinationStar.width / 2f
        val offsetY = -destinationStar.height / 2f

        // Update the position of the indicator
        destinationStar.x = centerX + offsetX + radius * cos(Math.toRadians(bearing.toDouble())).toFloat()
        destinationStar.y = centerY + offsetY + radius * sin(Math.toRadians(bearing.toDouble())).toFloat()

        // Make the indicator always rotated tangent to the compass
        destinationStar.rotation = bearing + 90
    }

    /**
     * Update the current location
     */
    private fun updateLocationUI(){

        // Update the declination value
        if (useTrueNorth){
            compass.declination = DeclinationCalculator()
                .calculateDeclination(gps.location, gps.altitude)
        } else {
            compass.declination = 0f
        }


        val location = gps.location

        // Update the latitude, longitude display
        locationTxt.text = location.toString()

        val altitude = if (useBarometricAltitude){
            barometer.altitude
        } else {
            gps.altitude
        }

        altitudeTxt.text = "Altitude ${getAltitudeString(altitude.value, units)}"

        // Update the navigation display
        updateNavigationUI()
    }

    private fun getAltitudeString(altitude: Float, units: String): String {
        if (units == "meters"){
            return "${altitude.roundToInt()} m"
        } else {
            return "${LocationMath.convertToBaseUnit(altitude, units).roundToInt()} ft"
        }
    }

}
