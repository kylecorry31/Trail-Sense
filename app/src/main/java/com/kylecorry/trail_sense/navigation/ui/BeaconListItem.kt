package com.kylecorry.trail_sense.navigation.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemBeaconBinding
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.share.BeaconCopy
import com.kylecorry.trail_sense.navigation.infrastructure.share.BeaconGeoSender
import com.kylecorry.trail_sense.navigation.infrastructure.share.BeaconSharesheet
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BeaconListItem(
    private val view: View,
    private val fragment: Fragment,
    private val scope: CoroutineScope,
    private val beacon: Beacon,
    myLocation: Coordinate
) {

    var onNavigate: () -> Unit = {}
    var onDeleted: () -> Unit = {}
    var onMoved: () -> Unit = {}
    var onEdit: () -> Unit = {}
    var onView: () -> Unit = {}

    private val navigationService = NavigationService()
    private val formatservice by lazy { FormatService(view.context) }
    private val prefs by lazy { UserPreferences(view.context) }
    private val repo by lazy { BeaconRepo.getInstance(view.context) }

    init {
        val binding = ListItemBeaconBinding.bind(view)

        binding.beaconName.text = beacon.name
        if (beacon.owner == BeaconOwner.User) {
            binding.beaconImage.setImageResource(R.drawable.ic_location)
            binding.beaconImage.imageTintList = ColorStateList.valueOf(beacon.color)
        } else if (beacon.owner == BeaconOwner.CellSignal) {
            when {
                beacon.name.contains(formatservice.formatQuality(Quality.Poor)) -> {
                    binding.beaconImage.setImageResource(CellSignalUtils.getCellQualityImage(Quality.Poor))
                    binding.beaconImage.imageTintList = ColorStateList.valueOf(
                        CustomUiUtils.getQualityColor(
                            Quality.Poor
                        )
                    )
                }
                beacon.name.contains(formatservice.formatQuality(Quality.Moderate)) -> {
                    binding.beaconImage.setImageResource(CellSignalUtils.getCellQualityImage(Quality.Moderate))
                    binding.beaconImage.imageTintList = ColorStateList.valueOf(
                        CustomUiUtils.getQualityColor(
                            Quality.Moderate
                        )
                    )
                }
                beacon.name.contains(formatservice.formatQuality(Quality.Good)) -> {
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
        binding.beaconSummary.text = formatservice.formatLargeDistance(distance)
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
            scope.launch {
                val newBeacon = beacon.copy(visible = !beaconVisibility)

                withContext(Dispatchers.IO) {
                    repo.addBeacon(BeaconEntity.from(newBeacon))
                }

                withContext(Dispatchers.Main) {
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
            onNavigate()
        }


        val menuListener = PopupMenu.OnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_view_beacon -> {
                    onView()
                }
                R.id.action_send -> {
                    val sender = BeaconSharesheet(view.context)
                    sender.send(beacon)
                }
                R.id.action_qr -> {
                    val sheet = BeaconQRBottomSheet()
                    sheet.beacon = beacon
                    sheet.show(fragment)
                }
                R.id.action_copy -> {
                    val sender = BeaconCopy(view.context)
                    sender.send(beacon)
                }
                R.id.action_map -> {
                    val sender = BeaconGeoSender(view.context)
                    sender.send(beacon)
                }
                R.id.action_move -> {
                    CustomUiUtils.pickBeaconGroup(
                        view.context,
                        view.context.getString(R.string.move)
                    ) {
                        it ?: return@pickBeaconGroup
                        val newGroupId = if (it.id == -1L) null else it.id
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repo.addBeacon(BeaconEntity.from(beacon.copy(beaconGroupId = newGroupId)))
                            }

                            withContext(Dispatchers.Main) {
                                Alerts.toast(
                                    view.context,
                                    view.context.getString(R.string.beacon_moved_to, it.name)
                                )
                                onMoved()
                            }
                        }
                    }
                }
                R.id.action_edit_beacon -> {
                    onEdit()
                }
                R.id.action_delete_beacon -> {
                    Alerts.dialog(
                        view.context,
                        view.context.getString(R.string.delete_beacon),
                        beacon.name
                    ) { cancelled ->
                        if (!cancelled) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    repo.deleteBeacon(BeaconEntity.from(beacon))
                                }

                                withContext(Dispatchers.Main) {
                                    onDeleted()
                                }
                            }
                        }
                    }
                }
            }
            true
        }

        binding.beaconMenuBtn.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            val inflater = popup.menuInflater
            inflater.inflate(
                if (beacon.temporary) R.menu.temporary_beacon_item_menu else R.menu.beacon_item_menu,
                popup.menu
            )
            popup.setOnMenuItemClickListener(menuListener)
            popup.show()
        }
    }

}