package com.kylecorry.trail_sense.navigation.beacons.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.PopupMenu
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemBeaconBinding
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BeaconGroupListItem(
    private val view: View,
    private val scope: CoroutineScope,
    private val group: BeaconGroup
) {

    var onOpen: () -> Unit = {}
    var onDeleted: () -> Unit = {}
    var onEdit: () -> Unit = {}

    private val repo by lazy { BeaconRepo.getInstance(view.context) }

    init {
        val binding = ListItemBeaconBinding.bind(view)

        binding.beaconName.text = group.name
        binding.beaconImage.setImageResource(R.drawable.ic_beacon_group)
        binding.beaconImage.imageTintList = ColorStateList.valueOf(AppColor.Orange.color)
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                repo.getBeaconsInGroup(group.id).size
            }

            withContext(Dispatchers.Main) {
                binding.beaconSummary.text = view.context.resources.getQuantityString(
                    R.plurals.beacon_group_summary,
                    count,
                    count
                )
            }
        }

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
                    Alerts.dialog(
                        view.context,
                        view.context.getString(R.string.delete_beacon_group),
                        view.context.getString(R.string.delete_beacon_group_message, group.name),
                    ) { cancelled ->
                        if (!cancelled) {
                            scope.launch {
                                withContext(Dispatchers.IO){
                                    repo.deleteBeaconGroup(BeaconGroupEntity.from(group))
                                }

                                withContext(Dispatchers.Main){
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
            inflater.inflate(R.menu.beacon_group_item_menu, popup.menu)
            popup.setOnMenuItemClickListener(menuListener)
            popup.show()
        }

    }


}