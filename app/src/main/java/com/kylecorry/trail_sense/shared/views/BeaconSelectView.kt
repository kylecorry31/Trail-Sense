package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
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
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance.BeaconDistanceCalculatorFactory
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.loading.BeaconLoader
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.NearestBeaconSort
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class BeaconSelectView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    var location = Coordinate.zero
        set(value) {
            field = value
            updateBeaconList()
        }
    private var beaconList: ListView<IBeacon>
    private var displayedGroup: BeaconGroup? = null
    private val formatService by lazy { FormatService(this.context) }
    private val prefs by lazy { UserPreferences(this.context) }
    private var binding: ViewBeaconSelectBinding

    private val beaconService by lazy { BeaconService(context) }
    private val distanceFactory by lazy { BeaconDistanceCalculatorFactory(beaconService) }
    private val beaconSort by lazy { NearestBeaconSort(distanceFactory, this::location) }
    private val beaconLoader by lazy { BeaconLoader(beaconService, prefs.navigation) }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    var beacon: Beacon? = null

    private var changeListener: ((beacon: Beacon?) -> Unit)? = null


    init {
        val view = inflate(context, R.layout.view_beacon_select, this)
        binding = ViewBeaconSelectBinding.bind(view)
        beaconList =
            ListView(
                binding.beaconRecycler,
                R.layout.list_item_plain_icon,
                this::updateBeaconListItem
            )
        beaconList.addLineSeparator()
        binding.beaconTitle.leftQuickAction.isVisible = false
        binding.beaconTitle.leftQuickAction.setOnClickListener {
            loadGroup(displayedGroup?.parentId)
            updateBeaconList()
        }
        binding.searchboxBeaconPicker.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                onSearch()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                onSearch()
                return true
            }
        })
        updateBeaconList()
    }

    private fun loadGroup(groupId: Long?) {
        scope.launch {
            displayedGroup = onIO {
                if (groupId != null) {
                    beaconService.getGroup(groupId)
                } else {
                    null
                }
            }
            updateBeaconList()
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
                Distance.meters(location.distanceTo(beacon.coordinate))
                    .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
            itemBinding.description.isVisible = true
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
            itemBinding.description.isVisible = false
            itemBinding.root.setOnClickListener {
                displayedGroup = beacon
                updateBeaconList()
            }
        }
    }

    private fun updateBeaconList() {
        scope.launch {
            val beacons = getBeacons()

            onMain {
                binding.searchboxBeaconPicker.isVisible = beacons.isNotEmpty()
                binding.beaconTitle.leftQuickAction.isVisible = displayedGroup != null
                binding.beaconTitle.title.text =
                    displayedGroup?.name ?: context.getString(R.string.beacons)
                binding.beaconEmptyText.isVisible = beacons.isEmpty()
                beaconList.setData(beacons)
            }
        }
    }

    private suspend fun getBeacons(): List<IBeacon> = onIO {
        val beacons = beaconLoader.load(binding.searchboxBeaconPicker.query?.toString(), displayedGroup?.id)
        beaconSort.sort(beacons)
    }

    private fun onSearch() {
        updateBeaconList()
    }

}