package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.infrastructure.*
import com.kylecorry.trail_sense.utils.UiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.shared.switchToFragment
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.ceil

class NavigatorFragment(
    private val initialDestination: Beacon? = null,
    private val createBeacon: GeoUriParser.NamedCoordinate? = null
) : Fragment() {

    constructor() : this(null, null)

    private lateinit var compass: ICompass
    private lateinit var gps: IGPS
    private lateinit var orientation: DeviceOrientation
    private lateinit var altimeter: IAltimeter

    // TODO: Extract ruler
    private var isRulerSetup = false
    private var areRulerTextViewsAligned = false

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
    private lateinit var ruler: ConstraintLayout
    private lateinit var parentLayout: ConstraintLayout
    private lateinit var accuracyView: LinearLayout
    private lateinit var gpsAccuracyTxt: TextView
    private lateinit var compassAccuracyTxt: TextView
    private lateinit var gpsAccuracy: LinearLayout
    private lateinit var compassAccuracy: LinearLayout
    private lateinit var speedTxt: TextView

    private lateinit var navigationSheet: LinearLayout
    private lateinit var beaconName: TextView
    private lateinit var beaconComments: ImageButton
    private lateinit var beaconDistance: TextView
    private lateinit var beaconDirection: TextView
    private lateinit var beaconDirectionCardinal: TextView
    private lateinit var beaconElevationView: LinearLayout
    private lateinit var beaconElevation: TextView
    private lateinit var beaconElevationDiff: TextView
    private lateinit var beaconEta: TextView

    private lateinit var beaconIndicators: List<ImageView>

    private lateinit var visibleCompass: ICompassView

    private lateinit var beaconRepo: BeaconRepo

    private var timer: Timer? = null
    private var handler: Handler? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_navigator, container, false)

        // Get views
        userPrefs = UserPreferences(requireContext())
        locationTxt = view.findViewById(R.id.location)
        altitudeTxt = view.findViewById(R.id.altitude)
        azimuthTxt = view.findViewById(R.id.compass_azimuth)
        directionTxt = view.findViewById(R.id.compass_direction)
        beaconBtn = view.findViewById(R.id.beaconBtn)
        rulerBtn = view.findViewById(R.id.ruler_btn)
        ruler = view.findViewById(R.id.ruler)
        parentLayout = view.findViewById(R.id.navigator_layout)
        accuracyView = view.findViewById(R.id.accuracy_view)
        gpsAccuracyTxt = view.findViewById(R.id.gps_accuracy_text)
        compassAccuracyTxt = view.findViewById(R.id.compass_accuracy_text)
        gpsAccuracy = view.findViewById(R.id.gps_accuracy_view)
        compassAccuracy = view.findViewById(R.id.compass_accuracy_view)
        speedTxt = view.findViewById(R.id.speed)

        navigationSheet = view.findViewById(R.id.navigation_sheet)
        beaconName = view.findViewById(R.id.beacon_name)
        beaconComments = view.findViewById(R.id.beacon_comment_btn)
        beaconDistance = view.findViewById(R.id.beacon_distance)
        beaconDirection = view.findViewById(R.id.beacon_direction)
        beaconDirectionCardinal = view.findViewById(R.id.beacon_direction_cardinal)
        beaconElevationView = view.findViewById(R.id.beacon_elevation_view)
        beaconElevation = view.findViewById(R.id.beacon_elevation)
        beaconElevationDiff = view.findViewById(R.id.beacon_elevation_diff)
        beaconEta = view.findViewById(R.id.beacon_eta)

        val beacons = mutableListOf<ImageView>()

        for (i in 0..(userPrefs.navigation.numberOfVisibleBeacons + 3)) {
            beacons.add(ImageView(requireContext()))
        }
        beaconIndicators = beacons

        val astronomyColor = UiUtils.androidTextColorPrimary(requireContext())

        val arrowImg = resources.getDrawable(R.drawable.ic_arrow_target, null)
        val sunImg = resources.getDrawable(R.drawable.sun, null)
        sunImg.setTint(astronomyColor)

        val moonImg = resources.getDrawable(R.drawable.moon_waxing_crescent, null)
        moonImg.setTint(astronomyColor)

        beaconIndicators.forEach {
            it.setImageDrawable(arrowImg)
            it.visibility = View.INVISIBLE
            parentLayout.addView(it)
        }

        beaconIndicators[0].setImageDrawable(sunImg)
        beaconIndicators[1].setImageDrawable(moonImg)


        beaconRepo = BeaconRepo(requireContext())

        compass = if (userPrefs.navigation.useLegacyCompass) {
            LegacyCompass(requireContext())
        } else {
            VectorCompass(requireContext())
        }

        orientation = DeviceOrientation(requireContext())
        gps = GPS(requireContext())

        if (createBeacon != null) {
            switchToFragment(
                PlaceBeaconFragment(beaconRepo, gps, createBeacon),
                addToBackStack = true
            )
        }

        val altimeterMode = userPrefs.navigation.altimeter

        altimeter = when (altimeterMode) {
            NavigationPreferences.AltimeterMode.GPS -> {
                FusedAltimeter(gps, Barometer(requireContext()))
            }
            NavigationPreferences.AltimeterMode.Barometer -> {
                Barometer(requireContext())
            }
            NavigationPreferences.AltimeterMode.None -> {
                NullBarometer()
            }
        }

        navigationVM =
            NavigationViewModel(compass, gps, altimeter, orientation, userPrefs, beaconRepo)
        navigationVM.beacon = initialDestination

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
                updateNavigator()
            }
        }

        rulerBtn.setOnClickListener {
            if (ruler.visibility == View.VISIBLE) {
                rulerBtn.setImageResource(R.drawable.ruler)
                ruler.visibility = View.GONE
            } else {
                rulerBtn.setImageResource(R.drawable.hide_ruler)
                ruler.visibility = View.VISIBLE
            }
        }

        beaconComments.setOnClickListener {
            if (navigationVM.hasComment) {
                UiUtils.alert(requireContext(), navigationVM.commentTitle, navigationVM.comment)
            }
        }

        accuracyView.setOnClickListener {
            UiUtils.alert(
                requireContext(),
                getString(R.string.accuracy_info_title),
                getString(R.string.accuracy_info, navigationVM.gpsHorizontalAccuracy, navigationVM.gpsVerticalAccuracy)
            )
        }

        return view
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
        compass.start(this::onCompassUpdate)
        gps.start(this::onLocationUpdate)
        altimeter.start(this::onAltitudeUpdate)
        orientation.start(this::onOrientationUpdate)

        val hasGPS = SensorChecker(requireContext()).hasGPS()

        if (!hasGPS) {
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
        timer?.cancel()
        timer = null
    }

    private fun updateUI() {

        if (context == null) {
            return
        }

        navigationVM.updateVisibleBeacon()

        if (navigationVM.showNavigationSheet) {
            navigationSheet.visibility = View.VISIBLE
        } else {
            navigationSheet.visibility = View.GONE
        }

        if (navigationVM.hasComment) {
            beaconComments.visibility = View.VISIBLE
        } else {
            beaconComments.visibility = View.GONE
        }

        beaconDistance.text = navigationVM.beaconDistance
        beaconName.text = navigationVM.beaconName
        beaconDirection.text = navigationVM.beaconDirection
        beaconDirectionCardinal.text = navigationVM.beaconCardinalDirection

        if (navigationVM.showBeaconElevation) {
            beaconElevationView.visibility = View.VISIBLE
        } else {
            beaconElevationView.visibility = View.GONE
        }

        beaconElevation.text = navigationVM.beaconElevation
        beaconElevationDiff.text = navigationVM.beaconElevationDiff
        beaconElevationDiff.setTextColor(requireContext().getColor(navigationVM.beaconElevationDiffColor))

        val eta = navigationVM.beaconEta
        beaconEta.text =
            if (eta == null) getString(R.string.distance_away) else getString(R.string.eta, eta)

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

        speedTxt.text = getString(
            R.string.speed_format,
            navigationVM.currentSpeed,
            getString(navigationVM.speedUnit)
        )

        if (navigationVM.showLinearCompass) {
            setVisibleCompass(linearCompass)
        } else {
            setVisibleCompass(roundCompass)
        }

        setupRuler()

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

    private fun onOrientationUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun onCompassUpdate(): Boolean {
        updateUI()
        return true
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

    private fun setupRuler() {
        val dpi = resources.displayMetrics.densityDpi
        val height =
            navigationVM.rulerScale * ruler.height / dpi.toDouble() * if (userPrefs.distanceUnits == UserPreferences.DistanceUnits.Meters) 2.54 else 1.0

        if (height == 0.0 || context == null) {
            return
        }

        if (!isRulerSetup) {
            val primaryColor = UiUtils.androidTextColorPrimary(requireContext())

            for (i in 0..ceil(height).toInt() * 8) {
                val inches = i / 8.0
                val tv = TextView(context)
                val bar = View(context)
                bar.setBackgroundColor(primaryColor)
                val layoutParams = ConstraintLayout.LayoutParams(1, 4)
                bar.layoutParams = layoutParams
                when {
                    inches % 1.0 == 0.0 -> {
                        bar.layoutParams.width = 48
                        tv.text = inches.toInt().toString()
                    }
                    inches % 0.5 == 0.0 -> {
                        bar.layoutParams.width = 36
                    }
                    inches % 0.25 == 0.0 -> {
                        bar.layoutParams.width = 24
                    }
                    else -> {
                        bar.layoutParams.width = 12
                    }
                }
                bar.y =
                    ruler.height * (inches / height).toFloat() + resources.getDimensionPixelSize(R.dimen.ruler_top)
                if (!tv.text.isNullOrBlank()) {
                    tv.setTextColor(primaryColor)
                    ruler.addView(tv)
                    tv.y = bar.y
                    tv.x =
                        bar.layoutParams.width.toFloat() + resources.getDimensionPixelSize(R.dimen.ruler_label)
                }

                ruler.addView(bar)
            }
        } else if (!areRulerTextViewsAligned) {
            for (view in ruler.children) {
                if (view.height != 0) {
                    areRulerTextViewsAligned = true
                }
                view.y -= view.height / 2f
            }
        }

        isRulerSetup = true
    }

    private fun updateNavigator() {
        if (navigationVM.showDestination) {
            // Navigating
            gps.start(this::onLocationUpdate)
            beaconBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_cancel))
            onLocationUpdate()
        } else {
            // Not navigating
            beaconBtn.setImageDrawable(context?.getDrawable(R.drawable.ic_beacon))
            onLocationUpdate()
        }
    }

}
