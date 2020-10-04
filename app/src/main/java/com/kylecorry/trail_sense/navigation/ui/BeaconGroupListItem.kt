package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.PopupMenu
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemBeaconBinding
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
        val binding = ListItemBeaconBinding.bind(view)

        binding.beaconName.text = group.name
        binding.beaconImage.setImageResource(R.drawable.ic_beacon_group)
        val count = repo.getNumberOfBeaconsInGroup(group.id)
        binding.beaconSummary.text =
            view.context.resources.getQuantityString(R.plurals.beacon_group_summary, count, count)
        binding.visibleBtn.visibility = View.GONE

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

        binding.beaconMenuBtn.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            val inflater = popup.menuInflater
            inflater.inflate(R.menu.beacon_group_item_menu, popup.menu)
            popup.setOnMenuItemClickListener(menuListener)
            popup.show()
        }

    }

}