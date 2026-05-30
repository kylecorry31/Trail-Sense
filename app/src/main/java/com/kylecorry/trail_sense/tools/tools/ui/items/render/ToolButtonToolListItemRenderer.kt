package com.kylecorry.trail_sense.tools.tools.ui.items.render

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import com.google.android.material.color.MaterialColors
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.luna.text.capitalizeWords
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemToolBinding
import com.kylecorry.trail_sense.tools.tools.ui.items.ToolListItem

class ToolButtonToolListItemRenderer : ToolListItemRenderer {
    override fun render(binding: ListItemToolBinding, item: ToolListItem) {
        val context = binding.root.context

        val backgroundColor = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorSurfaceContainerHighest
        )

        val contentColor = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorOnSurface
        )

        // Icon (start)
        binding.title.setCompoundDrawables(Resources.dp(context, 24f).toInt(), left = item.icon)
        Colors.setImageColor(
            binding.title,
            contentColor
        )

        // Icon (end)
        binding.icon.isVisible = false

        // Background
        binding.root.setBackgroundResource(R.drawable.rounded_rectangle)
        binding.root.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        binding.root.elevation = 2f

        // Text
        binding.title.setTextColor(contentColor)
        binding.title.paint.isFakeBoldText = false
        binding.title.textSize = 14f
        binding.title.text = item.title?.capitalizeWords()
        binding.title.isVisible = item.title != null

        // Click action
        binding.root.setOnClickListener {
            item.onClick(it)
        }

        // Long click action
        binding.root.setOnLongClickListener {
            item.onLongClick(it)
        }

        // Margins
        val margin = Resources.dp(context, 8f).toInt()
        val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(margin)

        // Height
        params.height = Resources.dp(context, 64f).toInt()
    }
}
