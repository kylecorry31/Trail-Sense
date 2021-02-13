package com.kylecorry.trail_sense.navigation.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.databinding.ActivityNavigatorBinding
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationCopy
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.tools.backtrack.ui.QuickActionBacktrack
import com.kylecorry.trail_sense.tools.flashlight.ui.QuickActionFlashlight
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.Position
import com.kylecorry.trailsensecore.domain.units.Quality
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.persistence.Clipboard
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import com.kylecorry.trailsensecore.infrastructure.sensors.compass.ICompass
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.DeviceOrientation
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.time.Duration
import java.time.Instant
import java.util.*


class NavigatorFragment : Fragment() {

    private var shownAccuracyToast: Boolean = false
    private val compass by lazy { sensorService.getCompass() }
    private val gps by lazy { sensorService.getGPS() }
    private val orientation by lazy { sensorService.getDeviceOrientation() }
    private val altimeter by lazy { sensorService.getAltimeter() }

    private lateinit var roundCompass: ICompassView
    private lateinit var linearCompass: ICompassView
    private val userPrefs by lazy { UserPreferences(requireContext()) }

    private var _binding: ActivityNavigatorBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController: NavController

    private lateinit var destinationPanel: DestinationPanel

    private lateinit var beaconIndicators: List<ImageView>

