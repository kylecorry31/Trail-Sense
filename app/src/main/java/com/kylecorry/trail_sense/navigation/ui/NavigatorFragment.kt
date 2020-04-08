package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.Navigator
import com.kylecorry.trail_sense.navigation.domain.compass.DeclinationCalculator
import com.kylecorry.trail_sense.navigation.domain.compass.OrientationCompass
import com.kylecorry.trail_sense.navigation.infrastructure.BeaconDB
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.shared.doTransaction
import com.kylecorry.trail_sense.shared.math.normalizeAngle
import com.kylecorry.trail_sense.shared.sensors.altimeter.BarometricAltimeter
import com.kylecorry.trail_sense.shared.sensors.gps.GPS
import java.util.*
import kotlin.math.roundToInt

class NavigatorFragment(private val initialDestination: Beacon? = null) : Fragment(), Observer {

    private lateinit var compass: OrientationCompass
    private lateinit var gps: GPS
    private lateinit var navigator: Navigator
    private lateinit var altimeter: BarometricAltimeter

    private var units = "meters"
    private var useTrueNorth = false
    private var altimeterMode = NavigationPreferences.AltimeterMode.GPS

    // UI Fields
    private lateinit var azimuthTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var locationTxt: TextView
    private lateinit var navigationTxt: TextView
    private lateinit var beaconBtn: FloatingActionButton
    private lateinit var altitudeTxt: TextView
    private lateinit var compassView: CompassView
    private lateinit var prefs: NavigationPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_navigator, container, false)

        prefs = NavigationPreferences(context!!)

        compass = OrientationCompass(context!!)
        gps = GPS(context!!)
        altimeter = BarometricAltimeter(context!!)
        navigator = Navigator()
        if (initialDestination != null){
            navigator.destination = initialDestination
        }

        // Assign the UI fields
        azimuthTxt = view.findViewById(R.id.compass_azimuth)
        directionTxt = view.findViewById(R.id.compass_direction)
        locationTxt = view.findViewById(R.id.location)
        navigationTxt = view.findViewById(R.id.navigation)
        beaconBtn = view.findViewById(R.id.beaconBtn)
        altitudeTxt = view.findViewById(R.id.altitude)

        compassView = CompassView(
            view.findViewById(R.id.needle),
            view.findViewById(R.id.destination_star),
            view.findViewById(R.id.azimuth_indicator)
        )

        beaconBtn.setOnClickListener {
            // Open the navigation select screen
            // Allows user to choose destination from list or add a destination to the list
            if (!navigator.hasDestination){
                fragmentManager?.doTransaction {
                    this.addToBackStack(null)
                    this.replace(R.id.fragment_holder,
                        BeaconListFragment(
                            BeaconDB(
                                context!!
                            ), gps
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
        altimeter.addObserver(this)

        useTrueNorth = prefs.useTrueNorth
        altimeterMode = prefs.altimeter
        units = prefs.distanceUnits

        if (useTrueNorth){
            compass.declination = DeclinationCalculator()
                .calculateDeclination(gps.location, gps.altitude)
        } else {
            compass.declination = 0f
        }

        if (altimeterMode == NavigationPreferences.AltimeterMode.GPS){
            if (gps.altitude.value != 0.0f) {
                altimeter.setAltitude(gps.altitude.value)
            }
            gps.updateLocation {
                gps.updateLocation {
                    altimeter.setAltitude(gps.altitude.value)
                }
            }
        } else {
            altimeter.setAltitudeFromSeaLevel()
        }

        altimeter.start()

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
        altimeter.stop()

        // Remove the observers
        compass.deleteObserver(this)
        gps.deleteObserver(this)
        navigator.deleteObserver(this)
        altimeter.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == compass) updateCompassUI()
        if (o == gps) updateLocationUI()
        if (o == navigator) updateNavigator()
        if (o == altimeter) updateLocationUI()
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
            beaconBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_beacon))
            updateNavigationUI()
        }
    }

    /**
     * Update the compass
     */
    private fun updateCompassUI() {
        // Update the text boxes
        val azimuth = (compass.azimuth.value.roundToInt() % 360).toString().padStart(3, ' ')
        val direction = compass.direction.symbol.toUpperCase(Locale.getDefault()).padEnd(2, ' ')
        azimuthTxt.text = "${azimuth}°"
        directionTxt.text = direction

        // Rotate the compass
        compassView.setAzimuth(compass.azimuth.value)

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
            compassView.hideBeacon()
            navigationTxt.text = ""
            return
        }

        val declination = DeclinationCalculator()
            .calculateDeclination(gps.location, gps.altitude)

        // Retrieve the current location and azimuth
        val location = gps.location

        // Get the distance to the bearing
        val distance = navigator.getDistance(location)
        var bearing = navigator.getBearing(location)

        // The bearing is already in true north format, convert that to magnetic north
        if (!useTrueNorth) bearing -= declination
        bearing = normalizeAngle(bearing)

        // Display the direction indicator
        compassView.showBeacon(bearing)
        // Update the direction text
        navigationTxt.text = "${navigator.getDestinationName()}:    ${bearing.roundToInt()}°    -    ${LocationMath.distanceToReadableString(distance, units)}"
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

        val altitude = altimeter.altitude

        altitudeTxt.text = "Altitude ${getAltitudeString(altitude.value, units)}"

        // Update the navigation display
        updateNavigationUI()
    }

    private fun getAltitudeString(altitude: Float, units: String): String {
        return if (units == "meters"){
            "${altitude.roundToInt()} m"
        } else {
            "${LocationMath.convertToBaseUnit(altitude, units).roundToInt()} ft"
        }
    }

}
