package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPackListBinding
import com.kylecorry.trail_sense.databinding.ListItemPackBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.infrastructure.ItemRepo
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.system.tryOrNothing
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PackListFragment : BoundFragment<FragmentPackListBinding>() {

    private val packRepo by lazy { ItemRepo.getInstance(requireContext()) }
    private lateinit var packs: LiveData<List<Pack>>
    private lateinit var listView: ListView<Pack>

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

    private fun loadPacks() {
        packs = packRepo.getPacks()
        listView = ListView(binding.packList, R.layout.list_item_pack) { itemView, pack ->
            val packBinding = ListItemPackBinding.bind(itemView)
            packBinding.name.text = pack.name
            packBinding.packMenuItem.setOnClickListener {
                UiUtils.openMenu(it, R.menu.pack_item_menu) { id ->
                    when (id) {
                        R.id.action_pack_rename -> {
                            renamePack(pack)
                        }
                        R.id.action_pack_copy -> {
                            copyPack(pack)
                        }
                        R.id.action_pack_delete -> {
                            deletePack(pack)
                        }
                    }
                    true
                }
            }
            packBinding.root.setOnClickListener {
                openPack(pack.id)
            }
        }

        listView.addLineSeparator()

        packs.observe(viewLifecycleOwner) {
            listView.setData(it.sortedWith(compareBy({ it.name }, { it.id })))
        }

        binding.addBtn.setOnClickListener { createPack() }
    }

    private fun renamePack(pack: Pack) {
        CustomUiUtils.pickText(
            requireContext(),
            getString(R.string.rename),
            null,
            pack.name,
            hint = getString(R.string.name_hint)
        ) {
            if (it != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        packRepo.addPack(pack.copy(name = it))
                    }
                }
            }
        }
    }

    private fun deletePack(pack: Pack) {
        UiUtils.alertWithCancel(
            requireContext(),
            getString(R.string.delete_pack),
            pack.name
        ) { cancelled ->
            if (!cancelled) {
                lifecycleScope.launch {
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
        CustomUiUtils.pickText(
            requireContext(),
            getString(R.string.new_packing_list),
            null,
            null,
            hint = getString(R.string.name_hint)
        ) {
            if (it != null) {
                lifecycleScope.launch {
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
        CustomUiUtils.pickText(
            requireContext(),
            getString(R.string.new_packing_list),
            null,
            null,
            hint = getString(R.string.name_hint)
        ) {
            if (it != null) {
                lifecycleScope.launch {
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

}