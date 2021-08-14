package com.kylecorry.trail_sense.navigation.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.files.ExternalFileService
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBeaconListBinding
import com.kylecorry.trail_sense.navigation.domain.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.infrastructure.export.BeaconIOService
import com.kylecorry.trail_sense.navigation.infrastructure.export.JsonBeaconImporter
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.domain.navigation.BeaconOwner
import com.kylecorry.trailsensecore.domain.navigation.IBeacon
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant


class BeaconListFragment : BoundFragment<FragmentBeaconListBinding>() {

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val externalFileService by lazy { ExternalFileService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private lateinit var beaconList: ListView<IBeacon>
    private lateinit var navController: NavController
    private val sensorService by lazy { SensorService(requireContext()) }
    private var displayedGroup: BeaconGroup? = null

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
            val loc: MyNamedCoordinate? = requireArguments().getParcelable("initial_location")
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

        binding.importExportBeacons.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.apply {
                setTitle(getString(R.string.import_export_beacons))
                setPositiveButton(getString(R.string.import_btn)) { dialog, _ ->
                    importBeacons()
                    dialog.dismiss()
                }
                setNeutralButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                setNegativeButton(getString(R.string.export)) { dialog, _ ->
                    exportBeacons()
                    dialog.dismiss()
                }
            }

            val dialog = builder.create()
            dialog.show()
        }

        binding.overlayMask.setOnClickListener {
            // TODO: Maybe collapse the fab menu
        }

        binding.createMenu.setOverlay(binding.overlayMask)
        binding.createMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_import_qr_beacon -> {
                    requestPermissions(
                        listOf(Manifest.permission.CAMERA)
                    ) {
                        if (Camera.isAvailable(requireContext())) {
                            importBeaconFromQR()
                        } else {
                            UiUtils.longToast(
                                requireContext(),
                                getString(R.string.camera_permission_denied)
                            )
                        }
                    }
                    setCreateMenuVisibility(false)
                }
                R.id.action_import_gpx_beacons -> {
                    importBeacons()
                    setCreateMenuVisibility(false)
                }
                R.id.action_create_beacon_group -> {
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
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    beaconRepo.addBeaconGroup(BeaconGroupEntity(text ?: ""))
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
        binding.createBtn.setImageResource(if (!isShowing) R.drawable.ic_plus else R.drawable.ic_cancel)
    }

