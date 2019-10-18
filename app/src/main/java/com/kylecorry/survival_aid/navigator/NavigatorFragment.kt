package com.kylecorry.survival_aid.navigator

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.survival_aid.R
import com.kylecorry.survival_aid.doTransaction
import kotlinx.android.synthetic.main.activity_navigator.*
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class NavigatorFragment(private val initialDestination: Beacon? = null) : Fragment(), Observer {

    private lateinit var compass: Compass
    private lateinit var gps: GPS
    private lateinit var navigator: Navigator
    private val unitSystem = UnitSystem.IMPERIAL

    // UI Fields
    private lateinit var azimuthTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var needleImg: ImageView
    private lateinit var destinationStar: ImageView
    private lateinit var locationTxt: TextView
    private lateinit var accuracyTxt: TextView
    private lateinit var navigationTxt: TextView
    private lateinit var beaconBtn: FloatingActionButton
    private lateinit var locationBtn: FloatingActionButton
    private lateinit var trueNorthBtn: SwitchCompat

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_navigator, container, false)

        compass = Compass(context!!)
        gps = GPS(context!!)
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
        accuracyTxt = view.findViewById(R.id.location_accuracy)
        navigationTxt = view.findViewById(R.id.navigation)
        beaconBtn = view.findViewById(R.id.beaconBtn)
        locationBtn = view.findViewById(R.id.locationBtn)
        trueNorthBtn = view.findViewById(R.id.true_north)

        locationBtn.setOnClickListener {
            gps.updateLocation()
        }

        beaconBtn.setOnClickListener {
            // Open the navigation select screen
            // Allows user to choose destination from list or add a destination to the list
            if (!navigator.hasDestination){
                fragmentManager?.doTransaction {
                    this.addToBackStack(null)
                    this.replace(R.id.fragment_holder, BeaconListFragment(BeaconDB(context!!), gps))
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

        // Start the low level sensors
        compass.start()
        gps.updateLocation()

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

        // Remove the observers
        compass.deleteObserver(this)
        gps.deleteObserver(this)
        navigator.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == compass) updateCompassUI()
        if (o == gps) updateLocationUI()
        if (o == navigator) updateNavigator()
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
            gps.stop()
            beaconBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_beacon))
            updateNavigationUI()
        }
    }

    /**
     * Update the compass
     */
    private fun updateCompassUI(){
        // Get the compass value
        var azimuthValue = compass.azimuth
        val declination = gps.declination

        if (trueNorthBtn.isChecked){
            azimuthValue += declination
            azimuthValue %= 360
        }

        // Update the text boxes
        val azimuth = (azimuthValue.roundToInt() % 360).toString().padStart(3, ' ')
        val direction = compass.direction.symbol.toUpperCase().padEnd(2, ' ')
        azimuthTxt.text = "${azimuth}°"
        directionTxt.text = direction

        // Rotate the compass
        needleImg.rotation = -azimuthValue

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

        val declination = gps.declination

        // Display the indicator
        destinationStar.visibility = View.VISIBLE

        // Retrieve the current location and azimuth
        val location = gps.location
        var azimuth = compass.azimuth
        if (trueNorthBtn.isChecked) azimuth += declination
        azimuth %= 360

        location ?: return

        // Get the distance to the bearing
        val distance = navigator.getDistance(location)
        var bearing = navigator.getBearing(location)

        // The bearing is already in true north format, convert that to magnetic north
        if (!trueNorthBtn.isChecked) bearing -= declination
        bearing %= 360

        // Display the direction indicator
        val adjBearing = -azimuth - 90 + bearing
        val imgCenterX = needleImg.x + needleImg.width / 2f
        val imgCenterY = needleImg.y + needleImg.height / 2f
        val radius = needleImg.width / 2f + 30
        displayDestinationBearing(adjBearing, imgCenterX, imgCenterY, radius)

        // Update the direction text
        navigationTxt.text = "${navigator.getDestinationName()}:    ${bearing.roundToInt()}°    -    ${LocationMath.distanceToReadableString(distance, unitSystem)}"
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
        val location = gps.location
        val accuracy = gps.accuracy

        // Check to see if the GPS got a location
        if (location == null){
            locationTxt.text = getString(R.string.location_unknown)
            return
        }

        // Update the latitude, longitude display
        locationTxt.text = "${location.latitude}, ${location.longitude}"
        accuracyTxt.text = "GPS accuracy: ${LocationMath.distanceToReadableString(accuracy, unitSystem)}"

        // Update the navigation display
        updateNavigationUI()
    }

}
