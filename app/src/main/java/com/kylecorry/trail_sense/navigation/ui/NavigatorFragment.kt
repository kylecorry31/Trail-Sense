package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.infrastructure.flashlight.FlashlightProxy
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.FlashlightState
import com.kylecorry.trail_sense.navigation.domain.Position
import com.kylecorry.trail_sense.navigation.infrastructure.*
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.flashlight.Flashlight
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationSharesheet
import com.kylecorry.trail_sense.shared.Cache
import com.kylecorry.trail_sense.shared.Throttle
import com.kylecorry.trail_sense.shared.system.UiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.sensors.declination.IDeclinationProvider
import com.kylecorry.trail_sense.shared.switchToFragment
import java.util.*
import kotlin.concurrent.fixedRateTimer

class NavigatorFragment(
    private val initialDestination: Beacon? = null,
    private val createBeacon: GeoUriParser.NamedCoordinate? = null
) : Fragment() {

    constructor() : this(null, null)

    private lateinit var compass: ICompass
    private lateinit var gps: IGPS
    private lateinit var declinationProvider: IDeclinationProvider
    private lateinit var orientation: DeviceOrientation
    private lateinit var altimeter: IAltimeter

    private lateinit var roundCompass: ICompassView
    private lateinit var linearCompass: ICompassView
    private lateinit var userPrefs: UserPreferences

    private lateinit var navigationVM: NavigationViewModel

    private lateinit var locationTxt: TextView
    private lateinit var altitudeTxt: TextView
    private lateinit var azimuthTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var beaconBtn: FloatingActionButton
    private lateinit var rulerBtn: FloatingActionButton
    private lateinit var flashlightBtn: FloatingActionButton
    private lateinit var ruler: Ruler
    private lateinit var parentLayout: ConstraintLayout
    private lateinit var accuracyView: LinearLayout
    private lateinit var gpsAccuracyTxt: TextView
    private lateinit var compassAccuracyTxt: TextView
    private lateinit var gpsAccuracy: LinearLayout
    private lateinit var compassAccuracy: LinearLayout
    private lateinit var speedTxt: TextView

    private lateinit var destinationPanel: DestinationPanel

    private lateinit var beaconIndicators: List<ImageView>

    private lateinit var visibleCompass: ICompassView

    private lateinit var beaconRepo: BeaconRepo
    private var flashlightState = FlashlightState.Off

    private lateinit var sensorService: SensorService
    private val flashlight by lazy { Flashlight(requireContext()) }
    private val cache by lazy { Cache(requireContext()) }
    private val throttle = Throttle(16)

    private var timer: Timer? = null
    private var handler: Handler? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_navigator, container, false)

        sensorService = SensorService(requireContext())

        // Get views
        userPrefs = UserPreferences(requireContext())
        locationTxt = view.findViewById(R.id.location)
        altitudeTxt = view.findViewById(R.id.altitude)
        azimuthTxt = view.findViewById(R.id.compass_azimuth)
        directionTxt = view.findViewById(R.id.compass_direction)
        beaconBtn = view.findViewById(R.id.beaconBtn)
        rulerBtn = view.findViewById(R.id.ruler_btn)
        flashlightBtn = view.findViewById(R.id.flashlight_btn)
        ruler = Ruler(view.findViewById(R.id.ruler))
        parentLayout = view.findViewById(R.id.navigator_layout)
        accuracyView = view.findViewById(R.id.accuracy_view)
        gpsAccuracyTxt = view.findViewById(R.id.gps_accuracy_text)
        compassAccuracyTxt = view.findViewById(R.id.compass_accuracy_text)
        gpsAccuracy = view.findViewById(R.id.gps_accuracy_view)
        compassAccuracy = view.findViewById(R.id.compass_accuracy_view)
        speedTxt = view.findViewById(R.id.speed)

        destinationPanel = DestinationPanel(view.findViewById(R.id.navigation_sheet))

        val beacons = mutableListOf<ImageView>()

        for (i in 0..(userPrefs.navigation.numberOfVisibleBeacons + 3)) {
            beacons.add(ImageView(requireContext()))
        }
        beaconIndicators = beacons

        val astronomyColor = UiUtils.androidTextColorPrimary(requireContext())

        val arrowImg = ResourcesCompat.getDrawable(resources, R.drawable.ic_arrow_target, null)
        val sunImg = ResourcesCompat.getDrawable(resources, R.drawable.sun, null)
        sunImg?.setTint(astronomyColor)

        val moonImg = ResourcesCompat.getDrawable(resources, R.drawable.moon_waxing_crescent, null)
        moonImg?.setTint(astronomyColor)

        beaconIndicators.forEach {
            it.setImageDrawable(arrowImg)
            it.visibility = View.INVISIBLE
            parentLayout.addView(it)
        }

        beaconIndicators[0].setImageDrawable(sunImg)
        beaconIndicators[1].setImageDrawable(moonImg)

        beaconRepo = BeaconRepo(requireContext())

        compass = sensorService.getCompass()
        orientation = sensorService.getDeviceOrientation()
        gps = sensorService.getGPS()
        declinationProvider = sensorService.getDeclinationProvider()

        if (createBeacon != null) {
            switchToFragment(
                PlaceBeaconFragment(beaconRepo, gps, createBeacon),
                addToBackStack = true
            )
        }

        altimeter = sensorService.getAltimeter()

        navigationVM =
            NavigationViewModel(compass, gps, altimeter, orientation, userPrefs, beaconRepo)
        navigationVM.beacon = initialDestination
        val beacon = navigationVM.beacon?.id
        if (beacon != null) {
            showCalibrationDialog()
            cache.putInt(Cache.LAST_BEACON_ID, beacon)
        }

        roundCompass = CompassView(
            view.findViewById(R.id.needle),
            beaconIndicators,
            view.findViewById(R.id.azimuth_indicator)
        )
        linearCompass = LinearCompassViewHldr(
            view.findViewById(R.id.linear_compass),
            beaconIndicators
        )

        visibleCompass = linearCompass
        setVisibleCompass(roundCompass)

        locationTxt.setOnLongClickListener {
            val sender = LocationSharesheet(requireContext())
            sender.send(navigationVM.shareableLocation)
            true
        }

        beaconBtn.setOnClickListener {
            if (!navigationVM.showDestination) {
                switchToFragment(
                    BeaconListFragment(beaconRepo, gps),
                    addToBackStack = true
                )
            } else {
                navigationVM.beacon = null
                cache.remove(Cache.LAST_BEACON_ID)
                updateNavigator()
            }
        }

        rulerBtn.setOnClickListener {
            if (ruler.visible) {
                UiUtils.setButtonState(rulerBtn, false)
                ruler.hide()
            } else {
                UiUtils.setButtonState(rulerBtn, true)
                ruler.show()
            }
        }

        if (!FlashlightProxy.hasFlashlight(requireContext())) {
            flashlightBtn.visibility = View.GONE
        }

        flashlightBtn.setOnClickListener {
            flashlightState = getNextFlashlightState(flashlightState)
            flashlight.set(flashlightState)
        }

        accuracyView.setOnClickListener { displayAccuracyTips() }

        return view
    }

    private fun displayAccuracyTips() {
        UiUtils.alert(
            requireContext(),
            getString(R.string.accuracy_info_title),
            getString(
                R.string.accuracy_info,
                navigationVM.gpsHorizontalAccuracy,
                navigationVM.gpsVerticalAccuracy,
                navigationVM.gpsSatellites
            )
        )
    }

    private fun getNextFlashlightState(currentState: FlashlightState): FlashlightState {
        return flashlight.getNextState(currentState)
    }

    private fun updateFlashlightUI() {
        when (flashlightState) {
            FlashlightState.On -> {
                flashlightBtn.setImageResource(R.drawable.flashlight)
                UiUtils.setButtonState(flashlightBtn, true)
            }
            FlashlightState.SOS -> {
                flashlightBtn.setImageResource(R.drawable.flashlight_sos)
                UiUtils.setButtonState(flashlightBtn, true)
            }
            else -> {
                flashlightBtn.setImageResource(R.drawable.flashlight)
                UiUtils.setButtonState(flashlightBtn, false)
            }
        }
    }

    private fun setVisibleCompass(compass: ICompassView) {
        if (visibleCompass == compass) {
            if (compass != roundCompass) {
                roundCompass.visibility = View.INVISIBLE
            } else {
                linearCompass.visibility = View.INVISIBLE
            }
        }

        compass.beacons = navigationVM.nearestBeacons
        compass.azimuth = navigationVM.azimuth
        visibleCompass.visibility = View.INVISIBLE
        visibleCompass = compass
        visibleCompass.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        val lastBeaconId = cache.getInt(Cache.LAST_BEACON_ID)
        if (lastBeaconId != null) {
            navigationVM.beacon = beaconRepo.get(lastBeaconId)
        }
        flashlightState = flashlight.getState()
        compass.start(this::onCompassUpdate)
        gps.start(this::onLocationUpdate)
        altimeter.start(this::onAltitudeUpdate)
        orientation.start(this::onOrientationUpdate)

        if (declinationProvider.hasValidReading) {
            onDeclinationUpdate()
        } else {
            declinationProvider.start(this::onDeclinationUpdate)
        }

        if (!userPrefs.useLocationFeatures) {
            beaconBtn.hide()
        } else {
            beaconBtn.show()
            if (userPrefs.navigation.showMultipleBeacons) {
                val that = this
                handler = Handler(Looper.getMainLooper())
                timer = fixedRateTimer(period = 15000) {
                    handler?.post {
                        gps.start(that::onLocationUpdate)
                    }
                }
            }
        }

        // Update the UI
        updateNavigator()
    }

    override fun onPause() {
        super.onPause()
        compass.stop(this::onCompassUpdate)
        gps.stop(this::onLocationUpdate)
        altimeter.stop(this::onAltitudeUpdate)
        orientation.stop(this::onOrientationUpdate)
        declinationProvider.stop(this::onDeclinationUpdate)
        timer?.cancel()
        timer = null
    }

    private fun updateUI() {

        if (throttle.isThrottled() || context == null) {
            return
        }

        updateFlashlightUI()

        navigationVM.updateVisibleBeacon()

        if (navigationVM.showNavigationSheet) {
            val beacon = navigationVM.visibleBeacon
            if (beacon != null) {
                destinationPanel.show(
                    getPosition(),
                    beacon,
                    navigationVM.declination,
                    navigationVM.useTrueNorth
                )
            }
        } else {
            destinationPanel.hide()
        }

        gpsAccuracyTxt.text = navigationVM.gpsAccuracy
        compassAccuracyTxt.text = navigationVM.compassAccuracy

        if (navigationVM.showCompassAccuracy) {
            compassAccuracy.visibility = View.VISIBLE
        } else {
            compassAccuracy.visibility = View.INVISIBLE
        }

        if (navigationVM.showGpsAccuracy) {
            gpsAccuracy.visibility = View.VISIBLE
        } else {
            gpsAccuracy.visibility = View.INVISIBLE
        }

        if (navigationVM.currentSpeed == 0.0f) {
            speedTxt.text = getString(R.string.dash)
        } else {
            speedTxt.text = getString(navigationVM.speedUnit, navigationVM.currentSpeed)
        }

        if (navigationVM.showLinearCompass) {
            setVisibleCompass(linearCompass)
        } else {
            setVisibleCompass(roundCompass)
        }

        azimuthTxt.text = navigationVM.azimuthTxt
        directionTxt.text = navigationVM.azimuthDirection
        visibleCompass.azimuth = navigationVM.azimuth
        visibleCompass.beacons = navigationVM.nearestBeacons

        altitudeTxt.text = navigationVM.altitude

        visibleCompass.beacons = navigationVM.nearestBeacons
        locationTxt.text = navigationVM.location

        beaconIndicators[0].visibility = navigationVM.sunBeaconVisibility
        beaconIndicators[1].visibility = navigationVM.moonBeaconVisibility
        beaconIndicators[0].alpha = navigationVM.sunBeaconOpacity
        beaconIndicators[1].alpha = navigationVM.moonBeaconOpacity

        beaconIndicators.forEach {
            if (it.height == 0) {
                it.visibility = View.INVISIBLE
            }
        }
    }

    private fun getPosition(): Position {
        return Position(gps.location, altimeter.altitude, compass.bearing, navigationVM.speed)
    }

    private fun showCalibrationDialog() {
        if (userPrefs.navigation.showCalibrationOnNavigateDialog) {
            UiUtils.alert(
                requireContext(), getString(R.string.calibrate_compass_dialog_title), getString(
                    R.string.calibrate_compass_on_navigate_dialog_content,
                    getString(R.string.dialog_ok)
                )
            )
        }
    }

    private fun onOrientationUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onCompassUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onDeclinationUpdate(): Boolean {
        navigationVM.declination = declinationProvider.declination
        updateUI()
        return false
    }

    private fun onAltitudeUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onLocationUpdate(): Boolean {
        updateUI()
        navigationVM.onLocationUpdate()
        return navigationVM.showDestination
    }

    private fun updateNavigator() {
        if (navigationVM.showDestination) {
            // Navigating
            gps.start(this::onLocationUpdate)
            beaconBtn.setImageResource(R.drawable.ic_cancel)
            onLocationUpdate()
        } else {
            // Not navigating
            beaconBtn.setImageResource(R.drawable.ic_beacon)
            onLocationUpdate()
        }
    }

}
