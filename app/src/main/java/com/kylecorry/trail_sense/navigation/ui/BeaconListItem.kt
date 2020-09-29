package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.share.BeaconCopy
import com.kylecorry.trail_sense.navigation.infrastructure.share.BeaconGeoSender
import com.kylecorry.trail_sense.navigation.infrastructure.share.BeaconSharesheet
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.persistence.Clipboard

class BeaconListItem(
    private val view: View,
    private val beacon: Beacon,
    myLocation: Coordinate
) {

    var onNavigate: () -> Unit = {}
    var onDeleted: () -> Unit = {}
    var onEdit: () -> Unit = {}

    private val navigationService = NavigationService()
    private val formatservice by lazy { FormatService(view.context) }
    private val prefs by lazy { UserPreferences(view.context) }
    private val repo by lazy { BeaconRepo(view.context) }

    init {
        val nameText: TextView = view.findViewById(R.id.beacon_name)
        val summaryTxt: TextView = view.findViewById(R.id.beacon_summary)
        val menuBtn: ImageButton = view.findViewById(R.id.beacon_menu_btn)
        val beaconImg: ImageView = view.findViewById(R.id.beacon_image)
        val visibilityBtn: ImageButton = view.findViewById(R.id.visible_btn)

        nameText.text = beacon.name
        beaconImg.setImageResource(R.drawable.ic_location)
        var beaconVisibility = beacon.visible
        val distance = navigationService.navigate(beacon.coordinate, myLocation, 0f).distance
        summaryTxt.text = formatservice.formatLargeDistance(distance)
        if (!prefs.navigation.showMultipleBeacons) {
            visibilityBtn.visibility = View.GONE
        } else {
            visibilityBtn.visibility = View.VISIBLE
        }
        if (beaconVisibility) {
            visibilityBtn.setImageResource(R.drawable.ic_visible)
        } else {
            visibilityBtn.setImageResource(R.drawable.ic_not_visible)
        }

        visibilityBtn.setOnClickListener {
            val newBeacon = beacon.copy(visible = !beaconVisibility)
            repo.add(newBeacon)
            beaconVisibility = newBeacon.visible
            if (beaconVisibility) {
                visibilityBtn.setImageResource(R.drawable.ic_visible)
            } else {
                visibilityBtn.setImageResource(R.drawable.ic_not_visible)
            }
        }

        view.setOnClickListener {
            onNavigate()
        }


        val menuListener = PopupMenu.OnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_send -> {
                    val sender = BeaconSharesheet(view.context)
                    sender.send(beacon)
                }
                R.id.action_copy -> {
                    val sender = BeaconCopy(view.context, Clipboard(view.context), prefs)
                    sender.send(beacon)
                }
                R.id.action_map -> {
                    val sender = BeaconGeoSender(view.context)
                    sender.send(beacon)
                }
                R.id.action_edit_beacon -> {
                    onEdit()
                }
                R.id.action_delete_beacon -> {
                    repo.delete(beacon)
                    onDeleted()
                }
            }
            true
        }

        menuBtn.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            val inflater = popup.menuInflater
            inflater.inflate(R.menu.beacon_item_menu, popup.menu)
            popup.setOnMenuItemClickListener(menuListener)
            popup.show()
        }
    }

}