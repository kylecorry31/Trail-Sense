package com.kylecorry.trail_sense.navigation.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.share.*
import com.kylecorry.trail_sense.shared.system.UiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.doTransaction
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.sensors.IGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.declination.IDeclinationProvider


class BeaconListFragment(private val _repo: BeaconRepo?, private val _gps: IGPS?) : Fragment() {

    private lateinit var beaconRepo: BeaconRepo
    private lateinit var gps: IGPS

    constructor() : this(null, null)

    private lateinit var beaconList: RecyclerView
    private lateinit var createBtn: FloatingActionButton
    private lateinit var adapter: BeaconAdapter
    private lateinit var emptyTxt: TextView
    private lateinit var shareSheet: LinearLayout
    private lateinit var prefs: UserPreferences
    private lateinit var location: Coordinate
    private lateinit var navigationService: NavigationService
    private val sensorService by lazy { SensorService(requireContext()) }

    private var selectedBeacon: Beacon? = null

    private var showMultipleBeacons = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_beacon_list, container, false)

        beaconRepo = _repo ?: BeaconRepo(requireContext())
        gps = _gps ?: sensorService.getGPS()
        location = gps.location
        navigationService = NavigationService()

        beaconList = view.findViewById(R.id.beacon_recycler)
        createBtn = view.findViewById(R.id.create_beacon_btn)
        emptyTxt = view.findViewById(R.id.beacon_empty_text)
        shareSheet = view.findViewById(R.id.share_sheet)

        prefs = UserPreferences(requireContext())
        showMultipleBeacons = prefs.navigation.showMultipleBeacons

        val layoutManager = LinearLayoutManager(context)
        beaconList.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            context,
            layoutManager.orientation
        )
        beaconList.addItemDecoration(dividerItemDecoration)

        val beacons = beaconRepo.get().sortedBy {
            navigationService.navigate(
                it.coordinate,
                location,
                0f
            ).distance
        }
        updateBeaconEmptyText(beacons.isNotEmpty())

        adapter = BeaconAdapter(beacons)
        beaconList.adapter = adapter

        createBtn.setOnClickListener {
            parentFragmentManager.doTransaction {
                this.replace(
                    R.id.fragment_holder,
                    PlaceBeaconFragment(
                        beaconRepo,
                        gps
                    )
                )
                addToBackStack(javaClass.name)
            }
        }

        shareSheet.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            shareSheet.visibility = View.GONE
        }

        shareSheet.findViewById<LinearLayout>(R.id.share_action_send).setOnClickListener {
            selectedBeacon?.apply {
                val sender = BeaconSharesheet(requireContext())
                sender.send(this)
            }
            shareSheet.visibility = View.GONE
        }

        shareSheet.findViewById<LinearLayout>(R.id.share_action_copy_coordinates)
            .setOnClickListener {
                selectedBeacon?.apply {
                    val sender = BeaconCoordinatesCopy(Clipboard(requireContext()), prefs)
                    sender.send(this)
                }
                shareSheet.visibility = View.GONE
            }

        shareSheet.findViewById<LinearLayout>(R.id.share_action_copy_beacon).setOnClickListener {
            selectedBeacon?.apply {
                val sender = BeaconCopy(Clipboard(requireContext()), prefs)
                sender.send(this)
            }
            shareSheet.visibility = View.GONE
        }

        shareSheet.findViewById<LinearLayout>(R.id.share_action_geo).setOnClickListener {
            selectedBeacon?.apply {
                val sender = BeaconGeoSender(requireContext())
                sender.send(this)
            }
            shareSheet.visibility = View.GONE
        }

        if (beacons.isNotEmpty() && prefs.navigation.showBeaconListToast) {
            UiUtils.shortToast(requireContext(), getString(R.string.long_press_beacon_toast))
            prefs.navigation.showBeaconListToast = false
        }

        return view
    }

    private fun updateBeaconEmptyText(hasBeacons: Boolean) {
        if (!hasBeacons) {
            emptyTxt.visibility = View.VISIBLE
        } else {
            emptyTxt.visibility = View.GONE
        }
    }

    inner class BeaconHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var nameText: TextView = itemView.findViewById(R.id.beacon_name_disp)
        private var locationText: TextView = itemView.findViewById(R.id.beacon_location_disp)
        private var distanceText: TextView = itemView.findViewById(R.id.beacon_distance_disp)
        private var copyBtn: ImageButton = itemView.findViewById(R.id.copy_btn)
        private var visibilityBtn: ImageButton = itemView.findViewById(R.id.visible_btn)
        private var beaconVisibility = false

        fun bindToBeacon(beacon: Beacon) {
            beaconVisibility = beacon.visible
            nameText.text = beacon.name

            locationText.text = prefs.navigation.formatLocation(beacon.coordinate)
            val distance = navigationService.navigate(beacon.coordinate, location, 0f).distance
            distanceText.text = LocationMath.distanceToReadableString(distance, prefs.distanceUnits)


            if (!showMultipleBeacons) {
                visibilityBtn.visibility = View.GONE
            }

            if (beaconVisibility) {
                visibilityBtn.setImageResource(R.drawable.ic_visible)
            } else {
                visibilityBtn.setImageResource(R.drawable.ic_not_visible)
            }

            visibilityBtn.setOnClickListener {
                val newBeacon = beacon.copy(visible = !beaconVisibility)
                beaconRepo.add(newBeacon)
                beaconVisibility = newBeacon.visible
                if (beaconVisibility) {
                    visibilityBtn.setImageResource(R.drawable.ic_visible)
                } else {
                    visibilityBtn.setImageResource(R.drawable.ic_not_visible)
                }
            }

            copyBtn.setOnClickListener {
                shareSheet.visibility = View.VISIBLE
                selectedBeacon = beacon
            }

            itemView.setOnClickListener {
                parentFragmentManager.doTransaction {
                    this.replace(
                        R.id.fragment_holder,
                        NavigatorFragment(
                            beacon
                        )
                    )
                }
            }

            itemView.setOnLongClickListener {
                val dialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setPositiveButton(R.string.edit_beacon) { _, _ ->
                            parentFragmentManager.doTransaction {
                                this.replace(
                                    R.id.fragment_holder,
                                    PlaceBeaconFragment(
                                        beaconRepo,
                                        gps,
                                        null,
                                        beacon
                                    )
                                )
                                addToBackStack(javaClass.name)
                            }
                        }
                        setNeutralButton(R.string.dialog_cancel) { _, _ ->
                            // Do nothing
                        }
                        setNegativeButton(R.string.delete_beacon) { _, _ ->
                            beaconRepo.delete(beacon)
                            adapter.beacons = beaconRepo.get().sortedBy { beacon ->
                                navigationService.navigate(
                                    beacon.coordinate,
                                    location,
                                    0f
                                ).distance
                            }
                            updateBeaconEmptyText(adapter.beacons.isNotEmpty())
                        }
                        setMessage(prefs.navigation.formatLocation(beacon.coordinate))
                        setTitle(beacon.name)
                    }
                    builder.create()
                }
                dialog?.show()
                true
            }
        }
    }

    inner class BeaconAdapter(mBeacons: List<Beacon>) : RecyclerView.Adapter<BeaconHolder>() {

        var beacons: List<Beacon> = mBeacons
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconHolder {
            val view = layoutInflater.inflate(R.layout.list_item_beacon, parent, false)
            return BeaconHolder(view)
        }

        override fun getItemCount(): Int {
            return beacons.size
        }

        override fun onBindViewHolder(holder: BeaconHolder, position: Int) {
            val beacon = beacons[position]
            holder.bindToBeacon(beacon)
        }

    }

}

