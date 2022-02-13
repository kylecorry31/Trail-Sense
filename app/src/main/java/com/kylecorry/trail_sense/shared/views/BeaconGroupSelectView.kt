package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.databinding.ViewBeaconGroupSelectBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.AlphabeticalBeaconSort
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BeaconGroupSelectView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var beaconList: ListView<BeaconGroup>
    private var binding: ViewBeaconGroupSelectBinding

    // TODO: Cancel the job on destroy
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    var group: BeaconGroup? = null

    private val beaconService by lazy { BeaconService(this.context) }

    init {
        val view = inflate(context, R.layout.view_beacon_group_select, this)
        binding = ViewBeaconGroupSelectBinding.bind(view)
        beaconList =
            ListView(
                binding.beaconRecycler,
                R.layout.list_item_plain_icon,
                this::updateBeaconGroupListItem
            )
        beaconList.addLineSeparator()
        binding.beaconGroupTitle.leftQuickAction.setOnClickListener {
            loadGroup(group?.parentId)
        }
        updateBeaconList()
    }

    private fun loadGroup(groupId: Long?){
        scope.launch {
            group = onIO {
                if (groupId != null) {
                    beaconService.getGroup(groupId)
                } else {
                    null
                }
            }
            updateBeaconList()
        }
    }

    private fun updateBeaconGroupListItem(itemView: View, beacon: BeaconGroup) {
        val itemBinding = ListItemPlainIconBinding.bind(itemView)
        itemBinding.icon.alpha = 0.86f
        itemBinding.description.isVisible = false
        itemBinding.icon.imageTintList =
            ColorStateList.valueOf(Resources.color(context, R.color.orange_40))
        itemBinding.icon.setImageResource(R.drawable.ic_beacon_group)
        itemBinding.title.text = beacon.name
        itemBinding.root.setOnClickListener {
            group = beacon
            updateBeaconList()
        }
    }

    private fun updateBeaconList() {
        scope.launch {
            val groups = getGroups()
            onMain {
                binding.beaconGroupTitle.title.text =
                    group?.name ?: context.getString(R.string.no_group)
                binding.beaconGroupTitle.leftQuickAction.isVisible = group != null
                beaconList.setData(groups)
                binding.beaconEmptyText.isVisible = groups.isEmpty()
            }
        }
    }

    private suspend fun getGroups(): List<BeaconGroup> = onIO {
        val sort = AlphabeticalBeaconSort()
        val group = group?.id
        val groups = beaconService.getGroups(group)
        sort.sort(groups).map { it as BeaconGroup }
    }

}