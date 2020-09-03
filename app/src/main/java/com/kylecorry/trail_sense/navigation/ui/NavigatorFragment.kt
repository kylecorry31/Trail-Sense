package com.kylecorry.trail_sense.navigation.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.FlashlightState
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.domain.Position
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.navigation.infrastructure.*
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.flashlight.Flashlight
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationSharesheet
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.system.UiUtils
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.sensors.declination.IDeclinationProvider
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

    private val navigationService = NavigationService()
    private val astronomyService = AstronomyService()
    private val formatService by lazy { FormatService(requireContext()) }

    private var averageSpeed = 0f

    private lateinit var beacons: Collection<Beacon>
    private var nearbyBeacons: Collection<Beacon> = listOf()

    private var timer: Timer? = null
    private var handler: Handler? = null

    private var destination: Beacon? = null
    private var destinationBearing: Bearing? = null
    private var useTrueNorth = false

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

        for (i in 0..(userPrefs.navigation.numberOfVisibleBeacons + 4)) {
            beacons.add(ImageView(requireContext()))
        }
        beaconIndicators = beacons

        val astronomyColor = UiUtils.androidTextColorPrimary(requireContext())

        val arrowImg = UiUtils.drawable(requireContext(), R.drawable.ic_arrow_target)
        val destinationBearingImg = UiUtils.drawable(requireContext(), R.drawable.ic_arrow_target)
        val sunImg = UiUtils.drawable(requireContext(), R.drawable.sun)
        sunImg?.setTint(astronomyColor)

        val moonImg = UiUtils.drawable(requireContext(), R.drawable.moon_waxing_crescent)
        moonImg?.setTint(astronomyColor)

        beaconIndicators.forEach {
            it.setImageDrawable(arrowImg)
            it.visibility = View.INVISIBLE
            parentLayout.addView(it)
        }

        beaconIndicators[0].setImageDrawable(sunImg)
        beaconIndicators[1].setImageDrawable(moonImg)
        beaconIndicators[2].setImageDrawable(destinationBearingImg)
        beaconIndicators[2].imageTintList =
            ColorStateList.valueOf(UiUtils.color(requireContext(), R.color.colorAccent))

        beaconRepo = BeaconRepo(requireContext())

        compass = sensorService.getCompass()
        orientation = sensorService.getDeviceOrientation()
        gps = sensorService.getGPS()
        declinationProvider = sensorService.getDeclinationProvider()
        altimeter = sensorService.getAltimeter()

        averageSpeed = userPrefs.navigation.averageSpeed

        if (createBeacon != null) {
            switchToFragment(
                PlaceBeaconFragment(beaconRepo, gps, createBeacon),
                addToBackStack = true
            )
        }

        destination = initialDestination
        val beacon = destination?.id
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
            sender.send(gps.location)
            true
        }

        beaconBtn.setOnClickListener {
            if (destination == null) {
                switchToFragment(
                    BeaconListFragment(beaconRepo, gps),
                    addToBackStack = true
                )
            } else {
                destination = null
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

        if (!flashlight.isAvailable()) {
            flashlightBtn.visibility = View.GONE
        } else {
            flashlightBtn.setOnClickListener {
                flashlightState = getNextFlashlightState(flashlightState)
                flashlight.set(flashlightState)
            }
        }

        accuracyView.setOnClickListener { displayAccuracyTips() }

        roundCompass.setOnClickListener {
            if (destinationBearing == null) {
                destinationBearing = compass.bearing
                cache.putFloat(Cache.LAST_DEST_BEARING, compass.bearing.value)
            } else {
                destinationBearing = null
                cache.remove(Cache.LAST_DEST_BEARING)
            }
        }
        linearCompass.setOnClickListener {
            if (destinationBearing == null) {
                destinationBearing = compass.bearing
                cache.putFloat(Cache.LAST_DEST_BEARING, compass.bearing.value)
            } else {
                destinationBearing = null
                cache.remove(Cache.LAST_DEST_BEARING)
            }
        }

        return view
    }

    private fun displayAccuracyTips() {

        val gpsHorizontalAccuracy = gps.horizontalAccuracy
        val gpsVerticalAccuracy = gps.verticalAccuracy

        val gpsHAccuracyStr =
            if (gpsHorizontalAccuracy == null) getString(R.string.accuracy_distance_unknown) else getString(
                R.string.accuracy_distance_format,
                formatService.formatSmallDistance(gpsHorizontalAccuracy)
            )
        val gpsVAccuracyStr =
            if (gpsVerticalAccuracy == null) getString(R.string.accuracy_distance_unknown) else getString(
                R.string.accuracy_distance_format,
                formatService.formatSmallDistance(gpsVerticalAccuracy)
            )

        UiUtils.alert(
            requireContext(),
            getString(R.string.accuracy_info_title),
            getString(
                R.string.accuracy_info,
                gpsHAccuracyStr,
                gpsVAccuracyStr,
                gps.satellites.toString()
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

        visibleCompass.visibility = View.INVISIBLE
        visibleCompass = compass
        visibleCompass.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        useTrueNorth = userPrefs.navigation.useTrueNorth
        // Load the latest beacons
        beacons = beaconRepo.get()

        // Resume navigation
        val lastBeaconId = cache.getInt(Cache.LAST_BEACON_ID)
        if (lastBeaconId != null) {
            destination = beaconRepo.get(lastBeaconId)
        }

        val lastDestBearing = cache.getFloat(Cache.LAST_DEST_BEARING)
        if (lastDestBearing != null) {
            destinationBearing = Bearing(lastDestBearing)
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

    private fun getNearbyBeacons(): Collection<Beacon> {

        if (!userPrefs.navigation.showMultipleBeacons) {
            return listOf()
        }

        return navigationService.getNearbyBeacons(
            gps.location,
            beacons,
            userPrefs.navigation.numberOfVisibleBeacons,
            8f
        )
    }

    private fun getSunBearing(): Bearing {
        return transformTrueNorthBearing(astronomyService.getSunAzimuth(gps.location))
    }

    private fun getMoonBearing(): Bearing {
        return transformTrueNorthBearing(astronomyService.getMoonAzimuth(gps.location))
    }

    private fun getDestinationBearing(): Bearing? {
        val destLocation = destination?.coordinate ?: return null
        return transformTrueNorthBearing(gps.location.bearingTo(destLocation))
    }

    private fun getSelectedBeacon(nearby: Collection<Beacon>): Beacon? {
        return destination ?: getFacingBeacon(nearby)
    }

    private fun transformTrueNorthBearing(bearing: Bearing): Bearing {
        return if (useTrueNorth) {
            bearing
        } else {
            bearing.withDeclination(-declinationProvider.declination)
        }
    }

    private fun getFacingBeacon(nearby: Collection<Beacon>): Beacon? {
        return navigationService.getFacingBeacon(
            getPosition(),
            nearby,
            declinationProvider.declination,
            useTrueNorth
        )
    }

    private fun getCompassMarkers(nearby: Collection<Beacon>): Collection<Bearing> {
        if (destination != null) {
            return listOf(
                getSunBearing(),
                getMoonBearing(),
                destinationBearing ?: compass.bearing,
                getDestinationBearing() ?: Bearing(0f)
            )
        }

        if (!userPrefs.navigation.showMultipleBeacons) {
            return listOf(getSunBearing(), getMoonBearing(), destinationBearing ?: compass.bearing)
        }

        val sunAndMoon =
            listOf(getSunBearing(), getMoonBearing(), destinationBearing ?: compass.bearing)

        val beacons =
            nearby.map { transformTrueNorthBearing(gps.location.bearingTo(it.coordinate)) }

        return sunAndMoon + beacons
    }

    private fun updateUI() {

        if (throttle.isThrottled() || context == null) {
            return
        }

        updateFlashlightUI()

        val selectedBeacon = getSelectedBeacon(nearbyBeacons)

        if (selectedBeacon != null) {
            destinationPanel.show(
                getPosition(),
                selectedBeacon,
                declinationProvider.declination,
                userPrefs.navigation.useTrueNorth
            )
        } else {
            destinationPanel.hide()
        }

        gpsAccuracyTxt.text = formatService.formatAccuracy(gps.accuracy)
        compassAccuracyTxt.text = formatService.formatAccuracy(compass.accuracy)

        if (gps.speed == 0.0f) {
            speedTxt.text = getString(R.string.dash)
        } else {
            speedTxt.text = formatService.formatSpeed(gps.speed)
        }

        // Azimuth
        azimuthTxt.text = formatService.formatDegrees(compass.bearing.value)
        directionTxt.text = formatService.formatDirection(compass.bearing.direction)

        // Compass
        visibleCompass.azimuth = compass.bearing.value
        visibleCompass.beacons = getCompassMarkers(nearbyBeacons).map { it.value }

        // Altitude
        altitudeTxt.text = formatService.formatSmallDistance(altimeter.altitude)

        // Location
        locationTxt.text = formatService.formatLocation(gps.location)

        // Sun and moon
        beaconIndicators[0].visibility = getSunBeaconVisibility()
        beaconIndicators[1].visibility = getMoonBeaconVisibility()
        beaconIndicators[0].alpha = getSunBeaconOpacity()
        beaconIndicators[1].alpha = getMoonBeaconOpacity()
        beaconIndicators[2].visibility =
            if (destination != null || destinationBearing == null) View.INVISIBLE else View.VISIBLE

        beaconIndicators.forEach {
            if (it.height == 0) {
                it.visibility = View.INVISIBLE
            }
        }

        updateNavigationButton()
    }

    private fun shouldShowLinearCompass(): Boolean {
        return userPrefs.navigation.showLinearCompass && orientation.orientation == DeviceOrientation.Orientation.Portrait
    }

    private fun getSunBeaconOpacity(): Float {
        return if (astronomyService.isSunUp(gps.location)) {
            1f
        } else {
            0.5f
        }
    }

    private fun getMoonBeaconOpacity(): Float {
        return if (astronomyService.isMoonUp(gps.location)) {
            1f
        } else {
            0.5f
        }
    }

    private fun getSunBeaconVisibility(): Int {
        return if (userPrefs.astronomy.showOnCompassWhenDown) {
            View.VISIBLE
        } else if (!userPrefs.astronomy.showOnCompass || !astronomyService.isSunUp(gps.location)) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
    }

    private fun getMoonBeaconVisibility(): Int {
        return if (userPrefs.astronomy.showOnCompassWhenDown) {
            View.VISIBLE
        } else if (!userPrefs.astronomy.showOnCompass || !astronomyService.isMoonUp(gps.location)) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
    }

    private fun getPosition(): Position {
        return Position(gps.location, altimeter.altitude, compass.bearing, averageSpeed)
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
        if (shouldShowLinearCompass()) {
            setVisibleCompass(linearCompass)
        } else {
            setVisibleCompass(roundCompass)
        }
        updateUI()
        return true
    }

    private fun onCompassUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onDeclinationUpdate(): Boolean {
        compass.declination = declinationProvider.declination
        updateUI()
        return false
    }

    private fun onAltitudeUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onLocationUpdate(): Boolean {
        nearbyBeacons = getNearbyBeacons()
        updateAverageSpeed()
        updateUI()
        return destination != null
    }

    private fun updateAverageSpeed() {
        if (gps.speed < 0.2f) {
            return
        }

        if (gps.speed > 2f) {
            // If traveling by running, bike or car or sitting still
            averageSpeed = gps.speed
        } else {
            val lastSpeed = userPrefs.navigation.averageSpeed
            averageSpeed = if (lastSpeed == 0f) {
                gps.speed
            } else {
                lastSpeed * 0.4f + gps.speed * 0.6f
            }

            userPrefs.navigation.setAverageSpeed(averageSpeed)
        }
    }

    private fun updateNavigationButton() {
        if (destination != null) {
            beaconBtn.setImageResource(R.drawable.ic_cancel)
        } else {
            beaconBtn.setImageResource(R.drawable.ic_beacon)
        }
    }

    private fun updateNavigator() {
        if (destination != null) {
            // Navigating
            gps.start(this::onLocationUpdate)
            onLocationUpdate()
        } else {
            // Not navigating
            onLocationUpdate()
        }

        updateNavigationButton()
    }

}
