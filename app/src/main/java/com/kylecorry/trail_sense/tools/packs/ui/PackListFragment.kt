package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPackListBinding
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.observe
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.mappers.PackAction
import com.kylecorry.trail_sense.tools.packs.ui.mappers.PackListItemMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackListFragment : BoundFragment<FragmentPackListBinding>() {

    private val packRepo by lazy { PackRepo.getInstance(requireContext()) }
    private lateinit var packs: LiveData<List<Pack>>
    private val listMapper by lazy { PackListItemMapper(requireContext(), this::handlePackAction) }

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
        }
    }

    private fun loadPacks() {
        packs = packRepo.getPacks()
        binding.packList.emptyView = binding.emptyText

        observe(packs) {
            binding.packList.setItems(it.sortedWith(compareBy { -it.id }), listMapper)
        }

        binding.addBtn.setOnClickListener { createPack() }
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

}