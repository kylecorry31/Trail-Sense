package com.kylecorry.trail_sense.tools.tools.ui.items.render

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.luna.text.capitalizeWords
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.trail_sense.databinding.ListItemToolBinding
import com.kylecorry.trail_sense.tools.tools.ui.items.ToolListItem

class HeaderToolListItemRenderer : ToolListItemRenderer {
    override fun render(binding: ListItemToolBinding, item: ToolListItem) {
        val context = binding.root.context

        binding.title.setCompoundDrawables()

        if (item.icon == null) {
            binding.icon.isVisible = false
        } else {
            binding.icon.isVisible = true
            binding.icon.setImageResource(item.icon)
            Colors.setImageColor(
                binding.icon,
                Resources.androidTextColorPrimary(context)
            )
        }

        binding.root.background = null
        binding.root.elevation = 0f

        binding.title.setTextColor(Resources.androidTextColorPrimary(context))
        binding.title.paint.isFakeBoldText = false
        binding.title.textSize = 24f
        binding.title.text = item.title?.capitalizeWords()
        binding.title.isVisible = item.title != null

        binding.root.setOnClickListener(null)
        binding.root.setOnLongClickListener(null)

        binding.icon.setOnClickListener {
            item.onClick(it)
        }

        binding.icon.setOnLongClickListener {
            item.onLongClick(it)
        }

        val margins = Resources.dp(context, 8f).toInt()
        val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(0, margins * 2, 0, margins)

        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
