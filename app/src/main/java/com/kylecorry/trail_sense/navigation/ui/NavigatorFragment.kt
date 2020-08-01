package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.infrastructure.BeaconDB
import com.kylecorry.trail_sense.navigation.infrastructure.GeoUriParser
import com.kylecorry.trail_sense.navigation.infrastructure.LocationSharesheet
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
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

    constructor(): this(null, null)

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
    private lateinit var navigationTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var beaconBtn: FloatingActionButton
    private lateinit var rulerBtn: FloatingActionButton
    private lateinit var ruler: ConstraintLayout
    private lateinit var parentLayout: ConstraintLayout
    private lateinit var accuracyTxt: TextView

    private lateinit var beaconIndicators: List<ImageView>

    private lateinit var visibleCompass: ICompassView

    private lateinit var beaconDB: BeaconDB

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
        navigationTxt = view.findViewById(R.id.navigation)
        directionTxt = view.findViewById(R.id.compass_direction)
        beaconBtn = view.findViewById(R.id.beaconBtn)
        rulerBtn = view.findViewById(R.id.ruler_btn)
        ruler = view.findViewById(R.id.ruler)
        parentLayout = view.findViewById(R.id.navigator_layout)
        accuracyTxt = view.findViewById(R.id.accuracy)

        val beacons = mutableListOf<ImageView>()

        for (i in 0..(userPrefs.navigation.numberOfVisibleBeacons + 3)){
            beacons.add(ImageView(requireContext()))
        }
        beaconIndicators = beacons

        val theme = requireContext().theme
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val arr = requireContext().obtainStyledAttributes(typedValue.data, IntArray(1) {
            android.R.attr.textColorSecondary
        })
        val astronomyColor = arr.getColor(0, -1)
        arr.recycle()

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


        beaconDB = BeaconDB(requireContext())

        compass = if (userPrefs.navigation.useLegacyCompass) {
            LegacyCompass(requireContext())
        } else {
            VectorCompass(requireContext())
        }

        orientation = DeviceOrientation(requireContext())
        gps = GPS(requireContext())

        if (createBeacon != null) {
            switchToFragment(
                PlaceBeaconFragment(beaconDB, gps, createBeacon),
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

        navigationVM = NavigationViewModel(compass, gps, altimeter, orientation, userPrefs, beaconDB)
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
                    BeaconListFragment(beaconDB, gps),
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
                ruler.visibility = View.INVISIBLE
            } else {
                rulerBtn.setImageResource(R.drawable.hide_ruler)
                ruler.visibility = View.VISIBLE
            }
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

        if (context == null){
            return
        }

        accuracyTxt.text = "Compass: ${navigationVM.compassAccuracy}\nGPS: ${navigationVM.gpsAccuracy}"

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
        navigationTxt.text = navigationVM.navigation
        locationTxt.text = navigationVM.location

        beaconIndicators[0].visibility = navigationVM.sunBeaconVisibility
        beaconIndicators[1].visibility = navigationVM.moonBeaconVisibility
        beaconIndicators[0].alpha = navigationVM.sunBeaconOpacity
        beaconIndicators[1].alpha = navigationVM.moonBeaconOpacity

        beaconIndicators.forEach {
            if (it.height == 0){
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

            val theme = requireContext().theme
            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val arr = requireContext().obtainStyledAttributes(typedValue.data, IntArray(1) {
                android.R.attr.textColorPrimary
            })
            val primaryColor = arr.getColor(0, -1)
            arr.recycle()

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
