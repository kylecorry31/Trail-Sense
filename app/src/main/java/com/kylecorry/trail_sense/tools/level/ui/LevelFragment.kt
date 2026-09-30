package com.kylecorry.trail_sense.tools.level.ui

import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useTopic
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.Toolbar
import kotlin.math.abs
import kotlin.math.hypot

class LevelFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_level) {

    override fun update() {
        // Views
        val titleView = useView<Toolbar>(R.id.level_title)
        val levelView = useView<BubbleLevel>(R.id.level)

        // Services
        val sensors = useService<SensorService>()
        val formatter = useService<FormatService>()
        val level = useMemo(sensors) { Level(sensors.getOrientation()) }

        // State
        val (x, y) = useTopic(level, level.x to -level.y) { it.x to -it.y }

        // View - Bubble
        useEffect(levelView, x, y) {
            levelView.xAngle = x
            levelView.yAngle = y
        }

        // View - Title
        useEffect(titleView, x, y) {
            val hypotenuse = hypot(x, y).coerceAtMost(90f)
            titleView.title.text = getString(
                R.string.bubble_level_angles,
                formatter.formatDegrees(abs(x), 1),
                formatter.formatDegrees(abs(y), 1),
                formatter.formatDegrees(hypotenuse, 1)
            )
        }
    }
}
