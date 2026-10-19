package com.kylecorry.trail_sense.tools.beacons.ui

import android.os.Bundle
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.flatten
import com.kylecorry.trail_sense.shared.extensions.useGPSLocation
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.DataPointView
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.DeleteBeaconCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.tools.tides.subsystem.TidesSubsystem
import com.kylecorry.trail_sense.tools.tides.ui.TideFormatter
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.ui.dialogs.ShowHighLowTemperatureDialogCommand
import java.time.LocalDate

class BeaconDetailsFragment : TrailSenseReactiveFragment(R.layout.fragment_beacon_details) {

    private val astronomy = AstronomyService()

    override fun update() {
        val context = useAndroidContext()

        // Views
        val titleView = useView<Toolbar>(R.id.beacon_title)
        val commentView = useView<TextView>(R.id.comment_text)

        // Services
        val formatter = useService<FormatService>()
        val beaconService = useService<BeaconService>()

        // Arguments
        val beaconId = useArgument<Long>("beacon_id") ?: 0L

        // State
        val beacon = useBackgroundMemo(beaconService, beaconId, resetOnResume) {
            beaconService.getBeacon(beaconId)
        }

        useEffect(titleView) {
            titleView.rightButton.flatten()
        }

        useEffect(titleView, beacon, formatter) {
            titleView.title.text = beacon?.name ?: ""
            titleView.subtitle.text = beacon?.let { formatter.formatLocation(it.coordinate) } ?: ""
        }

        useEffect(commentView, beacon) {
            commentView.text = beacon?.comment ?: ""
        }

        useLocationDataPoints(beacon)
        useTemperatureDataPoint(beacon)
        useSunDataPoints(beacon)
        useTideDataPoint(beacon)
        useActions(beacon)
    }

    private fun useLocationDataPoints(beacon: Beacon?) {
        val elevationView = useView<DataPointView>(R.id.beacon_altitude)
        val distanceView = useView<DataPointView>(R.id.beacon_distance)
        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val (location, _) = useGPSLocation()

        useEffect(elevationView, beacon, formatter, prefs) {
            val elevation = beacon?.elevation
            elevationView.isVisible = elevation != null
            if (elevation != null) {
                val d = Distance.meters(elevation).convertTo(prefs.baseDistanceUnits)
                elevationView.title =
                    formatter.formatDistance(d, Units.getDecimalPlaces(d.units), false)
            }
        }

        useEffect(distanceView, beacon, location, formatter, prefs) {
            distanceView.isVisible = beacon != null
            if (beacon != null) {
                val distance = Distance.meters(beacon.coordinate.distanceTo(location))
                    .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
                distanceView.title =
                    formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)
            }
        }
    }

    private fun useTemperatureDataPoint(beacon: Beacon?) {
        val temperatureView = useView<DataPointView>(R.id.beacon_temperature)
        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val weather = useService<WeatherSubsystem>()

        val temperatureRange = useBackgroundMemo(weather, beacon) {
            beacon?.let {
                weather.getTemperatureRange(
                    LocalDate.now(),
                    it.coordinate,
                    Distance.meters(it.elevation ?: 0f)
                )
            }
        }

        useEffect(temperatureView, temperatureRange, formatter, prefs) {
            temperatureView.isVisible = temperatureRange != null
            if (temperatureRange != null) {
                val units = prefs.temperatureUnits
                val lowValue = formatter.formatTemperature(temperatureRange.start.convertTo(units))
                val highValue = formatter.formatTemperature(temperatureRange.end.convertTo(units))
                temperatureView.title =
                    getString(R.string.slash_separated_pair, highValue, lowValue)
            }
        }

        useEffect(temperatureView, beacon) {
            temperatureView.setOnClickListener {
                if (beacon != null) {
                    inBackground {
                        ShowHighLowTemperatureDialogCommand(
                            this@BeaconDetailsFragment,
                            beacon.coordinate,
                            Distance.meters(beacon.elevation ?: 0f)
                        ).execute()
                    }
                }
            }
        }
    }

    private fun useSunDataPoints(beacon: Beacon?) {
        val sunriseView = useView<DataPointView>(R.id.beacon_sunrise)
        val sunsetView = useView<DataPointView>(R.id.beacon_sunset)
        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()

        val sunTimes = useBackgroundMemo(prefs, beacon) {
            beacon?.let {
                astronomy.getSunTimes(it.coordinate, prefs.astronomy.sunTimesMode, LocalDate.now())
            }
        }

        useEffect(sunriseView, sunsetView, sunTimes, formatter) {
            val rise = sunTimes?.rise?.let {
                formatter.formatTime(it.toLocalTime(), includeSeconds = false)
            }
            val set = sunTimes?.set?.let {
                formatter.formatTime(it.toLocalTime(), includeSeconds = false)
            }
            sunriseView.isVisible = rise != null
            sunsetView.isVisible = set != null
            rise?.let { sunriseView.title = it }
            set?.let { sunsetView.title = it }
        }
    }

    private fun useTideDataPoint(beacon: Beacon?) {
        val context = useAndroidContext()
        val tideView = useView<DataPointView>(R.id.beacon_tide)
        val tides = useMemo(context) { TidesSubsystem.getInstance(context) }
        val tideFormatter = useMemo(context) { TideFormatter(context) }

        val tide = useBackgroundMemo(tides, beacon) {
            beacon?.let { tides.getNearestTide(it.coordinate) }
        }

        useEffect(tideView, tide, tideFormatter) {
            tideView.isVisible = tide != null
            if (tide != null) {
                tideView.title = tideFormatter.getTideTypeName(tide.now.type)
            }
            tideView.setImageResource(tideFormatter.getTideTypeImage(tide?.now?.type))
        }
    }

    private fun useActions(beacon: Beacon?) {
        val context = useAndroidContext()
        val navController = useNavController()
        val titleView = useView<Toolbar>(R.id.beacon_title)
        val navigateButton = useView<MaterialButton>(R.id.navigate_btn)
        val editButton = useView<MaterialButton>(R.id.edit_btn)
        val beaconService = useService<BeaconService>()
        val navigator = useService<Navigator>()

        useEffect(navigateButton, navController, navigator, beacon) {
            navigateButton.setOnClickListener {
                if (beacon != null) {
                    navigator.navigateTo(beacon)
                    val bundle = Bundle().apply {
                        putBoolean(NavigatorFragment.ARG_NAVIGATION_STARTED, true)
                    }
                    navController.openTool(Tools.NAVIGATION, bundle)
                }
            }
        }

        useEffect(editButton, navController, beacon) {
            editButton.isVisible = beacon?.temporary == false
            editButton.setOnClickListener {
                if (beacon != null) {
                    val bundle = Bundle().apply { putLong("edit_beacon", beacon.id) }
                    navController.navigate(R.id.action_beacon_details_to_beacon_edit, bundle)
                }
            }
        }

        useEffect(titleView, navController, context, beaconService, beacon) {
            titleView.rightButton.setOnClickListener { button ->
                if (beacon != null) {
                    val menuItems = if (beacon.temporary) {
                        listOf(getString(R.string.share_ellipsis))
                    } else {
                        listOf(getString(R.string.share_ellipsis), getString(R.string.delete))
                    }
                    Pickers.menu(button, menuItems) { idx ->
                        when (idx) {
                            0 -> BeaconSender(this).send(beacon)
                            1 -> {
                                DeleteBeaconCommand(context, lifecycleScope, beaconService) {
                                    navController.navigateUp()
                                }.execute(beacon)
                            }
                        }
                        true
                    }
                }
            }
        }
    }
}