    private lateinit var visibleCompass: ICompassView

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }

    private val sensorService by lazy { SensorService(requireContext()) }
    private val sensorChecker by lazy { SensorChecker(requireContext()) }
    private val cache by lazy { Cache(requireContext()) }
    private val throttle = Throttle(20)

    private val navigationService = NavigationService()
    private val astronomyService = AstronomyService()
    private val geoService = GeoService()
    private val formatService by lazy { FormatService(requireContext()) }

    private var averageSpeed = 0f

    private var beacons: Collection<Beacon> = listOf()
    private var nearbyBeacons: Collection<Beacon> = listOf()

    private val intervalometer = Intervalometer {
        gps.start(this::onLocationUpdate)
    }

    private var destination: Beacon? = null
    private var destinationBearing: Bearing? = null
    private var useTrueNorth = false

    private var leftQuickAction: QuickActionButton? = null
    private var rightQuickAction: QuickActionButton? = null

    private var calibrateSnackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityNavigatorBinding.inflate(layoutInflater, container, false)
        rightQuickAction = QuickActionFlashlight(binding.navigationRightQuickAction, this)
        leftQuickAction = QuickActionBacktrack(binding.navigationLeftQuickAction, this)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        rightQuickAction?.onDestroy()
        leftQuickAction?.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val beaconId = arguments?.getLong("destination") ?: 0L

        if (beaconId != 0L) {
            showCalibrationDialog()
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    destination = beaconRepo.getBeacon(beaconId)?.toBeacon()
                    cache.putLong(LAST_BEACON_ID, beaconId)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        beaconRepo.getBeacons().observe(viewLifecycleOwner){
            beacons = it.map { it.toBeacon() }
            nearbyBeacons = getNearbyBeacons()
            updateUI()
        }

        rightQuickAction?.onCreate()
        leftQuickAction?.onCreate()
        navController = findNavController()

        destinationPanel = DestinationPanel(binding.navigationSheet)

        val tmpBeaconIndicators = mutableListOf<ImageView>()

        for (i in 0..(userPrefs.navigation.numberOfVisibleBeacons + 4)) {
            tmpBeaconIndicators.add(ImageView(requireContext()))
        }
        beaconIndicators = tmpBeaconIndicators

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
            binding.root.addView(it)
        }

        beaconIndicators[0].setImageDrawable(sunImg)
        beaconIndicators[1].setImageDrawable(moonImg)
        beaconIndicators[2].setImageDrawable(destinationBearingImg)
        beaconIndicators[2].imageTintList =
            ColorStateList.valueOf(UiUtils.color(requireContext(), R.color.colorAccent))

        compass.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        orientation.asLiveData().observe(viewLifecycleOwner, { onOrientationUpdate() })
        altimeter.asLiveData().observe(viewLifecycleOwner, { updateUI() })

        averageSpeed = userPrefs.navigation.averageSpeed

        roundCompass = CompassView(
            binding.needle,
            beaconIndicators,
            binding.azimuthIndicator
        )
        linearCompass = LinearCompassViewHldr(
            binding.linearCompass,
            beaconIndicators
        )

        visibleCompass = linearCompass
        setVisibleCompass(roundCompass)

        binding.location.setOnLongClickListener {
            val sender = LocationCopy(requireContext(), Clipboard(requireContext()))
            sender.send(gps.location)
            true
        }

        binding.beaconBtn.setOnClickListener {
            if (destination == null) {
                navController.navigate(R.id.action_navigatorFragment_to_beaconListFragment)
            } else {
                destination = null
                cache.remove(LAST_BEACON_ID)
                updateNavigator()
            }
        }

        binding.accuracyView.setOnClickListener { displayAccuracyTips() }

        roundCompass.setOnClickListener {
            if (destinationBearing == null) {
                destinationBearing = compass.bearing
                cache.putFloat(LAST_DEST_BEARING, compass.bearing.value)
                UiUtils.shortToast(
                    requireContext(),
                    getString(R.string.toast_destination_bearing_set)
                )
            } else {
                destinationBearing = null
                cache.remove(LAST_DEST_BEARING)
            }
        }
        linearCompass.setOnClickListener {
            if (destinationBearing == null) {
                destinationBearing = compass.bearing
                cache.putFloat(LAST_DEST_BEARING, compass.bearing.value)
                UiUtils.shortToast(
                    requireContext(),
                    getString(R.string.toast_destination_bearing_set)
                )
            } else {
                destinationBearing = null
                cache.remove(LAST_DEST_BEARING)
            }
        }

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
            ),
            R.string.dialog_ok
        )
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
        rightQuickAction?.onResume()
        leftQuickAction?.onResume()
        useTrueNorth = userPrefs.navigation.useTrueNorth

        // Resume navigation
        val lastBeaconId = cache.getLong(LAST_BEACON_ID)
        if (lastBeaconId != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    destination = beaconRepo.getBeacon(lastBeaconId)?.toBeacon()
                }
            }
        }

        val lastDestBearing = cache.getFloat(LAST_DEST_BEARING)
        if (lastDestBearing != null) {
            destinationBearing = Bearing(lastDestBearing)
        }

        gps.start(this::onLocationUpdate)
        compass.declination = getDeclination()

        binding.beaconBtn.show()
        if (userPrefs.navigation.showMultipleBeacons) {
            intervalometer.interval(Duration.ofSeconds(15))
        }

        // Update the UI
        updateNavigator()
    }

    override fun onPause() {
        super.onPause()
        calibrateSnackbar?.dismiss()
        rightQuickAction?.onPause()
        leftQuickAction?.onPause()
        gps.stop(this::onLocationUpdate)
        intervalometer.stop()
    }

    private fun getNearbyBeacons(): Collection<Beacon> {

        if (!userPrefs.navigation.showMultipleBeacons) {
            return listOf()
        }

        return navigationService.getNearbyBeacons(
            gps.location,
            beacons,
            userPrefs.navigation.numberOfVisibleBeacons,
            8f,
            userPrefs.navigation.maxBeaconDistance
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
            bearing.withDeclination(-getDeclination())
        }
    }

    private fun getDeclination(): Float {
        return if (!userPrefs.useAutoDeclination){
            userPrefs.declinationOverride
        } else {
            geoService.getDeclination(gps.location, gps.altitude)
        }
    }

    private fun getFacingBeacon(nearby: Collection<Beacon>): Beacon? {
        return navigationService.getFacingBeacon(
            getPosition(),
            nearby,
            getDeclination(),
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

        val selectedBeacon = getSelectedBeacon(nearbyBeacons)

        if (selectedBeacon != null) {
            destinationPanel.show(
                getPosition(),
                selectedBeacon,
                getDeclination(),
                userPrefs.navigation.useTrueNorth
            )
        } else {
            destinationPanel.hide()
        }

        binding.gpsStatus.setStatusText(getGPSStatus())
        binding.gpsStatus.setBackgroundTint(getGPSColor())
        binding.compassStatus.setStatusText(formatService.formatQuality(compass.quality))
        binding.compassStatus.setBackgroundTint(getCompassColor())

        if ((compass.quality == Quality.Poor || compass.quality == Quality.Moderate) && !shownAccuracyToast){
            calibrateSnackbar = Snackbar.make(
                binding.accuracyView, getString(
                    R.string.compass_calibrate_toast, formatService.formatQuality(
                        compass.quality
                    ).toLowerCase(Locale.getDefault())
                ), Snackbar.LENGTH_LONG
            )
            calibrateSnackbar?.setAnchorView(R.id.bottom_navigation)
            calibrateSnackbar?.setAction(getString(R.string.how)){
                displayAccuracyTips()
            }
            val originalMargin = binding.beaconBtn.marginBottom
            val originalAccuracyMargin = binding.accuracyView.marginBottom
            calibrateSnackbar?.addCallback(object :
                BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onShown(transientBottomBar: Snackbar?) {
                    super.onShown(transientBottomBar)
                    try {
                        binding.beaconBtn.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.bottomMargin = (transientBottomBar?.view?.height ?: 0) + originalMargin
                        }
                        binding.accuracyView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.bottomMargin = (transientBottomBar?.view?.height ?: 0) + originalAccuracyMargin
                        }
                    } catch (e: Exception){}
                }

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    try {
                        binding.beaconBtn.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.bottomMargin = originalMargin
                        }
                        binding.accuracyView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            this.bottomMargin = originalAccuracyMargin
                        }
                    } catch (e: Exception){}
                }
            })
            calibrateSnackbar?.show()
            shownAccuracyToast = true
        }

        if (gps.speed == 0.0f) {
            binding.speed.text = getString(R.string.dash)
        } else {
            binding.speed.text = formatService.formatSpeed(gps.speed)
        }

        // Azimuth
        binding.compassAzimuth.text = formatService.formatDegrees(compass.bearing.value)
        binding.compassDirection.text = formatService.formatDirection(compass.bearing.direction)

        // Compass
        visibleCompass.azimuth = compass.bearing.value
        visibleCompass.beacons = getCompassMarkers(nearbyBeacons).map { it.value }

        // Altitude
        binding.altitude.text = formatService.formatSmallDistance(altimeter.altitude)

        // Location
        binding.location.text = formatService.formatLocation(gps.location)

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
                ), R.string.dialog_ok
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

    private fun onLocationUpdate(): Boolean {
        nearbyBeacons = getNearbyBeacons()
        compass.declination = getDeclination()
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
            binding.beaconBtn.setImageResource(R.drawable.ic_cancel)
        } else {
            binding.beaconBtn.setImageResource(R.drawable.ic_beacon)
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

    @ColorInt
    private fun getCompassColor(): Int {
        return CustomUiUtils.getQualityColor(requireContext(), compass.quality)
    }

    @ColorInt
    private fun getGPSColor(): Int {
        if (gps is OverrideGPS){
            return UiUtils.color(requireContext(), R.color.green)
        }

        if (gps is CachedGPS || !sensorChecker.hasGPS()){
            return UiUtils.color(requireContext(), R.color.red)
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)){
            return UiUtils.color(requireContext(), R.color.yellow)
        }

        if (!gps.hasValidReading || (userPrefs.requiresSatellites && gps.satellites < 4)){
            return UiUtils.color(requireContext(), R.color.yellow)
        }

        return CustomUiUtils.getQualityColor(requireContext(), gps.quality)
    }

    private fun getGPSStatus(): String {
        if (gps is OverrideGPS){
            return getString(R.string.gps_user)
        }

        if (gps is CachedGPS || !sensorChecker.hasGPS()){
            return getString(R.string.gps_unavailable)
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)){
            return getString(R.string.gps_stale)
        }

        if (!gps.hasValidReading || (userPrefs.requiresSatellites && gps.satellites < 4)){
            return getString(R.string.gps_searching)
        }

        return formatService.formatQuality(gps.quality)
    }

    companion object {
        const val LAST_BEACON_ID = "last_beacon_id_long"
        const val LAST_DEST_BEARING = "last_dest_bearing"
    }

}
