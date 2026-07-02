package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class HighQualityMapReducer(
    files: FileSubsystem
) : BaseMapReduce(files, 75, Size(2048f, 2048f))
