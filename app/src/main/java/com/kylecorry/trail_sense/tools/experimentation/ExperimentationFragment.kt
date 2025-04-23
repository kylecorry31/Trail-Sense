package com.kylecorry.trail_sense.tools.experimentation

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import java.time.Duration

class ExperimentationFragment : TrailSenseReactiveFragment(R.layout.fragment_experimentation) {
    override fun update() {
        val context = useAndroidContext()
        val haptics = useMemo {
            HapticSubsystem.getInstance(context)
        }

        useEffectWithCleanup {
            haptics.interval(Duration.ofMillis(700), Duration.ofMillis(900))
            return@useEffectWithCleanup {
                haptics.off()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        resetHooks()
    }
}