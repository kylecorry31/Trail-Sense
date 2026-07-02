package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class LowQualityMapReducer(
    files: FileSubsystem
) : BaseMapReduce(files, 50, Size(1024f, 1024f))
