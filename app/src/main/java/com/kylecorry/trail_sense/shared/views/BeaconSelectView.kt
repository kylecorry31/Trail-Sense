package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconBinding
import com.kylecorry.trail_sense.databinding.ViewBeaconSelectBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
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
    private val formatService by lazy { FormatService(this.context) }
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
            binding.searchboxBeaconPicker.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    onSearch()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    onSearch()
                    return true
                }
            })
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
            ColorStateList.valueOf(Resources.color(context, R.color.orange_40))
        if (beacon is Beacon) {
            itemBinding.icon.setImageResource(R.drawable.ic_location)
            itemBinding.icon.imageTintList = ColorStateList.valueOf(beacon.color)
            itemBinding.title.text = beacon.name
            val distance =
                Distance.meters(location.distanceTo(beacon.coordinate)).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
            itemBinding.description.text = formatService.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
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

        val beacons: List<IBeacon>
        val search = binding.searchboxBeaconPicker.query
        if (!search.isNullOrBlank()) {
            val all = if (displayedGroup == null) {
                (allBeacons.filter { it.name.contains(search, true) }).map {
                    Pair(it, it.coordinate.distanceTo(location))
                }
            } else {
                (allBeacons.filter { it.name.contains(search, true) && it.beaconGroupId == displayedGroup?.id }).map {
                    Pair(it, it.coordinate.distanceTo(location))
                }
            }
            beacons = all.sortedBy { it.second }.map { it.first }
        } else {

            beacons = if (displayedGroup == null) {
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
                allBeacons.filter { beacon -> beacon.beaconGroupId == displayedGroup?.id }
                    .sortedBy {
                        it.coordinate.distanceTo(location)
                    }
            }

            binding.searchboxBeaconPicker.isVisible = beacons.isNotEmpty()
        }

        binding.backBtn.isVisible = displayedGroup != null

        binding.beaconTitle.text =
            displayedGroup?.name ?: context.getString(R.string.all_beacons)
        binding.beaconEmptyText.isVisible = beacons.isEmpty()
        beaconList.setData(beacons)
    }

    private fun onSearch() {
        updateBeaconList()
    }

}