package com.kylecorry.trail_sense.tools.ballistics.ui

import com.google.android.material.button.MaterialButtonToggleGroup
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import kotlin.math.roundToInt

class FragmentToolBallistics : TrailSenseReactiveFragment(R.layout.fragment_ballistics) {

    // TODO: Allow entry of MOA per click
    private val referenceDistance = Distance(100f, DistanceUnits.Yards)
    private val referenceInchesPerClick = 0.25f

    override fun update() {
        val toolbarView = useView<Toolbar>(R.id.ballistics_title)
        val distanceToTargetView = useView<DistanceInputView>(R.id.distance_to_target)
        val missDistanceXView = useView<DistanceInputView>(R.id.miss_distance_x)
        val missDistanceYView = useView<DistanceInputView>(R.id.miss_distance_y)
        val missDirectionXView = useView<MaterialButtonToggleGroup>(R.id.miss_direction_x)
        val missDirectionYView = useView<MaterialButtonToggleGroup>(R.id.miss_direction_y)

        val formatter = useService<FormatService>()

        val (offsetX, setOffsetX) = useState(Distance(0f, DistanceUnits.Inches))
        val (offsetY, setOffsetY) = useState(Distance(0f, DistanceUnits.Inches))
        val (offsetDirectionX, setOffsetDirectionX) = useState(Direction.Left)
        val (offsetDirectionY, setOffsetDirectionY) = useState(Direction.Up)
        val (distanceToTarget, setDistanceToTarget) = useState(Distance(0f, DistanceUnits.Yards))

        useEffect(
            distanceToTargetView,
            missDistanceXView,
            missDistanceYView,
            missDirectionXView,
            missDirectionYView
        ) {
            distanceToTargetView.units =
                formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
            distanceToTargetView.hint = getString(R.string.distance_to_target)
            distanceToTargetView.setOnValueChangeListener {
                setDistanceToTarget(it ?: Distance(0f, DistanceUnits.Yards))
            }

            missDistanceXView.units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)
            missDistanceXView.setOnValueChangeListener {
                setOffsetX(it ?: Distance(0f, DistanceUnits.Inches))
            }

            missDistanceYView.units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)
            missDistanceYView.setOnValueChangeListener {
                setOffsetY(it ?: Distance(0f, DistanceUnits.Inches))
            }

            missDirectionXView.check(R.id.miss_left)
            missDirectionXView.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    setOffsetDirectionX(
                        when (checkedId) {
                            R.id.miss_left -> Direction.Left
                            R.id.miss_right -> Direction.Right
                            else -> Direction.Left
                        }
                    )
                }
            }

            missDirectionYView.check(R.id.miss_up)
            missDirectionYView.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    setOffsetDirectionY(
                        when (checkedId) {
                            R.id.miss_up -> Direction.Up
                            R.id.miss_down -> Direction.Down
                            else -> Direction.Up
                        }
                    )
                }
            }
        }


        val adjustmentX = useMemo(offsetX, offsetDirectionX, distanceToTarget) {
            getAdjustment(
                referenceInchesPerClick,
                referenceDistance,
                offsetX,
                offsetDirectionX,
                distanceToTarget
            )
        }

        val adjustmentY = useMemo(offsetY, offsetDirectionY, distanceToTarget) {
            getAdjustment(
                referenceInchesPerClick,
                referenceDistance,
                offsetY,
                offsetDirectionY,
                distanceToTarget
            )
        }

        useEffect(toolbarView, adjustmentX, adjustmentY) {
            val adjustmentTexts = mutableListOf<String>()
            if (adjustmentX != null) {
                adjustmentTexts.add("${adjustmentX.direction.name} ${adjustmentX.clicks} clicks")
            }

            if (adjustmentY != null) {
                adjustmentTexts.add("${adjustmentY.direction.name} ${adjustmentY.clicks} clicks")
            }

            toolbarView.subtitle.text = adjustmentTexts.joinToString("\n")
        }

    }


    private fun getAdjustment(
        inchesPerClick: Float,
        referenceDistance: Distance,
        offset: Distance,
        offsetDirection: Direction,
        distanceToTarget: Distance
    ): Adjustment? {
        val offsetInches = offset.convertTo(DistanceUnits.Inches).distance
        val distanceToTargetYards = distanceToTarget.convertTo(DistanceUnits.Yards).distance
        val referenceYards = referenceDistance.convertTo(DistanceUnits.Yards).distance

        if (SolMath.isZero(inchesPerClick) || SolMath.isZero(distanceToTargetYards)) {
            return null
        }

        val clicks = (offsetInches / inchesPerClick) * (referenceYards / distanceToTargetYards)
        val clickDirection = when (offsetDirection) {
            Direction.Left -> Direction.Right
            Direction.Right -> Direction.Left
            Direction.Up -> Direction.Down
            Direction.Down -> Direction.Up
        }
        return Adjustment(clicks.roundToInt(), clickDirection)
    }

    private data class Adjustment(val clicks: Int, val direction: Direction)

    private enum class Direction {
        Left, Right, Up, Down
    }
}