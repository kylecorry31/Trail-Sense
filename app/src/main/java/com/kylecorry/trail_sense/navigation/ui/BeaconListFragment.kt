package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.switchToFragment
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.domain.navigation.IBeacon
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.view.ListView


class BeaconListFragment(private val _repo: BeaconRepo?, private val _gps: IGPS?) :
    Fragment() {

    private lateinit var beaconRepo: BeaconRepo
    private lateinit var gps: IGPS

    constructor() : this(null, null)

    private lateinit var beaconList: ListView<IBeacon>
    private lateinit var createBtn: FloatingActionButton
    private lateinit var emptyTxt: TextView
    private lateinit var titleTxt: TextView
    private val sensorService by lazy { SensorService(requireContext()) }
    private var displayedGroup: BeaconGroup? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_beacon_list, container, false)

        beaconRepo = _repo ?: BeaconRepo(requireContext())
        gps = _gps ?: sensorService.getGPS()

        val beaconRecyclerView = view.findViewById<RecyclerView>(R.id.beacon_recycler)
        beaconList =
            ListView(beaconRecyclerView, R.layout.list_item_beacon, this::updateBeaconListItem)
        beaconList.addLineSeparator()
        createBtn = view.findViewById(R.id.create_beacon_btn)
        emptyTxt = view.findViewById(R.id.beacon_empty_text)
        titleTxt = view.findViewById(R.id.beacon_title)

        updateBeaconList()

        createBtn.setOnClickListener {
            if (displayedGroup != null) {
                switchToFragment(
                    PlaceBeaconFragment(
                        beaconRepo,
                        gps,
                        initialGroup = displayedGroup
                    ), addToBackStack = true
                )
            } else {
                val builder = AlertDialog.Builder(requireContext())
                builder.apply {
                    setTitle(getString(R.string.beacon_create))
                    setPositiveButton(getString(R.string.beacon_create_beacon)) { dialog, _ ->
                        switchToFragment(
                            PlaceBeaconFragment(
                                beaconRepo,
                                gps
                            ), addToBackStack = true
                        )
                        dialog.dismiss()
                    }
                    setNeutralButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    setNegativeButton(getString(R.string.beacon_create_group)) { dialog, _ ->
                        editTextDialog(
                            requireContext(),
                            getString(R.string.beacon_create_group),
                            getString(R.string.beacon_group_name_hint),
                            null,
                            null,
                            getString(R.string.dialog_ok),
                            getString(R.string.dialog_cancel)
                        ) { cancelled, text ->
                            if (!cancelled) {
                                beaconRepo.add(BeaconGroup(0, text ?: ""))
                                updateBeaconList()
                            }
                        }
                        dialog.dismiss()
                    }
                }

                val dialog = builder.create()
                dialog.show()
            }
        }

        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && displayedGroup != null) {
                displayedGroup = null
                updateBeaconList()
                true
            } else false
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (gps.hasValidReading) {
            onLocationUpdate()
        } else {
            gps.start(this::onLocationUpdate)
        }
    }

    override fun onPause() {
        gps.stop(this::onLocationUpdate)
        super.onPause()
    }

    private fun onLocationUpdate(): Boolean {
        updateBeaconList()
        return false
    }

    private fun updateBeaconEmptyText(hasBeacons: Boolean) {
        if (!hasBeacons) {
            emptyTxt.visibility = View.VISIBLE
        } else {
            emptyTxt.visibility = View.GONE
        }
    }

    private fun updateBeaconListItem(itemView: View, beacon: IBeacon) {
        if (beacon is Beacon) {
            val listItem = BeaconListItem(itemView, beacon, gps.location)
            listItem.onEdit = {
                switchToFragment(
                    PlaceBeaconFragment(
                        beaconRepo,
                        gps,
                        null,
                        beacon
                    ), addToBackStack = true
                )
            }

            listItem.onNavigate = {
                switchToFragment(NavigatorFragment(beacon))
            }

            listItem.onDeleted = {
                updateBeaconList()
            }
        } else if (beacon is BeaconGroup) {
            val listItem = BeaconGroupListItem(itemView, beacon)
            listItem.onDeleted = {
                updateBeaconList()
            }
            listItem.onEdit = {
                editTextDialog(
                    requireContext(),
                    getString(R.string.beacon_create_group),
                    getString(R.string.beacon_group_name_hint),
                    null,
                    beacon.name,
                    getString(R.string.dialog_ok),
                    getString(R.string.dialog_cancel)
                ) { cancelled, text ->
                    if (!cancelled) {
                        beaconRepo.add(beacon.copy(name = text ?: ""))
                        updateBeaconList()
                    }
                }
            }
            listItem.onOpen = {
                displayedGroup = beacon
                updateBeaconList()
            }
        }
    }

    private fun editTextDialog(
        context: Context,
        title: String,
        hint: String?,
        description: String?,
        initialInputText: String?,
        okButton: String,
        cancelButton: String,
        onClose: (cancelled: Boolean, text: String?) -> Unit
    ): AlertDialog {
        val layout = FrameLayout(context)
        val editTextView = EditText(context)
        editTextView.setText(initialInputText)
        editTextView.hint = hint
        layout.setPadding(64, 0, 64, 0)
        layout.addView(editTextView)

        val builder = AlertDialog.Builder(context)
        builder.apply {
            setTitle(title)
            if (description != null) {
                setMessage(description)
            }
            setView(layout)
            setPositiveButton(okButton) { dialog, _ ->
                onClose(false, editTextView.text.toString())
                dialog.dismiss()
            }
            setNegativeButton(cancelButton) { dialog, _ ->
                onClose(true, null)
                dialog.dismiss()
            }
        }

        val dialog = builder.create()
        dialog.show()
        return dialog
    }

    private fun updateBeaconList() {
        context ?: return

        titleTxt.text = displayedGroup?.name ?: getString(R.string.beacon_list_title)


        val beacons = if (displayedGroup == null) {
            val ungrouped = beaconRepo.getByGroup(null).sortedBy {
                it.coordinate.distanceTo(gps.location)
            }

            val groups = beaconRepo.getGroups().sortedBy {
                it.name
            }
            ungrouped + groups
        } else {
            beaconRepo.getByGroup(displayedGroup?.id).sortedBy {
                it.coordinate.distanceTo(gps.location)
            }
        }

        updateBeaconEmptyText(beacons.isNotEmpty())

        beaconList.setData(beacons)
    }
}

