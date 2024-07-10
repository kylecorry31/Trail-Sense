package com.kylecorry.trail_sense.tools.packs.ui.commands

import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.text.slugify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.LighterPackIOService
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo

class ExportPackingListCommand(private val fragment: AndromedaFragment) : Command<Pack> {

    private val exportService = LighterPackIOService.create(fragment)

    private val repo = PackRepo.getInstance(fragment.requireContext())

    override fun execute(value: Pack) {
        fragment.inBackground(BackgroundMinimumState.Created) {
            var items: List<PackItem> = emptyList()
            Alerts.withLoading(fragment.requireContext(), fragment.getString(R.string.loading)) {
                items = repo.getItemsFromPackAsync(value.id)
            }
            exportService.export(items, "${value.name.slugify()}.csv")
            fragment.toast(fragment.getString(R.string.exported))
        }
    }
}