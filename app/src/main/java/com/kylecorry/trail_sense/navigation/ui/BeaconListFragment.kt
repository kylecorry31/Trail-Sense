package com.kylecorry.trail_sense.navigation.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentBeaconListBinding
import com.kylecorry.trail_sense.navigation.domain.BeaconGroupEntity
import com.kylecorry.trail_sense.navigation.infrastructure.export.BeaconIOService
import com.kylecorry.trail_sense.navigation.infrastructure.export.JsonBeaconImporter
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.domain.navigation.IBeacon
import com.kylecorry.trailsensecore.infrastructure.persistence.ExternalFileService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant


class BeaconListFragment : Fragment() {

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val externalFileService by lazy { ExternalFileService(requireContext()) }

    private var _binding: FragmentBeaconListBinding? = null
    private val binding get() = _binding!!
    private lateinit var beaconList: ListView<IBeacon>
    private lateinit var navController: NavController
    private val sensorService by lazy { SensorService(requireContext()) }
    private var displayedGroup: BeaconGroup? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBeaconListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val beaconRecyclerView = binding.beaconRecycler
        beaconList =
            ListView(beaconRecyclerView, R.layout.list_item_beacon, this::updateBeaconListItem)
        beaconList.addLineSeparator()
        navController = findNavController()

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                updateBeaconList()
            }
        }

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
            when (it.itemId){
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
                            updateBeaconList()
                        }
                    }
                }
                else -> {
                    remove()
                    requireActivity().onBackPressed()
                }
            }
        }
    }

    private fun setCreateMenuVisibility(isShowing: Boolean){
        if (isShowing){
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
        super.onPause()
    }

    private fun onLocationUpdate(): Boolean {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                updateBeaconList()
            }
        }
        return false
    }

    private fun updateBeaconEmptyText(hasBeacons: Boolean) {
        if (!hasBeacons) {
            binding.beaconEmptyText.visibility = View.VISIBLE
        } else {
            binding.beaconEmptyText.visibility = View.GONE
        }
    }

    private fun updateBeaconListItem(itemView: View, beacon: IBeacon) {
        if (beacon is Beacon) {
            val listItem = BeaconListItem(itemView, lifecycleScope, beacon, gps.location)
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
                val bundle = bundleOf("destination" to beacon.id)
                navController.navigate(R.id.action_beacon_list_to_action_navigation, bundle)
            }

            listItem.onDeleted = {
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
                        updateBeaconList()
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

    private suspend fun updateBeaconList() {
        context ?: return

        val beacons = withContext(Dispatchers.IO) {
            if (displayedGroup == null) {
                val ungrouped = beaconRepo.getBeaconsInGroup(null).sortedBy {
                    it.coordinate.distanceTo(gps.location)
                }.map { it.toBeacon() }

                val groups = beaconRepo.getGroupsSync().sortedBy {
                    it.name
                }.map { it.toBeaconGroup() }

                val all = (ungrouped + groups).map {
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
            _binding ?: return@withContext
            binding.beaconTitle.text = displayedGroup?.name ?: getString(R.string.select_beacon)
            updateBeaconEmptyText(beacons.isNotEmpty())
            beaconList.setData(beacons)
        }
    }

    private fun exportBeacons() {
        val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
        val intent = IntentUtils.createFile(exportFile, "application/gpx+xml")
        startActivityForResult(intent, REQUEST_CODE_EXPORT)
    }

    private fun importBeacons() {
        val requestFileIntent = IntentUtils.pickFile(
            "*/*",
            getString(R.string.select_import_file)
        )
        startActivityForResult(requestFileIntent, REQUEST_CODE_IMPORT)
    }

    private fun importFromUri(uri: Uri) {
        lifecycleScope.launch {
            val text = externalFileService.read(uri)
            text?.let {
                val count = if (text.startsWith("{")) {
                    // Legacy
                    val importer = JsonBeaconImporter(requireContext())
                    withContext(Dispatchers.IO) {
                        importer.import(text)
                    }
                } else {
                    val importer = BeaconIOService(requireContext())
                    withContext(Dispatchers.IO) {
                        importer.import(text)
                    }
                }
                withContext(Dispatchers.Main) {
                    UiUtils.shortToast(
                        requireContext(),
                        getString(R.string.beacons_imported, count)
                    )
                    updateBeaconList()
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
                        getString(R.string.beacons_exported, beacons.size)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMPORT && resultCode == Activity.RESULT_OK) {
            data?.data?.also { returnUri ->
                importFromUri(returnUri)
            }
        } else if (requestCode == REQUEST_CODE_EXPORT && resultCode == Activity.RESULT_OK) {
            data?.data?.also { returnUri ->
                exportToUri(returnUri)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_IMPORT = 6
        private const val REQUEST_CODE_EXPORT = 7
    }
}

