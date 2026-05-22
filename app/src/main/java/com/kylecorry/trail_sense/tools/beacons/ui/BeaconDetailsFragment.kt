package com.kylecorry.trail_sense.tools.beacons.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useAndroidContext
import com.kylecorry.trail_sense.shared.extensions.compose.useArgument
import com.kylecorry.trail_sense.shared.extensions.compose.useBackgroundCallback
import com.kylecorry.trail_sense.shared.extensions.compose.useBackgroundMemo
import com.kylecorry.trail_sense.shared.extensions.compose.useGPSLocation
import com.kylecorry.trail_sense.shared.extensions.compose.useMemo
import com.kylecorry.trail_sense.shared.extensions.compose.useNavController
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.compose.DataPoint
import com.kylecorry.trail_sense.shared.views.compose.Toolbar
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.tools.tides.subsystem.TidesSubsystem
import com.kylecorry.trail_sense.tools.tides.ui.TideFormatter
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.ui.dialogs.ShowHighLowTemperatureDialogCommand
import java.time.Duration
import java.time.LocalDate

class BeaconDetailsFragment : TrailSenseComposeFragment() {

    private val astronomy = AstronomyService()

    @Composable
    override fun FragmentContent() {
        val context = useAndroidContext()
        val beaconId = useArgument<Long>("beacon_id")
        val navController = useNavController()
        val beaconService = useService<BeaconService>()
        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val weather = useService<WeatherSubsystem>()
        val tides = useService<TidesSubsystem>()
        val tideFormatter = useMemo(context) { TideFormatter(context) }
        val location = useGPSLocation(Duration.ofSeconds(10)).first

        val beacon = useBackgroundMemo(beaconId, beaconService) {
            beaconId?.let { beaconService.getBeacon(it) }
        }

        val temperature = useBackgroundMemo(beacon, weather) {
            if (beacon == null) {
                return@useBackgroundMemo null
            }
            val temperatureRange = weather.getTemperatureRange(
                LocalDate.now(),
                beacon.coordinate,
                Distance.meters(beacon.elevation ?: 0f)
            )
            val temperatureUnits = prefs.temperatureUnits
            val lowValue = formatter.formatTemperature(
                temperatureRange.start.convertTo(temperatureUnits)
            )
            val highValue = formatter.formatTemperature(
                temperatureRange.end.convertTo(temperatureUnits)
            )
            getString(R.string.slash_separated_pair, highValue, lowValue)
        }

        val sunTimes = useBackgroundMemo(beacon, astronomy, prefs) {
            beacon?.let {
                astronomy.getSunTimes(
                    beacon.coordinate,
                    prefs.astronomy.sunTimesMode,
                    LocalDate.now()
                )
            }
        }

        val sunrise = useMemo(sunTimes, formatter) {
            sunTimes?.rise?.let {
                formatter.formatTime(it.toLocalTime(), includeSeconds = false)
            }
        }

        val sunset = useMemo(sunTimes, formatter) {
            sunTimes?.set?.let {
                formatter.formatTime(it.toLocalTime(), includeSeconds = false)
            }
        }

        val tide = useBackgroundMemo(beacon, tides, tideFormatter) {
            if (beacon == null) {
                return@useBackgroundMemo null
            }
            val nearestTide = tides.getNearestTide(beacon.coordinate)
            nearestTide?.let {
                BeaconTide(
                    tideFormatter.getTideTypeName(it.now.type),
                    tideFormatter.getTideTypeImage(it.now.type)
                )
            }
        }

        val distance = useMemo(beacon, location, formatter, prefs) {
            if (beacon == null) {
                return@useMemo null
            }
            val distance = Distance.meters(beacon.coordinate.distanceTo(location))
                .convertTo(prefs.baseDistanceUnits)
                .toRelativeDistance()
            formatter.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            )
        }

