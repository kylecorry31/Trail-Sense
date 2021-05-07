package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.databinding.ViewBeaconGroupSelectBinding
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.*

class BeaconGroupSelectView(context: Context?, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {

    var location = Coordinate.zero
        set(value) {
            field = value
            updateBeaconList()
        }
    private val beaconRepo by lazy { BeaconRepo.getInstance(this.context) }
    private lateinit var beaconList: ListView<BeaconGroup>
    private var allGroups: List<BeaconGroup> = listOf()
    private lateinit var binding: ViewBeaconGroupSelectBinding

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    var group: BeaconGroup? = null

    private var changeListener: ((group: BeaconGroup?) -> Unit)? = null


    init {
        context?.let {
            val view = inflate(it, R.layout.view_beacon_group_select, this)
            binding = ViewBeaconGroupSelectBinding.bind(view)
            beaconList =
                ListView(
                    binding.beaconRecycler,
                    R.layout.list_item_plain_icon,
                    this::updateBeaconGroupListItem
                )
            beaconList.addLineSeparator()
            scope.launch {
                loadGroups()
            }
        }
    }

    fun setOnBeaconGroupChangeListener(listener: ((group: BeaconGroup?) -> Unit)?) {
        changeListener = listener
    }

    private fun updateBeaconGroupListItem(itemView: View, beacon: BeaconGroup) {
        val itemBinding = ListItemPlainIconBinding.bind(itemView)
        itemBinding.icon.alpha = 0.86f
        itemBinding.description.isVisible = false
        itemBinding.icon.imageTintList =
            ColorStateList.valueOf(UiUtils.color(context, R.color.colorPrimary))
        itemBinding.icon.setImageResource(R.drawable.ic_beacon_group)
        itemBinding.title.text = beacon.name
        itemBinding.root.setOnClickListener {
            group = beacon
            changeListener?.invoke(group)
        }
    }

    private suspend fun loadGroups() {
        withContext(Dispatchers.IO) {
            allGroups =
                beaconRepo.getGroupsSync().map { it.toBeaconGroup() }.sortedBy { it.name }

        }
        withContext(Dispatchers.Main) {
            updateBeaconList()
        }
    }

    private fun updateBeaconList() {
        context ?: return
        binding.beaconEmptyText.isVisible = allGroups.isEmpty()
        beaconList.setData(
            allGroups + listOf(
                BeaconGroup(
                    -1,
                    context.getString(R.string.no_group)
                )
            )
        )
    }

}