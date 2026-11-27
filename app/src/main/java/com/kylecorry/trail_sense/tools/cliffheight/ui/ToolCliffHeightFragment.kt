package com.kylecorry.trail_sense.tools.cliffheight.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.PressState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useShowDisclaimer
import com.kylecorry.trail_sense.shared.extensions.useTimer
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.TileButton
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.cliffheight.domain.CliffHeightService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import java.time.Duration
import java.time.Instant

class ToolCliffHeightFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_cliff_height) {

    private val service = CliffHeightService()
    private val singlePressDuration = Duration.ofMillis(200)

    @SuppressLint("ClickableViewAccessibility")
    override fun update() {
        val titleView = useView<Toolbar>(R.id.cliff_height_title)
        val startButton = useView<TileButton>(R.id.start_btn)

        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val sensors = useService<SensorSubsystem>()
        val units = useMemo(prefs, resetOnResume) { prefs.baseDistanceUnits }
        val location = useMemo(sensors, resetOnResume) { sensors.lastKnownLocation }

        val (startTime, setStartTime) = useState<Instant?>(null)
        val (endTime, setEndTime) = useState<Instant?>(null)
        val (now, setNow) = useState(Instant.now())
        val isRunning = startTime != null && endTime == null

        useShowDisclaimer(
            getString(R.string.disclaimer_message_title),
            getString(R.string.tool_cliff_height_disclaimer),
            "cache_dialog_tool_cliff_height",
            cancelText = null,
            considerShownIfCancelled = true
        )

        useTimer(16) {
            if (isRunning) {
                setNow(Instant.now())
            }
        }

        useEffect(titleView) {
            titleView.rightButton.setOnClickListener {
                UserGuideUtils.showGuide(this, R.raw.guide_tool_cliff_height)
            }
        }

        val height = startTime?.let {
            service.getCliffHeight(it, endTime ?: maxOf(it, now), location).convertTo(units)
        }

        useEffect(titleView, height, formatter) {
            titleView.title.text = height?.let { formatter.formatDistance(it, 2) } ?: ""
        }

        useEffect(startButton, isRunning) {
            startButton.setState(isRunning)
        }

        useEffect(startButton, startTime, isRunning) {
            startButton.setOnTouchListener { _, event ->
                val action = when (event.action) {
                    MotionEvent.ACTION_UP -> PressState.Up
                    MotionEvent.ACTION_DOWN -> PressState.Down
                    else -> null
                }
                val pressTime = Instant.now()
                val isHeldLongEnough = startTime != null &&
                        Duration.between(startTime, pressTime) > singlePressDuration
                when {
                    !isRunning && action == PressState.Down -> {
                        setStartTime(pressTime)
                        setEndTime(null)
                        setNow(pressTime)
                    }

                    isRunning && action == PressState.Down -> setEndTime(pressTime)
                    isRunning && action == PressState.Up && isHeldLongEnough -> setEndTime(pressTime)
                }
                true
            }
        }
    }
}
