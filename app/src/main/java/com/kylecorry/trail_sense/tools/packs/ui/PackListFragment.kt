package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPackListBinding
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.commands.ExportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.commands.ImportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.mappers.PackAction
import com.kylecorry.trail_sense.tools.packs.ui.mappers.PackListItemMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackListFragment : BoundFragment<FragmentPackListBinding>() {

    private val packRepo by lazy { PackRepo.getInstance(requireContext()) }
    private val listMapper by lazy { PackListItemMapper(requireContext(), this::handlePackAction) }

    private var packs by state(emptyList<Pack>())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPacks()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPackListBinding {
        return FragmentPackListBinding.inflate(layoutInflater, container, false)
    }

    private fun handlePackAction(pack: Pack, action: PackAction) {
        when (action) {
            PackAction.Rename -> renamePack(pack)
            PackAction.Copy -> copyPack(pack)
            PackAction.Delete -> deletePack(pack)
            PackAction.Open -> openPack(pack.id)
            PackAction.Export -> exportPack(pack)
        }
    }

    private fun loadPacks() {
        binding.packList.emptyView = binding.emptyText

        observe(packRepo.getPacks()) {
            packs = it.sortedWith(compareBy { -it.id })
        }

        binding.addBtn.setOnClickListener { createPack() }

        bindCreateMenu()
    }

    private fun renamePack(pack: Pack) {
        Pickers.text(
            requireContext(),
            getString(R.string.rename),
            null,
            pack.name,
            hint = getString(R.string.name)
        ) {
            if (it != null) {
                inBackground {
                    withContext(Dispatchers.IO) {
                        packRepo.addPack(pack.copy(name = it))
                    }
                }
            }
        }
    }

    private fun exportPack(pack: Pack) {
        ExportPackingListCommand(this).execute(pack)
    }

    private fun deletePack(pack: Pack) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_pack),
            pack.name
        ) { cancelled ->
            if (!cancelled) {
                inBackground {
                    withContext(Dispatchers.IO) {
                        packRepo.deletePack(pack)
                    }
                }
            }
        }
    }

    private fun openPack(packId: Long) {
        tryOrNothing {
            val bundle = bundleOf("pack_id" to packId)
            findNavController().navigate(R.id.action_pack_to_pack_items, bundle)
        }
    }

    private fun createPack() {
        Pickers.text(
            requireContext(),
            getString(R.string.new_packing_list),
            null,
            null,
            hint = getString(R.string.name)
        ) {
            if (it != null) {
                inBackground {
                    val packId = withContext(Dispatchers.IO) {
                        packRepo.addPack(Pack(0, it))
                    }

                    withContext(Dispatchers.Main) {
                        openPack(packId)
                    }
                }
            }
        }
    }

    private fun copyPack(oldPack: Pack) {
        Pickers.text(
            requireContext(),
            getString(R.string.new_packing_list),
            null,
            null,
            hint = getString(R.string.name)
        ) {
            if (it != null) {
                inBackground {
                    val packId = withContext(Dispatchers.IO) {
                        packRepo.copyPack(oldPack, Pack(0, it))
                    }

                    withContext(Dispatchers.Main) {
                        openPack(packId)
                    }
                }
            }
        }
    }

    private fun bindCreateMenu() {
        binding.createMenu.setOverlay(binding.overlayMask)
        binding.createMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_import_packing_list -> {
                    setCreateMenuVisibility(false)
                    ImportPackingListCommand(this).execute()
                }

                R.id.action_create_packing_list -> {
                    setCreateMenuVisibility(false)
                    createPack()
                }
            }
            true
        }
        binding.createMenu.setOnHideListener {
            binding.addBtn.setImageResource(R.drawable.ic_add)
        }

        binding.createMenu.setOnShowListener {
            binding.addBtn.setImageResource(R.drawable.ic_cancel)
        }

        binding.addBtn.setOnClickListener {
            setCreateMenuVisibility(!isCreateMenuOpen())
        }

        onBackPressed {
            when {
                isCreateMenuOpen() -> {
                    setCreateMenuVisibility(false)
                }

                else -> {
                    remove()
                    findNavController().navigateUp()
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

    override fun onUpdate() {
        super.onUpdate()
        effect("packs", packs, lifecycleHookTrigger.onResume()) {
            binding.packList.setItems(packs, listMapper)
        }
    }

}