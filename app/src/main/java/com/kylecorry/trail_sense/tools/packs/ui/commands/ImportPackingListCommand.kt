package com.kylecorry.trail_sense.tools.packs.ui.commands

import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.LighterPackIOService
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo

class ImportPackingListCommand(private val fragment: AndromedaFragment) : Command {

    private val importService = LighterPackIOService.create(fragment)
    private val repo = PackRepo.getInstance(fragment.requireContext())

    override fun execute() {
        fragment.inBackground(BackgroundMinimumState.Created) {
            var items: List<PackItem>? = null
            Alerts.withLoading(fragment.requireContext(), fragment.getString(R.string.loading)) {
                items = importService.import()
            }

            // If items are null or empty, show a toast and return
            if (items.isNullOrEmpty()) {
                fragment.toast(fragment.getString(R.string.no_items_found))
                return@inBackground
            }

            // Ask the user for the pack name
            // TODO: Default to the file name
            val name = CoroutinePickers.text(
                fragment.requireContext(),
                fragment.getString(R.string.name)
            ) ?: return@inBackground

            // Create the pack
            var packId = 0L
            Alerts.withLoading(fragment.requireContext(), fragment.getString(R.string.loading)) {
                packId = repo.addPack(Pack(0, name))
                for (item in items!!) {
                    val newItem = item.copy(packId = packId)
                    repo.addItem(newItem)
                }
            }

            // Open the pack
            val bundle = bundleOf("pack_id" to packId)
            fragment.findNavController().navigateWithAnimation(R.id.packItemListFragment, bundle)
        }
    }
}