package com.kylecorry.trail_sense.navigation.beacons.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.filterIndices
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBeaconListBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance.BeaconDistanceCalculatorFactory
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.export.BeaconGpxConverter
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.export.BeaconGpxImporter
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.loading.BeaconLoader
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.NearestBeaconSort
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.onBackPressed
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import java.time.Instant


class BeaconListFragment : BoundFragment<FragmentBeaconListBinding>() {

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private lateinit var beaconList: ListView<IBeacon>
    private lateinit var navController: NavController
    private val sensorService by lazy { SensorService(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private var displayedGroup: BeaconGroup? = null
    private val beaconService by lazy { BeaconService(requireContext()) }
    private val distanceFactory by lazy { BeaconDistanceCalculatorFactory(beaconService) }
    private val beaconSort by lazy { NearestBeaconSort(distanceFactory, gps::location) }
    private val beaconLoader by lazy { BeaconLoader(beaconSort, beaconService, prefs) }

    private val gpxService by lazy {
        IOFactory().createGpxService(this)
    }

    private val delayedUpdate = Timer {
        if (isBound) {
            binding.loading.isVisible = false
        }
        runInBackground {
            updateBeaconList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireArguments().containsKey("initial_location")) {
            val loc: GeoUri? = requireArguments().getParcelable("initial_location")
            loc?.let { createBeacon(initialLocation = it) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        beaconList =
            ListView(binding.beaconRecycler, R.layout.list_item_beacon, this::updateBeaconListItem)
        beaconList.addLineSeparator()
        navController = findNavController()

        binding.searchbox.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                onSearch()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                onSearch()
                return true
            }
        })

        binding.beaconTitle.rightQuickAction.setOnClickListener {
            onExportBeacons()
        }

        binding.createMenu.setOverlay(binding.overlayMask)
        binding.createMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_import_qr_beacon -> {
                    requestCamera { hasPermission ->
                        if (hasPermission) {
                            importBeaconFromQR()
                        } else {
                            alertNoCameraPermission()
                        }
                    }
                    setCreateMenuVisibility(false)
                }
                R.id.action_import_gpx_beacons -> {
                    importBeacons()
                    setCreateMenuVisibility(false)
                }
                R.id.action_create_beacon_group -> {
                    Pickers.text(
                        requireContext(),
                        getString(R.string.group),
                        null,
                        null,
                        getString(R.string.name)
                    ) {
                        if (it != null) {
                            runInBackground {
                                onIO {
                                    beaconService.add(BeaconGroup(0, it, displayedGroup?.id))
                                }

                                updateBeaconList()
                            }
                        }
                        setCreateMenuVisibility(false)
                    }
                }
                R.id.action_create_beacon -> {
                    setCreateMenuVisibility(false)
                    createBeacon(group = displayedGroup?.id)
                }
            }
            true
        }
        binding.createMenu.setOnHideListener {
            binding.createBtn.setImageResource(R.drawable.ic_add)
        }

        binding.createMenu.setOnShowListener {
            binding.createBtn.setImageResource(R.drawable.ic_cancel)
        }

        binding.createBtn.setOnClickListener {
            setCreateMenuVisibility(!isCreateMenuOpen())
        }