    private fun isCreateMenuOpen(): Boolean {
        return binding.createMenu.isVisible
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
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                beaconRepo.addBeaconGroup(
                                    BeaconGroupEntity.from(
                                        beacon.copy(
                                            name = text ?: ""
                                        )
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

    private suspend fun updateBeaconList(resetScroll: Boolean = false) {
        if (!isBound) {
            return
        }

        val beacons = withContext(Dispatchers.IO) {
            val search = binding.searchbox.query
            if (!search.isNullOrBlank()) {
                val all = if (displayedGroup != null) {
                    beaconRepo.searchBeaconsInGroup(
                        binding.searchbox.query.toString(),
                        displayedGroup?.id
                    )
                } else {
                    beaconRepo.searchBeacons(binding.searchbox.query.toString())
                }
                all.sortedBy {
                    it.coordinate.distanceTo(gps.location)
                }.map { it.toBeacon() }
            } else if (displayedGroup == null) {
                val ungrouped = beaconRepo.getBeaconsInGroup(null).sortedBy {
                    it.coordinate.distanceTo(gps.location)
                }.map { it.toBeacon() }

                val groups = beaconRepo.getGroupsSync().sortedBy {
                    it.name
                }.map { it.toBeaconGroup() }

                val signal =
                    if (prefs.navigation.showLastSignalBeacon && prefs.backtrackSaveCellHistory) {
                        beaconRepo.getTemporaryBeacon(BeaconOwner.CellSignal)?.toBeacon()
                    } else {
                        null
                    }

                val all = (ungrouped + groups + listOfNotNull(signal)).map {
                    if (it is Beacon) {
                        Pair(it, it.coordinate.distanceTo(gps.location))
                    } else {
                        val groupBeacons = beaconRepo.getBeaconsInGroup(it.id).map { b ->
                            b.coordinate.distanceTo(gps.location)
                        }.minOrNull()
                        Pair(it, groupBeacons ?: Float.POSITIVE_INFINITY)
                    }
                }

                all.sortedBy { it.second }.map { it.first }
            } else {
                beaconRepo.getBeaconsInGroup(displayedGroup?.id).sortedBy {
                    it.coordinate.distanceTo(gps.location)
                }.map { it.toBeacon() }
            }
        }

        withContext(Dispatchers.Main) {
            context ?: return@withContext
            binding.beaconTitle.text =
                displayedGroup?.name ?: getString(R.string.select_beacon)
            updateBeaconEmptyText(beacons.isNotEmpty())
            beaconList.setData(beacons)
            if (resetScroll) {
                beaconList.scrollToPosition(0, false)
            }
        }
    }

    private fun importBeaconFromQR() {
        val sheet = BeaconImportQRBottomSheet()
        sheet.onBeaconScanned = {
            val bundle = bundleOf("initial_location" to it)
            sheet.dismiss()
            navController.navigate(
                R.id.action_beaconListFragment_to_placeBeaconFragment,
                bundle
            )
        }
        sheet.show(this)
    }

    private fun exportBeacons() {
        val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
        createFile(exportFile, "application/gpx+xml") {
            it?.also { returnUri ->
                exportToUri(returnUri)
            }
        }
    }

    private fun importBeacons() {
        pickFile(
            "*/*",
            getString(R.string.select_import_file)
        ) {
            it?.also { returnUri ->
                importFromUri(returnUri)
            }
        }
    }

    private fun importFromUri(uri: Uri) {
        lifecycleScope.launch {
            val text = externalFileService.read(uri)
            text?.let {
                if (text.startsWith("{")) {
                    // Legacy
                    val importer = JsonBeaconImporter(requireContext())
                    val count = withContext(Dispatchers.IO) {
                        importer.import(text)
                    }
                    withContext(Dispatchers.Main) {
                        UiUtils.shortToast(
                            requireContext(),
                            resources.getQuantityString(R.plurals.beacons_imported, count, count)
                        )
                        updateBeaconList()
                    }
                } else {
                    val importer = BeaconIOService(requireContext())
                    val waypoints = withContext(Dispatchers.IO) {
                        importer.getGPXWaypoints(text)
                    }
                    if (waypoints.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            // TODO: Allow user to choose which beacons to import
                            UiUtils.alertWithCancel(
                                requireContext(),
                                resources.getQuantityString(
                                    R.plurals.import_beacons,
                                    waypoints.size,
                                    waypoints.size
                                ),
                                "",
                                getString(R.string.dialog_ok),
                                getString(R.string.dialog_cancel)
                            ) { cancelled ->
                                if (!cancelled) {
                                    lifecycleScope.launch {
                                        val count = withContext(Dispatchers.IO) {
                                            importer.import(waypoints)
                                        }
                                        withContext(Dispatchers.Main) {
                                            UiUtils.shortToast(
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

            }
        }
    }


    private fun exportToUri(uri: Uri) {
        lifecycleScope.launch {
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

            val gpx = BeaconIOService(requireContext()).export(beacons, groups)


            val success = withContext(Dispatchers.IO) {
                externalFileService.write(uri, gpx)
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    UiUtils.shortToast(
                        requireContext(),
                        resources.getQuantityString(
                            R.plurals.beacons_exported,
                            beacons.size,
                            beacons.size
                        )
                    )
                } else {
                    UiUtils.shortToast(
                        requireContext(),
                        getString(R.string.beacon_export_error)
                    )
                }
            }
        }
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
}

