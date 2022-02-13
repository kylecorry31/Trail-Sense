package com.kylecorry.trail_sense.navigation.beacons.ui

import android.content.res.ColorStateList
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemBeaconBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands.DeleteBeaconCommand
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands.MoveBeaconCommand
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import kotlinx.coroutines.launch

class BeaconListItem(
    private val view: View,
    private val fragment: Fragment,
    private val beacon: Beacon,
    myLocation: Coordinate
) {

    var onNavigate: () -> Unit = {}
    var onDeleted: () -> Unit = {}
    var onMoved: () -> Unit = {}
    var onEdit: () -> Unit = {}
    var onView: () -> Unit = {}

    private val navigationService = NavigationService()
    private val formatService by lazy { FormatService(view.context) }
    private val prefs by lazy { UserPreferences(view.context) }
    private val service by lazy { BeaconService(view.context) }

    init {
        val binding = ListItemBeaconBinding.bind(view)

        binding.beaconName.text = beacon.name
        if (beacon.owner == BeaconOwner.User) {
            binding.beaconImage.setImageResource(R.drawable.ic_location)
            binding.beaconImage.imageTintList = ColorStateList.valueOf(beacon.color)
        } else if (beacon.owner == BeaconOwner.CellSignal) {
            when {
                beacon.name.contains(formatService.formatQuality(Quality.Poor)) -> {
                    binding.beaconImage.setImageResource(CellSignalUtils.getCellQualityImage(Quality.Poor))
                    binding.beaconImage.imageTintList = ColorStateList.valueOf(
                        CustomUiUtils.getQualityColor(
                            Quality.Poor
                        )
                    )
                }
                beacon.name.contains(formatService.formatQuality(Quality.Moderate)) -> {
                    binding.beaconImage.setImageResource(CellSignalUtils.getCellQualityImage(Quality.Moderate))
                    binding.beaconImage.imageTintList = ColorStateList.valueOf(
                        CustomUiUtils.getQualityColor(
                            Quality.Moderate
                        )
                    )
                }
                beacon.name.contains(formatService.formatQuality(Quality.Good)) -> {
                    binding.beaconImage.setImageResource(CellSignalUtils.getCellQualityImage(Quality.Good))
                    binding.beaconImage.imageTintList = ColorStateList.valueOf(
                        CustomUiUtils.getQualityColor(
                            Quality.Good
                        )
                    )
                }
                else -> {
                    binding.beaconImage.setImageResource(CellSignalUtils.getCellQualityImage(Quality.Unknown))
                    binding.beaconImage.imageTintList = ColorStateList.valueOf(
                        CustomUiUtils.getQualityColor(
                            Quality.Unknown
                        )
                    )
                }
            }
        }
        var beaconVisibility = beacon.visible
        val distance = navigationService.navigate(beacon.coordinate, myLocation, 0f).distance
        val d = Distance.meters(distance).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        binding.beaconSummary.text =
            formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
        if (!(prefs.navigation.showMultipleBeacons || prefs.navigation.areMapsEnabled) || beacon.temporary) {
            binding.visibleBtn.visibility = View.GONE
        } else {
            binding.visibleBtn.visibility = View.VISIBLE
        }
        if (beaconVisibility) {
            binding.visibleBtn.setImageResource(R.drawable.ic_visible)
        } else {
            binding.visibleBtn.setImageResource(R.drawable.ic_not_visible)
        }

        binding.visibleBtn.setOnClickListener {
            fragment.lifecycleScope.launch {
                val newBeacon = beacon.copy(visible = !beaconVisibility)

                onIO {
                    service.add(newBeacon)
                }

                onMain {
                    beaconVisibility = newBeacon.visible
                    if (beaconVisibility) {
                        binding.visibleBtn.setImageResource(R.drawable.ic_visible)
                    } else {
                        binding.visibleBtn.setImageResource(R.drawable.ic_not_visible)
                    }
                }
            }
        }

        view.setOnClickListener {
            onView()
        }

        view.setOnLongClickListener {
            onNavigate()
            true
        }

        binding.beaconMenuBtn.setOnClickListener {
            Pickers.menu(
                it,
                if (beacon.temporary) R.menu.temporary_beacon_item_menu else R.menu.beacon_item_menu
            ) {
                when (it) {
                    R.id.action_navigate_beacon -> {
                        onNavigate()
                    }
                    R.id.action_send -> {
                        BeaconSender(fragment).send(beacon)
                    }
                    R.id.action_move -> {
                        val command = MoveBeaconCommand(
                            view.context,
                            fragment.lifecycleScope,
                            service,
                            onMoved
                        )
                        command.execute(beacon)
                    }
                    R.id.action_edit_beacon -> {
                        onEdit()
                    }
                    R.id.action_delete_beacon -> {
                        val command = DeleteBeaconCommand(
                            view.context,
                            fragment.lifecycleScope,
                            service,
                            onDeleted
                        )
                        command.execute(beacon)
                    }
                }
                true
            }
        }
    }

}