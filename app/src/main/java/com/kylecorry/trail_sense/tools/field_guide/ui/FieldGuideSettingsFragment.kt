package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideNameDisplay

class FieldGuideSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.field_guide_preferences, rootKey)

        list(R.string.pref_field_guide_name_display)?.apply {
            entries = arrayOf(
                getString(R.string.common_name),
                getString(R.string.scientific_name),
                getString(R.string.both_names)
            )
            entryValues = arrayOf(
                FieldGuideNameDisplay.Common.id.toString(),
                FieldGuideNameDisplay.Scientific.id.toString(),
                FieldGuideNameDisplay.Both.id.toString()
            )
        }

        onClick(preference(R.string.pref_field_guide_built_in_pages_holder)) {
            showBuiltInPagesPicker()
        }
    }

    private fun showBuiltInPagesPicker() {
        inBackground {
            val service = getAppService<FieldGuideService>()
            val formatter = getAppService<FieldGuideFormatService>()
            val pages = service.getAllPages(includeHidden = true)
                .filter { it.isBuiltIn }
                .sortedBy { formatter.formatName(it).toString() }
            val selectedIndices = pages.mapIndexedNotNull { index, page ->
                index.takeUnless { page.isHidden }
            }

            onMain {
                Pickers.items(
                    requireContext(),
                    getString(R.string.built_in_pages),
                    pages.map { formatter.formatName(it) },
                    selectedIndices
                ) { selection ->
                    if (selection == null) {
                        return@items
                    }

                    val visiblePageIds = selection.map { pages[it].id }.toSet()
                    inBackground {
                        pages.filter { page ->
                            page.isHidden == visiblePageIds.contains(page.id)
                        }.forEach { page ->
                            service.setPageHidden(page.id, !page.isHidden)
                        }
                    }
                }
            }
        }
    }
}
