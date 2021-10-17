package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.paths.LineStyle
import com.kylecorry.trail_sense.shared.paths.PathPointColoringStyle
import java.time.Duration

interface IBacktrackPreferences {
    val backtrackPathColor: AppColor
    val backtrackPathStyle: LineStyle
    val backtrackPointStyle: PathPointColoringStyle
    val backtrackHistory: Duration
}