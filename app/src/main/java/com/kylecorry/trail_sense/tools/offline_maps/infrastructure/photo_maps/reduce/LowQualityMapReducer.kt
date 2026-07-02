package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.MapService

class LowQualityMapReducer(
    files: FileSubsystem,
    service: MapService
) : BaseMapReduce(files, service, 50, Size(1024f, 1024f))
