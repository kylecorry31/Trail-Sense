package com.kylecorry.trail_sense.navigation.paths.domain.pathsort.mappers

import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup
import com.kylecorry.trail_sense.shared.grouping.mapping.ISuspendMapper

class PathNameMapper : ISuspendMapper<IPath, String?> {
    override suspend fun map(item: IPath): String? {
        return if (item is Path) {
            item.name
        } else {
            (item as PathGroup).name
        }
    }
}