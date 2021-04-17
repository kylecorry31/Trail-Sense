package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.databinding.ViewBeaconSelectBinding
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.domain.navigation.IBeacon
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.IsLargeUnitSpecification
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.*


class BeaconSelectView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    var location = Coordinate.zero
        set(value) {
            field = value
            updateBeaconList()
        }
    private val beaconRepo by lazy { BeaconRepo.getInstance(this.context) }
    private lateinit var beaconList: ListView<IBeacon>
    private var allBeacons: List<Beacon> = listOf()
    private var allGroups: List<BeaconGroup> = listOf()
    private var displayedGroup: BeaconGroup? = null
    private val formatService by lazy { FormatServiceV2(this.context) }
    private val prefs by lazy { UserPreferences(this.context) }
    private lateinit var binding: ViewBeaconSelectBinding

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    var beacon: Beacon? = null

    private var changeListener: ((beacon: Beacon?) -> Unit)? = null


    init {
        context?.let {
            val view = inflate(it, R.layout.view_beacon_select, this)
            binding = ViewBeaconSelectBinding.bind(view)
            beaconList =
                ListView(
                    binding.beaconRecycler,
                    R.layout.list_item_plain_icon,
                    this::updateBeaconListItem
                )
            beaconList.addLineSeparator()
            binding.backBtn.isVisible = false
            binding.beaconSelectTitleBar.setOnClickListener {
                displayedGroup = null
                updateBeaconList()
            }
            scope.launch {
                loadBeacons()
            }
        }
    }

    fun setOnBeaconChangeListener(listener: ((beacon: Beacon?) -> Unit)?) {
        changeListener = listener
    }

    private fun updateBeaconListItem(itemView: View, beacon: IBeacon) {
        val itemBinding = ListItemPlainIconBinding.bind(itemView)
        itemBinding.icon.alpha = 0.86f
        itemBinding.icon.imageTintList =
            ColorStateList.valueOf(UiUtils.color(context, R.color.colorPrimary))
        if (beacon is Beacon) {
            itemBinding.icon.setImageResource(R.drawable.ic_location)
            itemBinding.title.text = beacon.name
            val distance =
                Distance.meters(location.distanceTo(beacon.coordinate)).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
            itemBinding.description.text = formatService.formatDistance(
                distance,
                if (IsLargeUnitSpecification().isSatisfiedBy(distance.units)) 2 else 0
            )
            itemBinding.root.setOnClickListener {
                this.beacon = beacon
                changeListener?.invoke(beacon)
            }
        } else if (beacon is BeaconGroup) {
            itemBinding.icon.setImageResource(R.drawable.ic_beacon_group)
            itemBinding.title.text = beacon.name
            val count = allBeacons.count { it.beaconGroupId == beacon.id }
            itemBinding.description.text = context.resources.getQuantityString(
                R.plurals.beacon_group_summary,
                count,
                count
            )
            itemBinding.root.setOnClickListener {
                displayedGroup = beacon
                updateBeaconList()
            }
        }
    }

    private suspend fun loadBeacons() {
        withContext(Dispatchers.IO) {
            allBeacons = beaconRepo.getBeaconsSync().map { it.toBeacon() }
            allGroups = beaconRepo.getGroupsSync().map { it.toBeaconGroup() }
        }
        withContext(Dispatchers.Main) {
            updateBeaconList()
        }
    }

    private fun updateBeaconList() {
        context ?: return

        val beacons = if (displayedGroup == null) {
            val ungrouped = allBeacons.filter { it.beaconGroupId == null }
            val all = (ungrouped + allGroups).map {
                if (it is Beacon) {
                    Pair(it, it.coordinate.distanceTo(location))
                } else {
                    val groupBeacons =
                        allBeacons.filter { beacon -> beacon.beaconGroupId == it.id }.map { b ->
                            b.coordinate.distanceTo(location)
                        }.minOrNull()
                    Pair(it, groupBeacons ?: Float.POSITIVE_INFINITY)
                }
            }
            all.sortedBy { it.second }.map { it.first }
        } else {
            allBeacons.filter { beacon -> beacon.beaconGroupId == displayedGroup?.id }.sortedBy {
                it.coordinate.distanceTo(location)
            }
        }

        binding.backBtn.isVisible = displayedGroup != null

        binding.beaconTitle.text =
            displayedGroup?.name ?: context.getString(R.string.all_beacons)
        binding.beaconEmptyText.isVisible = beacons.isEmpty()
        beaconList.setData(beacons)
    }

}