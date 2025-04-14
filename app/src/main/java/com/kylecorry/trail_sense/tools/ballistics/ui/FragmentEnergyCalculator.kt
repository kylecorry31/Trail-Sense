package com.kylecorry.trail_sense.tools.ballistics.ui

import android.widget.TextView
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.shared.views.WeightInputView

class FragmentEnergyCalculator : TrailSenseReactiveFragment(R.layout.fragment_energy_calculator) {

    override fun update() {
        val bulletSpeedView = useView<DistanceInputView>(R.id.bullet_speed)
        val bulletWeightView = useView<WeightInputView>(R.id.bullet_weight)
        val energyView = useView<TextView>(R.id.energy_amount)

        val formatter = useService<FormatService>()

        val (bulletSpeed, setBulletSpeed) = useState(
            Speed(
                0f,
                DistanceUnits.Feet,
                TimeUnits.Seconds
            )
        )

        val (bulletWeight, setBulletWeight) = useState<Weight?>(null)

        useEffect(bulletSpeedView, bulletWeightView) {
            bulletSpeedView.units = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
            bulletSpeedView.hint = getString(R.string.muzzle_velocity)
            bulletSpeedView.setOnValueChangeListener {
                setBulletSpeed(
                    Speed(
                        it?.distance ?: 0f,
                        it?.units ?: DistanceUnits.Feet,
                        TimeUnits.Seconds
                    )
                )
            }

            bulletWeightView.units = formatter.sortWeightUnits(WeightUnits.entries)
            // TODO: Select grains or grams depending on metric or imperial settings
            bulletWeightView.hint = getString(R.string.bullet_weight)
            bulletWeightView.setOnValueChangeListener {
                setBulletWeight(it)
            }
        }

        val energy = useMemo(bulletSpeed, bulletWeight) {
            getBulletEnergy(bulletSpeed, bulletWeight ?: return@useMemo null)
        }

        useEffect(energyView, energy) {
            if (energy == null) {
                energyView.text = null
                return@useEffect
            }

            // TODO: Add energy types to Sol and format accordingly
            val formatted = DecimalFormatter.format(
                energy * 0.73756f,
                1
            )

            energyView.text = "$formatted FPE"
        }


    }

    private fun getBulletEnergy(bulletSpeed: Speed, bulletWeight: Weight): Float {
        val speedMs = bulletSpeed.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).speed
        val weightKg = bulletWeight.convertTo(WeightUnits.Kilograms).weight
        return 0.5f * weightKg * speedMs * speedMs
    }
}