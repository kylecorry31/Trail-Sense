package com.kylecorry.trail_sense.tools.tools.ui.items.render

import com.kylecorry.trail_sense.databinding.ListItemToolBinding
import com.kylecorry.trail_sense.tools.tools.ui.items.ToolListItem
import com.kylecorry.trail_sense.tools.tools.ui.items.ToolListItemStyle

class DelegateToolListItemRenderer: ToolListItemRenderer {

    private val toolRenderer = ToolButtonToolListItemRenderer()
    private val categoryRenderer = CategoryToolListItemRenderer()
    private val headerRenderer = HeaderToolListItemRenderer()

    override fun render(binding: ListItemToolBinding, item: ToolListItem) {
        when (item.style) {
            ToolListItemStyle.Tool -> toolRenderer.render(binding, item)
            ToolListItemStyle.Category -> categoryRenderer.render(binding, item)
            ToolListItemStyle.Header -> headerRenderer.render(binding, item)
        }
    }
}