        onBackPressed {
            when {
                isCreateMenuOpen() -> {
                    setCreateMenuVisibility(false)
                }
                displayedGroup != null -> {
                    runInBackground {
                        val parent = displayedGroup?.parentId
                        displayedGroup = if (parent != null) {
                            onIO { beaconService.getGroup(parent) }
                        } else {
                            null
                        }
                        updateBeaconList(true)
                    }
                }
                else -> {
                    remove()
                    navController.navigateUp()
                }
            }
        }
    }

    private fun setCreateMenuVisibility(isShowing: Boolean) {
        if (isShowing) {
            binding.createMenu.show()
        } else {
            binding.createMenu.hide()
        }
    }

    private fun isCreateMenuOpen(): Boolean {
        return binding.createMenu.isVisible
    }

    private fun onExportBeacons() {
        runInBackground {
            val gpx = getExportGPX()
            onMain {
                Pickers.items(
                    requireContext(),
                    getString(R.string.export),
                    gpx.waypoints.map { it.name ?: formatService.formatLocation(it.coordinate) },
                    List(gpx.waypoints.size) { it },
                ) {
                    if (it != null && it.isNotEmpty()) {
                        val selectedWaypoints = gpx.waypoints.filterIndices(it)
                        export(gpx.copy(waypoints = selectedWaypoints))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        clearBeaconList()
        binding.loading.isVisible = true
        if (gps.hasValidReading) {
            onLocationUpdate()
        } else {
            gps.start(this::onLocationUpdate)
        }
    }

    override fun onPause() {
        gps.stop(this::onLocationUpdate)
        delayedUpdate.stop()
        super.onPause()
    }

    private fun onLocationUpdate(): Boolean {
        delayedUpdate.once(LOAD_DELAY)
        return false
    }

    private fun updateBeaconEmptyText(hasBeacons: Boolean) {
        binding.beaconEmptyText.isVisible = !hasBeacons
    }

    private fun updateBeaconListItem(itemView: View, beacon: IBeacon) {
        if (beacon is Beacon) {
            val listItem = BeaconListItem(itemView, this, beacon, gps.location)
            listItem.onView = {
                val bundle = bundleOf("beacon_id" to beacon.id)
                navController.navigate(
                    R.id.action_beacon_list_to_beaconDetailsFragment,
                    bundle
                )
            }

            listItem.onEdit = {
                createBeacon(editingBeaconId = beacon.id)
            }

            listItem.onNavigate = {
                Preferences(requireContext()).putLong("last_beacon_id_long", beacon.id)
                navController.navigateUp()
            }

            listItem.onDeleted = {
                runInBackground {
                    updateBeaconList()
                }
            }

            listItem.onMoved = {
                runInBackground {
                    updateBeaconList()
                }
            }
        } else if (beacon is BeaconGroup) {
            val listItem = BeaconGroupListItem(itemView, lifecycleScope, beacon)
            listItem.onDeleted = {
                runInBackground {
                    updateBeaconList()
                }
            }
            listItem.onMoved = {
                runInBackground {
                    updateBeaconList()
                }
            }
            listItem.onEdit = {
                Pickers.text(
                    requireContext(),
                    getString(R.string.group),
                    null,
                    beacon.name,
                    getString(R.string.name)
                ) {
                    if (it != null) {
                        runInBackground {
                            onIO {
                                beaconRepo.addBeaconGroup(
                                    BeaconGroupEntity.from(
                                        beacon.copy(name = it)
                                    )
                                )
                            }
                            updateBeaconList()
                        }
                    }
                }
            }
            listItem.onOpen = {
                displayedGroup = beacon
                runInBackground {
                    updateBeaconList(true)
                }
            }
        }
    }

    private suspend fun updateBeaconList(resetScroll: Boolean = false) {
        if (!isBound) {
            return
        }

        val beacons = getBeacons()

        onMain {
            if (!isBound) return@onMain
            binding.beaconTitle.title.text =
                displayedGroup?.name ?: getString(R.string.beacons)
            updateBeaconEmptyText(beacons.isNotEmpty())
            beaconList.setData(beacons)
            if (resetScroll) {
                beaconList.scrollToPosition(0, false)
            }
        }
    }

    private fun importBeaconFromQR() {
        val encoder = BeaconQREncoder()
        CustomUiUtils.scanQR(this, getString(R.string.beacon_qr_import_instructions)) {
            if (it != null) {
                val beacon = encoder.decode(it) ?: return@scanQR true
                createBeacon(group = displayedGroup?.id, initialLocation = GeoUri.from(beacon))
            }
            false
        }
    }

    private fun export(gpx: GPXData) {
        runInBackground {
            val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
            val success = gpxService.export(gpx, exportFile)
            onMain {
                if (success) {
                    toast(
                        resources.getQuantityString(
                            R.plurals.beacons_exported,
                            gpx.waypoints.size,
                            gpx.waypoints.size
                        )
                    )
                } else {
                    toast(
                        getString(R.string.beacon_export_error)
                    )
                }
            }
        }
    }

    private fun importBeacons() {
        val importer = BeaconGpxImporter(requireContext())
        runInBackground {
            val gpx = gpxService.import()
            val waypoints = gpx?.waypoints ?: emptyList()
            onMain {
                Pickers.items(
                    requireContext(),
                    getString(R.string.import_btn),
                    waypoints.map {
                        it.name ?: formatService.formatLocation(it.coordinate)
                    },
                    List(waypoints.size) { it }
                ) {
                    if (it != null) {
                        runInBackground {
                            val gpxToImport = GPXData(waypoints.filterIndices(it), emptyList())
                            val count = onIO {
                                importer.import(gpxToImport, displayedGroup?.id)
                            }
                            onMain {
                                Alerts.toast(
                                    requireContext(),
                                    resources.getQuantityString(
                                        R.plurals.beacons_imported,
                                        count,
                                        count
                                    )
                                )
                                updateBeaconList()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getExportGPX(): GPXData = onIO {
        val all = beaconService.getBeacons(
            displayedGroup?.id,
            includeGroups = true,
            includeChildren = true,
            includeRoot = true
        )

        val groups = all.filterIsInstance<BeaconGroup>()
        val beacons = all.filterIsInstance<Beacon>()

        BeaconGpxConverter().toGPX(beacons, groups)
    }

    private fun onSearch() {
        runInBackground {
            updateBeaconList(true)
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBeaconListBinding {
        return FragmentBeaconListBinding.inflate(layoutInflater, container, false)
    }


    private suspend fun getBeacons(): List<IBeacon> = onIO {
        beaconLoader.load(binding.searchbox.query?.toString(), displayedGroup?.id)
    }

    private fun createBeacon(
        group: Long? = null,
        initialLocation: GeoUri? = null,
        editingBeaconId: Long? = null
    ) {
        val bundle = bundleOf()

        group?.let { bundle.putLong("initial_group", it) }
        initialLocation?.let { bundle.putParcelable("initial_location", it) }
        editingBeaconId?.let { bundle.putLong("edit_beacon", it) }

        navController.navigate(
            R.id.action_beaconListFragment_to_placeBeaconFragment,
            bundle
        )
    }

    private fun clearBeaconList(){
        if (isBound){
            beaconList.setData(emptyList())
        }
    }

    companion object {
        private const val LOAD_DELAY = 400L
    }

}

