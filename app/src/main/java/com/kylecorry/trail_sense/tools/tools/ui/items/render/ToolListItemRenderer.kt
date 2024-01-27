package com.kylecorry.trail_sense.tools.tools.ui.items.render

import com.kylecorry.trail_sense.databinding.ListItemToolBinding
import com.kylecorry.trail_sense.tools.tools.ui.items.ToolListItem

interface ToolListItemRenderer {
    fun render(binding: ListItemToolBinding, item: ToolListItem)
}