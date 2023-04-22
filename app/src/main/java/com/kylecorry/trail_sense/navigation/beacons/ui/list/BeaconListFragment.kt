package com.kylecorry.trail_sense.navigation.beacons.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.filterIndices
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBeaconListBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands.CreateBeaconGroupCommand
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands.DeleteBeaconCommand
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands.MoveBeaconCommand
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands.MoveBeaconGroupCommand
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.commands.RenameBeaconGroupCommand
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.export.BeaconGpxConverter
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.export.BeaconGpxImporter
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.loading.BeaconLoader
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.BeaconSortMethod
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.MostRecentBeaconSort
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.NameBeaconSort
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.onBackPressed
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.lists.GroupListManager
import com.kylecorry.trail_sense.shared.lists.bind
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import java.time.Instant


class BeaconListFragment : BoundFragment<FragmentBeaconListBinding>() {

    private val gps by lazy { sensorService.getGPS() }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private lateinit var navController: NavController
    private val sensorService by lazy { SensorService(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val beaconService by lazy { BeaconService(requireContext()) }
    private val beaconLoader by lazy { BeaconLoader(beaconService, prefs.navigation) }
    private var sort = BeaconSortMethod.Closest

    private val listMapper by lazy {
        IBeaconListItemMapper(
            requireContext(),
            gps,
            this::handleBeaconAction,
            this::handleBeaconGroupAction
        )
    }

    private var initialLocation: GeoUri? = null

    private val gpxService by lazy {
        IOFactory().createGpxService(this)
    }

    private lateinit var manager: GroupListManager<IBeacon>
    private var lastRoot: IBeacon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireArguments().containsKey("initial_location")) {
            initialLocation = requireArguments().getParcelable("initial_location")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()

        sort = prefs.navigation.beaconSort

        binding.beaconRecycler.emptyView = binding.beaconEmptyText
        manager = GroupListManager(
            lifecycleScope,
            beaconLoader,
            lastRoot,
            this::sortBeacons
        )

        manager.bind(binding.searchbox)
        manager.bind(binding.beaconRecycler, binding.beaconTitle.title, listMapper) {
            it?.name ?: getString(R.string.beacons)
        }

        initialLocation?.let {
            initialLocation = null
            createBeacon(initialLocation = it)
        }

        binding.beaconTitle.rightButton.setOnClickListener {
            val defaultSort = prefs.navigation.beaconSort
            Pickers.menu(
                it, listOf(
                    getString(R.string.sort_by, getSortString(defaultSort)),
                    getString(R.string.export)
                )
            ) { selected ->
                when (selected) {
                    0 -> changeSort()
                    1 -> onExportBeacons()
                }
                true
            }
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
                    val command =
                        CreateBeaconGroupCommand(requireContext(), lifecycleScope, beaconService) {
                            refresh()
                        }
                    command.execute(manager.root?.id)
                    setCreateMenuVisibility(false)
                }
                R.id.action_create_beacon -> {
                    setCreateMenuVisibility(false)
                    createBeacon(group = manager.root?.id)
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
                else -> {
                    if (!manager.up()) {
                        remove()
                        navController.navigateUp()
                    }
                }
            }
        }
    }

