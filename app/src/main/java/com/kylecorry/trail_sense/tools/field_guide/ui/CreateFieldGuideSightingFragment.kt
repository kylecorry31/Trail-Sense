package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.core.widget.addTextChangedListener
import com.google.android.material.materialswitch.MaterialSwitch
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useCoordinateInputView
import com.kylecorry.trail_sense.shared.extensions.useElevationInputView
import com.kylecorry.trail_sense.shared.extensions.useNavController
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

        // State
        val (datetime, setDatetime) = useState<LocalDateTime?>(null)
        val (location, setLocation) = useState<Coordinate?>(null)
        val (elevation, setElevation) = useState<Distance?>(null)
        val (notes, setNotes) = useState<String?>(null)
        val (harvested, setHarvested) = useState(false)

        // Services
        val repo = useService<FieldGuideRepo>()
        val navController = useNavController()
        val context = useAndroidContext()

        // Effects
        useEffect(datetimeView) {
            datetimeView.setOnItemSelectedListener {
                setDatetime(it)
            }
            datetimeView.setValue(LocalDateTime.now())
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

        useEffect(
            titleView,
            context,
            navController,
            repo,
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
                                0,
                                pageId,
                                datetime?.toZonedDateTime()?.toInstant(),
                                location,
                                elevation?.meters()?.distance,
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

        // TODO: Add support for editing an existing sighting
        // TODO: Add change detection
    }
}