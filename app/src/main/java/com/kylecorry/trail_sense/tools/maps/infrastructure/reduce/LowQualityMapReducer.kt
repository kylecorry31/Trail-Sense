package com.kylecorry.trail_sense.tools.maps.infrastructure.reduce

import android.content.Context
import com.kylecorry.sol.math.geometry.Size

class LowQualityMapReducer(context: Context) : BaseMapReduce(context, 50, Size(1024f, 1024f))