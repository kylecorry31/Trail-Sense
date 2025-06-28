package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.reduce

import android.content.Context
import com.kylecorry.sol.math.geometry.Size

class MediumQualityMapReducer(context: Context): BaseMapReduce(context, 50, Size(2048f, 2048f))