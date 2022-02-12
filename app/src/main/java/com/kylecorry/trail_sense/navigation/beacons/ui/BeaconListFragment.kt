package com.kylecorry.trail_sense.navigation.beacons.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
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
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance.BeaconDistanceCalculatorFactory
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.export.BeaconGpxConverter
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.export.BeaconGpxImporter
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.NearestBeaconSort
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val gpxService by lazy {
        IOFactory().createGpxService(this)
    }

    private val delayedUpdate = Timer {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                updateBeaconList()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireArguments().containsKey("initial_location")) {
            val loc: GeoUri? = requireArguments().getParcelable("initial_location")
            if (loc != null) {
                findNavController().navigate(
                    R.id.action_beaconListFragment_to_placeBeaconFragment,
                    bundleOf("initial_location" to loc)
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val beaconRecyclerView = binding.beaconRecycler
        beaconList =
            ListView(beaconRecyclerView, R.layout.list_item_beacon, this::updateBeaconListItem)
        beaconList.addLineSeparator()
        navController = findNavController()

        delayedUpdate.once(100)

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
                                withContext(Dispatchers.IO) {
                                    beaconRepo.addBeaconGroup(BeaconGroupEntity(it))
                                }

                                withContext(Dispatchers.Main) {
                                    updateBeaconList()
                                }
                            }
                        }
                        setCreateMenuVisibility(false)
                    }
                }
                R.id.action_create_beacon -> {
                    setCreateMenuVisibility(false)
                    navController.navigate(R.id.action_beaconListFragment_to_placeBeaconFragment)
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
            if (displayedGroup != null) {
                val bundle = bundleOf("initial_group" to displayedGroup!!.id)
                navController.navigate(
                    R.id.action_beaconListFragment_to_placeBeaconFragment,
                    bundle
                )
            } else {
                setCreateMenuVisibility(!isCreateMenuOpen())
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            when {
                isCreateMenuOpen() -> {
                    setCreateMenuVisibility(false)
                }
                displayedGroup != null -> {
                    displayedGroup = null
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            updateBeaconList(true)
                        }
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
            withContext(Dispatchers.Main) {
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
        delayedUpdate.once(100)
        return false
    }

    private fun updateBeaconEmptyText(hasBeacons: Boolean) {
        binding.beaconEmptyText.isVisible = !hasBeacons
    }

    private fun updateBeaconListItem(itemView: View, beacon: IBeacon) {
        if (beacon is Beacon) {
            val listItem = BeaconListItem(itemView, this, lifecycleScope, beacon, gps.location)
            listItem.onView = {
                val bundle = bundleOf("beacon_id" to beacon.id)
                navController.navigate(
                    R.id.action_beacon_list_to_beaconDetailsFragment,
                    bundle
                )
            }

            listItem.onEdit = {
                val bundle = bundleOf("edit_beacon" to beacon.id)
                navController.navigate(
                    R.id.action_beaconListFragment_to_placeBeaconFragment,
                    bundle
                )
            }

            listItem.onNavigate = {
                Preferences(requireContext()).putLong("last_beacon_id_long", beacon.id)
                navController.navigateUp()
            }

            listItem.onDeleted = {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        updateBeaconList()
                    }
                }
            }

            listItem.onMoved = {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        updateBeaconList()
                    }
                }
            }
        } else if (beacon is BeaconGroup) {
            val listItem = BeaconGroupListItem(itemView, lifecycleScope, beacon)
            listItem.onDeleted = {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        updateBeaconList()
                    }
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
                            withContext(Dispatchers.IO) {
                                beaconRepo.addBeaconGroup(
                                    BeaconGroupEntity.from(
                                        beacon.copy(name = it)
                                    )
                                )
                            }
                            withContext(Dispatchers.Main) {
                                updateBeaconList()
                            }
                        }
                    }
                }
            }
            listItem.onOpen = {
                displayedGroup = beacon
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        updateBeaconList(true)
                    }
                }
            }
        }
    }

    private suspend fun updateBeaconList(resetScroll: Boolean = false) {
        if (!isBound) {
            return
        }

        val beacons = getBeacons()

        withContext(Dispatchers.Main) {
            context ?: return@withContext
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
                navController.navigate(
                    R.id.action_beaconListFragment_to_placeBeaconFragment,
                    bundleOf("initial_location" to GeoUri.from(beacon))
                )
            }
            false
        }
    }

    private fun export(gpx: GPXData) {
        runInBackground {
            val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
            val success = gpxService.export(gpx, exportFile)
            withContext(Dispatchers.Main) {
                if (success) {
                    Alerts.toast(
                        requireContext(),
                        resources.getQuantityString(
                            R.plurals.beacons_exported,
                            gpx.waypoints.size,
                            gpx.waypoints.size
                        )
                    )
                } else {
                    Alerts.toast(
                        requireContext(),
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
            withContext(Dispatchers.Main) {
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
                            val count = withContext(Dispatchers.IO) {
                                importer.import(gpxToImport)
                            }
                            withContext(Dispatchers.Main) {
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

    private suspend fun getExportGPX(): GPXData {
        val groups = withContext(Dispatchers.IO) {
            if (displayedGroup == null) {
                beaconRepo.getGroupsSync().map { it.toBeaconGroup() }
            } else {
                listOf(displayedGroup!!)
            }
        }
        val beacons = withContext(Dispatchers.IO) {
            if (displayedGroup == null) {
                beaconRepo.getBeaconsSync().map { it.toBeacon() }
            } else {
                beaconRepo.getBeaconsInGroup(displayedGroup!!.id).map { it.toBeacon() }
            }
        }

        return BeaconGpxConverter().toGPX(beacons, groups)
    }

    private fun onSearch() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                updateBeaconList(true)
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBeaconListBinding {
        return FragmentBeaconListBinding.inflate(layoutInflater, container, false)
    }


    private suspend fun getBeacons(): List<IBeacon> = withContext(Dispatchers.IO) {
        val sort = NearestBeaconSort(distanceFactory, gps::location)
        val search = binding.searchbox.query?.toString()
        val group = displayedGroup?.id
        val beacons = if (search.isNullOrBlank()) {
            getBeaconsByGroup(group)
        } else {
            getBeaconsBySearch(search, group)
        }
        sort.sort(beacons)
    }

    private suspend fun getBeaconsBySearch(search: String, groupFilter: Long?) = withContext(Dispatchers.IO){
        if (groupFilter != null) {
            beaconRepo.searchBeaconsInGroup(
                search,
                groupFilter
            )
        } else {
            beaconRepo.searchBeacons(search)
        }.map { it.toBeacon() }
    }

    private suspend fun getBeaconsByGroup(group: Long?) = withContext(Dispatchers.IO){
        val signal = if (group == null) getLastSignalBeacon() else null
        (beaconService.getBeacons(displayedGroup?.id) + signal).mapNotNull { it }
    }

    private suspend fun getLastSignalBeacon(): Beacon? {
        return if (prefs.navigation.showLastSignalBeacon && prefs.backtrackSaveCellHistory) {
            beaconService.getTemporaryBeacon(BeaconOwner.CellSignal)
        } else {
            null
        }
    }

}