        val elevation = useMemo(beacon, formatter, prefs) {
            if (beacon?.elevation == null) {
                return@useMemo null
            }
            val distance = Distance.meters(beacon.elevation).convertTo(prefs.baseDistanceUnits)
            formatter.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            )
        }

        val beaconLocation = useMemo(beacon, formatter) {
            beacon?.let { formatter.formatLocation(beacon.coordinate) }
        }

        val deleteBeacon = useBackgroundCallback(beaconService, navController) { beaconToDelete: Beacon ->
            beaconService.delete(beaconToDelete)
            onMain {
                navController.navigateUp()
            }
        }

        val showTemperatureDetails = useBackgroundCallback { beaconToShow: Beacon ->
            ShowHighLowTemperatureDialogCommand(
                this@BeaconDetailsFragment,
                beaconToShow.coordinate,
                Distance.meters(beaconToShow.elevation ?: 0f)
            ).execute()
        }

        val uiState = beacon?.let {
            BeaconDetailsState(
                title = it.name,
                subtitle = beaconLocation,
                altitude = elevation,
                distance = distance,
                temperature = temperature,
                sunrise = sunrise,
                sunset = sunset,
                tide = tide,
                comment = it.comment,
                canEdit = !it.temporary
            )
        }

        BeaconDetailsContent(
            state = uiState,
            onNavigate = {
                beaconId?.let {
                    val bundle = Bundle().apply {
                        putLong("destination", it)
                    }
                    navController.openTool(Tools.NAVIGATION, bundle)
                }
            },
            onEdit = {
                beaconId?.let {
                    val bundle = Bundle().apply {
                        putLong("edit_beacon", it)
                    }
                    findNavController().navigate(
                        R.id.action_beacon_details_to_beacon_edit,
                        bundle
                    )
                }
            },
            onShowTemperatureDetails = {
                beacon?.let(showTemperatureDetails)
            },
            onMenu = { view ->
                val currentBeacon = beacon ?: return@BeaconDetailsContent
                Pickers.menu(
                    view,
                    listOf(getString(R.string.share_ellipsis), getString(R.string.delete))
                ) { idx ->
                    when (idx) {
                        0 -> BeaconSender(this@BeaconDetailsFragment).send(currentBeacon)
                        1 -> Alerts.dialog(
                            requireContext(),
                            getString(R.string.delete),
                            currentBeacon.name
                        ) { cancelled ->
                            if (!cancelled) {
                                deleteBeacon(currentBeacon)
                            }
                        }
                    }
                    true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BeaconDetailsContent(
    state: BeaconDetailsState?,
    onNavigate: () -> Unit,
    onEdit: () -> Unit,
    onShowTemperatureDetails: () -> Unit,
    onMenu: (View) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Toolbar(
            title = state?.title,
            subtitle = state?.subtitle,
            id = R.id.beacon_title,
            rightButtonIcon = R.drawable.ic_menu_dots,
            flattenRightButton = true,
            onRightButtonViewClick = onMenu,
            modifier = Modifier.fillMaxWidth()
        )

        BeaconDetailsGrid(
            state = state,
            onShowTemperatureDetails = onShowTemperatureDetails,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SelectionContainer {
                Text(
                    text = state?.comment.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("comment_text")
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 16.dp,
                    bottom = dimensionResource(R.dimen.default_bottom_margin)
                )
        ) {
            Button(
                onClick = onNavigate,
                enabled = state != null,
                modifier = Modifier.testTag("navigate_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_beacon),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = stringResource(R.string.navigate))
            }

            if (state?.canEdit == true) {
                IconButton(
                    onClick = onEdit,
                    colors = IconButtonDefaults.filledIconButtonColors(),
                    shape = IconButtonDefaults.filledShape,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .testTag("edit_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun BeaconDetailsGrid(
    state: BeaconDetailsState?,
    onShowTemperatureDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val points = listOfNotNull(
        state?.altitude?.let {
            BeaconDetailsPoint(
                title = it,
                description = stringResource(R.string.elevation),
                icon = R.drawable.ic_altitude,
                tag = "beacon_altitude"
            )
        },
        state?.distance?.let {
            BeaconDetailsPoint(
                title = it,
                description = stringResource(R.string.distance),
                icon = R.drawable.ruler,
                tag = "beacon_distance"
            )
        },
        state?.temperature?.let {
            BeaconDetailsPoint(
                title = it,
                description = stringResource(R.string.temperature_high_low),
                icon = R.drawable.ic_temperature_range,
                tag = "beacon_temperature",
                onClick = onShowTemperatureDetails
            )
        },
        state?.sunrise?.let {
            BeaconDetailsPoint(
                title = it,
                description = stringResource(R.string.sunrise_label),
                icon = R.drawable.ic_sunrise_notification,
                tag = "beacon_sunrise"
            )
        },
        state?.sunset?.let {
            BeaconDetailsPoint(
                title = it,
                description = stringResource(R.string.sunset_label),
                icon = R.drawable.ic_sunset_notification,
                tag = "beacon_sunset"
            )
        },
        state?.tide?.let {
            BeaconDetailsPoint(
                title = it.title,
                description = stringResource(R.string.tide),
                icon = it.icon,
                tag = "beacon_tide"
            )
        }
    )

    LazyVerticalGrid(
        GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("beacon_grid")
    ) {
        items(points) {
            Box(
                modifier = Modifier.testTag(it.tag)
            ) {
                DataPoint(
                    title = it.title,
                    description = it.description,
                    icon = it.icon,
                    onClick = it.onClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private data class BeaconDetailsState(
    val title: String,
    val subtitle: String?,
    val altitude: String?,
    val distance: String?,
    val temperature: String?,
    val sunrise: String?,
    val sunset: String?,
    val tide: BeaconTide?,
    val comment: String?,
    val canEdit: Boolean
)

private data class BeaconTide(
    val title: String,
    @DrawableRes val icon: Int
)

private data class BeaconDetailsPoint(
    val title: String,
    val description: String,
    @DrawableRes val icon: Int,
    val tag: String,
    val onClick: (() -> Unit)? = null
)

@Preview(showBackground = true)
@Composable
private fun BeaconDetailsPreview() {
    MaterialTheme {
        BeaconDetailsContent(
            state = BeaconDetailsState(
                title = "Test Beacon",
                subtitle = "42.000000°,  -72.000000°",
                altitude = "1000 ft",
                distance = "2.4 mi",
                temperature = "72 °F / 54 °F",
                sunrise = "6:12 AM",
                sunset = "7:45 PM",
                tide = BeaconTide("High", R.drawable.ic_tide_high),
                comment = "Test notes",
                canEdit = true
            ),
            onNavigate = {},
            onEdit = {},
            onShowTemperatureDetails = {},
            onMenu = {},
            modifier = Modifier
                .fillMaxSize()
                .height(600.dp)
        )
    }
}
