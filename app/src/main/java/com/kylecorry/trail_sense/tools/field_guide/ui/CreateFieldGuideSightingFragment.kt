package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.core.widget.addTextChangedListener
import com.google.android.material.materialswitch.MaterialSwitch
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useBindCoordinateAndElevationViews
import com.kylecorry.trail_sense.shared.extensions.useCoordinateInputView
import com.kylecorry.trail_sense.shared.extensions.useElevationInputView
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useUnsavedChangesPrompt
import com.kylecorry.trail_sense.shared.views.MaterialDateTimeInputView
import com.kylecorry.trail_sense.shared.views.Notepad
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import java.time.LocalDateTime

class CreateFieldGuideSightingFragment :
    TrailSenseReactiveFragment(R.layout.fragment_create_field_guide_sighting) {

    override fun update() {
        // Views
        val datetimeView = useView<MaterialDateTimeInputView>(R.id.time)
        val coordinateView = useCoordinateInputView(R.id.location, lifecycleHookTrigger)
        val elevationView = useElevationInputView(R.id.elevation, lifecycleHookTrigger)
        val notesView = useView<Notepad>(R.id.notes)
        val harvestedSwitchView = useView<MaterialSwitch>(R.id.harvested)
        val titleView = useView<Toolbar>(R.id.title)

        // Arguments
        val pageId = useArgument<Long>("page_id") ?: 0
        val sightingId = useArgument<Long>("sighting_id") ?: 0

        // State
        val (datetime, setDatetime) = useState<LocalDateTime?>(null)
        val (location, setLocation) = useState<Coordinate?>(null)
        val (elevation, setElevation) = useState<Distance?>(null)
        val (notes, setNotes) = useState<String?>(null)
        val (harvested, setHarvested) = useState(false)
        val (initialDatetime, setInitialDatetime) = useState<LocalDateTime?>(null)

        // Services
        val repo = useService<FieldGuideRepo>()
        val navController = useNavController()
        val context = useAndroidContext()
        val prefs = useService<UserPreferences>()

        // Memo
        val initialSighting = useBackgroundMemo(repo, sightingId) {
            if (sightingId != 0L) {
                repo.getSighting(sightingId)
            } else {
                null
            }
        }

        val hasChanges = useMemo(
            initialSighting,
            initialDatetime,
            datetime,
            location,
            elevation,
            notes,
            harvested
        ) {
            if (initialSighting != null) {
                datetime?.toZonedDateTime()?.toInstant() != initialSighting.time ||
                        location != initialSighting.location ||
                        elevation?.meters()?.value != initialSighting.altitude ||
                        notes != initialSighting.notes ||
                        harvested != initialSighting.harvested
            } else {
                datetime != initialDatetime ||
                        location != null ||
                        elevation != null ||
                        !notes.isNullOrEmpty() ||
                        harvested
            }
        }

        // Effects
        useEffect(datetimeView) {
            datetimeView.setOnItemSelectedListener {
                setDatetime(it)
            }
            val initial = LocalDateTime.now()
            setInitialDatetime(initial)
            datetimeView.setValue(initial)
        }

        useEffect(coordinateView) {
            coordinateView.setOnCoordinateChangeListener {
                setLocation(it)
            }
        }

        useEffect(elevationView) {
            elevationView.setOnElevationChangeListener {
                setElevation(it)
            }
        }

        useEffect(notesView) {
            notesView.addTextChangedListener {
                setNotes(it?.toString())
            }
        }

        useEffect(harvestedSwitchView) {
            harvestedSwitchView.setOnCheckedChangeListener { _, isChecked ->
                setHarvested(isChecked)
            }
        }

        useBackgroundEffect(
            initialSighting,
            datetimeView,
            coordinateView,
            elevationView,
            notesView,
            harvestedSwitchView,
            repo,
            prefs
        ) {
            val sighting = initialSighting ?: return@useBackgroundEffect
            val elevation = sighting.altitude?.let {
                Distance.meters(it).convertTo(prefs.baseDistanceUnits)
            }
            // Update views
            sighting.time?.let {
                datetimeView.setValue(it.toZonedDateTime().toLocalDateTime())
            }
            coordinateView.coordinate = sighting.location
            elevationView.elevation = elevation
            notesView.setText(sighting.notes)
            harvestedSwitchView.isChecked = sighting.harvested == true

            // Update state
            setDatetime(sighting.time?.toZonedDateTime()?.toLocalDateTime())
            setLocation(sighting.location)
            setElevation(elevation)
            setNotes(sighting.notes)
            setHarvested(sighting.harvested == true)
        }

        useEffect(
            titleView,
            context,
            navController,
            repo,
            sightingId,
            pageId,
            datetime,
            location,
            elevation,
            harvested,
            notes
        ) {
            CustomUiUtils.setButtonState(titleView.rightButton, true)
            titleView.rightButton.setOnClickListener {
                inBackground {
                    Alerts.withLoading(context, getString(R.string.saving)) {
                        repo.addSighting(
                            Sighting(
                                sightingId,
                                pageId,
                                datetime?.toZonedDateTime()?.toInstant(),
                                location,
                                elevation?.meters()?.value,
                                harvested,
                                notes
                            )
                        )
                        onMain {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }

        useBindCoordinateAndElevationViews(coordinateView, elevationView)

        useUnsavedChangesPrompt(hasChanges)
    }
}