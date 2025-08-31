package com.kylecorry.trail_sense.tools.field_guide.ui

import android.widget.TextView
import androidx.core.os.bundleOf
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class SightingListFragment : TrailSenseReactiveFragment(R.layout.fragment_sightings_list) {
    override fun update() {
        // Views
        val listView = useView<AndromedaListView>(R.id.list)
        val emptyTextView = useView<TextView>(R.id.sightings_empty_text)
        val createButtonView = useView<FloatingActionButton>(R.id.create_btn)

        // Services
        val formatter = useService<FormatService>()
        val repo = useService<FieldGuideRepo>()
        val navController = useNavController()
        val context = useAndroidContext()

        // Arguments
        val pageId = useArgument<Long>("page_id") ?: 0

        // State
        val (deleteKey, setDeleteKey) = useState(0)

        val sightings = useBackgroundMemo(repo, pageId, deleteKey, resetOnResume) {
            repo.getPage(pageId)?.sightings?.sortedByDescending { it.time }
        } ?: emptyList()

        val deleteSighting = useCallback(context, repo, deleteKey) { sighting: Sighting ->
            Alerts.dialog(
                context,
                getString(R.string.delete),
                sighting.time?.let { time -> formatter.formatRelativeDateTime(time.toZonedDateTime()) }
                    ?: getString(R.string.sighting)) { cancelled ->
                if (!cancelled) {
                    inBackground {
                        repo.deleteSighting(sighting)
                        setDeleteKey(deleteKey + 1)
                    }
                }
            }

        }

        val sightingListItems = useMemo(formatter, sightings, deleteSighting) {
            sightings.map {
                ListItem(
                    it.id,
                    it.time?.let { time -> formatter.formatRelativeDateTime(time.toZonedDateTime()) }
                        ?: getString(R.string.sighting),
                    it.notes,
                    tags = listOfNotNull(
                        if (it.harvested == true) ListItemTag(
                            getString(R.string.harvested),
                            null,
                            Resources.androidTextColorSecondary(context)
                        ) else null
                    ),
                    menu = listOf(
                        ListMenuItem(getString(R.string.delete)) {
                            deleteSighting(it)
                        }
                    )
                ) {
                    // TODO: Edit on click
                }
            }
        }

        // Effects
        useEffect(listView, emptyTextView) {
            listView.emptyView = emptyTextView
        }

        useEffect(listView, sightingListItems) {
            listView.setItems(sightingListItems)
        }

        useEffect(createButtonView, pageId, navController) {
            createButtonView.setOnClickListener {
                navController.navigateWithAnimation(
                    R.id.createFieldGuideSightingFragment,
                    bundleOf("page_id" to pageId)
                )
            }
        }

    }
}