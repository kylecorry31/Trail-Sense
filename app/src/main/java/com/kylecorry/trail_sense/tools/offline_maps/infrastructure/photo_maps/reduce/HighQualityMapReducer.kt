package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.MapService

class HighQualityMapReducer(
    files: FileSubsystem,
    service: MapService
) : BaseMapReduce(files, 75, Size(2048f, 2048f))
