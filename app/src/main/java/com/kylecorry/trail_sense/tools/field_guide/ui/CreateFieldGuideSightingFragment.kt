package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useCoordinateInputView
import com.kylecorry.trail_sense.shared.extensions.useElevationInputView
import com.kylecorry.trail_sense.shared.views.MaterialDateTimeInputView
import com.kylecorry.trail_sense.shared.views.Notepad

class CreateFieldGuideSightingFragment :
    TrailSenseReactiveFragment(R.layout.fragment_create_field_guide_sighting) {

    override fun update() {
        // Views
        val datetimeView = useView<MaterialDateTimeInputView>(R.id.time)
        val coordinateView = useCoordinateInputView(R.id.location, lifecycleHookTrigger)
        val elevationView = useElevationInputView(R.id.elevation, lifecycleHookTrigger)
        val notesView = useView<Notepad>(R.id.notes)
        val titleView = useView<Toolbar>(R.id.title)

        // State

        // Services
    }
}