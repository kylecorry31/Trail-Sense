package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class BeaconGroupListItem(
    private val view: View,
    private val group: BeaconGroup
) {

    var onOpen: () -> Unit = {}
    var onDeleted: () -> Unit = {}
    var onEdit: () -> Unit = {}

    private val repo by lazy { BeaconRepo(view.context) }

    init {
        val nameText: TextView = view.findViewById(R.id.beacon_name)
        val summaryTxt: TextView = view.findViewById(R.id.beacon_summary)
        val menuBtn: ImageButton = view.findViewById(R.id.beacon_menu_btn)
        val beaconImg: ImageView = view.findViewById(R.id.beacon_image)
        val visibilityBtn: ImageButton = view.findViewById(R.id.visible_btn)

        nameText.text = group.name
        beaconImg.setImageResource(R.drawable.ic_beacon_group)
        val count = repo.getNumberOfBeaconsInGroup(group.id)
        summaryTxt.text =
            view.context.resources.getQuantityString(R.plurals.beacon_group_summary, count, count)
        visibilityBtn.visibility = View.GONE

        view.setOnClickListener {
            onOpen()
        }

        val menuListener = PopupMenu.OnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_edit_beacon_group -> {
                    onEdit()
                }
                R.id.action_delete_beacon_group -> {
                    UiUtils.alertWithCancel(
                        view.context,
                        view.context.getString(R.string.delete_beacon_group),
                        view.context.getString(R.string.delete_beacon_group_message, group.name),
                        view.context.getString(R.string.dialog_ok),
                        view.context.getString(R.string.dialog_cancel)
                    ) { cancelled ->
                        if (!cancelled) {
                            repo.delete(group)
                            onDeleted()
                        }
                    }
                }
            }
            true
        }

        menuBtn.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            val inflater = popup.menuInflater
            inflater.inflate(R.menu.beacon_group_item_menu, popup.menu)
            popup.setOnMenuItemClickListener(menuListener)
            popup.show()
        }

    }

}