    private suspend fun sortBeacons(beacons: List<IBeacon>): List<IBeacon> {
        val method = when (sort) {
            BeaconSortMethod.MostRecent -> MostRecentBeaconSort(beaconService)
            BeaconSortMethod.Closest -> ClosestBeaconSort(beaconService, gps::location)
            BeaconSortMethod.Name -> NameBeaconSort()
        }
        return method.sort(beacons)
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
        inBackground {
            val gpx = getExportGPX()
            onMain {
                Pickers.items(
                    requireContext(),
                    getString(R.string.export),
                    gpx.waypoints.map { it.name ?: formatService.formatLocation(it.coordinate) },
                    List(gpx.waypoints.size) { it },
                ) {
                    if (!it.isNullOrEmpty()) {
                        val selectedWaypoints = gpx.waypoints.filterIndices(it)
                        export(gpx.copy(waypoints = selectedWaypoints))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        manager.refresh()
        // Get a GPS reading
        gps.start(this::onLocationUpdate)
    }

    override fun onPause() {
        gps.stop(this::onLocationUpdate)
        tryOrNothing {
            lastRoot = manager.root
        }
        super.onPause()
    }

    private fun onLocationUpdate(): Boolean {
        return false
    }

    private fun importBeaconFromQR() {
        val encoder = BeaconQREncoder()
        CustomUiUtils.scanQR(this, getString(R.string.beacon_qr_import_instructions)) {
            if (it != null) {
                val beacon = encoder.decode(it) ?: return@scanQR true
                createBeacon(group = manager.root?.id, initialLocation = GeoUri.from(beacon))
            }
            false
        }
    }

    private fun changeSort() {
        val sortOptions = BeaconSortMethod.values()
        Pickers.item(
            requireContext(),
            getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.navigation.beaconSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.navigation.beaconSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: BeaconSortMethod): String {
        return when (sortMethod) {
            BeaconSortMethod.MostRecent -> getString(R.string.most_recent)
            BeaconSortMethod.Closest -> getString(R.string.closest)
            BeaconSortMethod.Name -> getString(R.string.name)
        }
    }

    private fun onSortChanged() {
        manager.refresh(true)
    }

    private fun export(gpx: GPXData) {
        inBackground(BackgroundMinimumState.Created) {
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
        inBackground(BackgroundMinimumState.Created) {
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
                        inBackground {
                            val gpxToImport = GPXData(waypoints.filterIndices(it), emptyList())
                            val count = onIO {
                                importer.import(gpxToImport, manager.root?.id)
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
                                refresh()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getExportGPX(): GPXData = onIO {
        val all = beaconService.getBeacons(
            manager.root?.id,
            includeGroups = true,
            maxDepth = null,
            includeRoot = true
        )

        val groups = all.filterIsInstance<BeaconGroup>()
        val beacons = all.filterIsInstance<Beacon>()

        BeaconGpxConverter().toGPX(beacons, groups)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBeaconListBinding {
        return FragmentBeaconListBinding.inflate(layoutInflater, container, false)
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

    private fun refresh() {
        manager.refresh()
    }

    private fun viewBeacon(id: Long) {
        val bundle = bundleOf("beacon_id" to id)
        navController.navigate(
            R.id.action_beacon_list_to_beaconDetailsFragment,
            bundle
        )
    }

    private fun navigate(beacon: Beacon) {
        PreferencesSubsystem.getInstance(requireContext()).preferences.putLong("last_beacon_id_long", beacon.id)
        // TODO: Confirm it is always navigate up that gets there
        navController.navigateUp()
    }

    private fun delete(beacon: Beacon) {
        val command = DeleteBeaconCommand(
            requireContext(),
            lifecycleScope,
            beaconService
        ) {
            refresh()
        }
        command.execute(beacon)
    }

    private fun delete(group: BeaconGroup) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete),
            getString(
                R.string.delete_beacon_group_message,
                group.name
            ),
        ) { cancelled ->
            if (!cancelled) {
                inBackground {
                    onIO {
                        beaconService.delete(group)
                    }

                    onMain {
                        refresh()
                    }
                }

            }
        }
    }

    private fun move(beacon: Beacon) {
        val command = MoveBeaconCommand(
            requireContext(),
            lifecycleScope,
            beaconService
        ) {
            refresh()
        }
        command.execute(beacon)
    }

    private fun move(group: BeaconGroup) {
        val command =
            MoveBeaconGroupCommand(requireContext(), lifecycleScope, beaconService) { refresh() }
        command.execute(group)
    }

    private fun toggleVisibility(beacon: Beacon) {
        inBackground {
            val newBeacon = beacon.copy(visible = !beacon.visible)

            onIO {
                beaconService.add(newBeacon)
            }

            onMain {
                refresh()
            }
        }
    }

    private fun rename(group: BeaconGroup) {
        val command =
            RenameBeaconGroupCommand(requireContext(), lifecycleScope, beaconService) { refresh() }
        command.execute(group)
    }

    private fun handleBeaconGroupAction(group: BeaconGroup, action: BeaconGroupAction) {
        when (action) {
            BeaconGroupAction.Open -> {
                manager.open(group.id)
            }
            BeaconGroupAction.Edit -> rename(group)
            BeaconGroupAction.Delete -> delete(group)
            BeaconGroupAction.Move -> move(group)
        }
    }

    private fun handleBeaconAction(beacon: Beacon, action: BeaconAction) {
        when (action) {
            BeaconAction.View -> viewBeacon(beacon.id)
            BeaconAction.Edit -> createBeacon(editingBeaconId = beacon.id)
            BeaconAction.Navigate -> navigate(beacon)
            BeaconAction.Delete -> delete(beacon)
            BeaconAction.Move -> move(beacon)
            BeaconAction.ToggleVisibility -> toggleVisibility(beacon)
            BeaconAction.Share -> BeaconSender(this).send(beacon)
        }
    }
}

