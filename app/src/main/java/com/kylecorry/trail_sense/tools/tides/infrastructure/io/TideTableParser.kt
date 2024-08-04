package com.kylecorry.trail_sense.tools.tides.infrastructure.io

import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import java.io.InputStream

interface TideTableParser {
    fun parse(stream: InputStream): TideTable?
}