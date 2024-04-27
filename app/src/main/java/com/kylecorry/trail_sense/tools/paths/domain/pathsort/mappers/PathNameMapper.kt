package com.kylecorry.trail_sense.tools.paths.domain.pathsort.mappers

import com.kylecorry.trail_sense.shared.grouping.mapping.ISuspendMapper
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup

class PathNameMapper : ISuspendMapper<IPath, String?> {
    override suspend fun map(item: IPath): String? {
        return if (item is Path) {
            item.name
        } else {
            (item as PathGroup).name
        }
    }
}