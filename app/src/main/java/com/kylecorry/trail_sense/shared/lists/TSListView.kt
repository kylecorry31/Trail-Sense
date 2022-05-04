package com.kylecorry.trail_sense.shared.lists

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconMenuBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.setCompoundDrawables
import com.kylecorry.trail_sense.shared.colors.ColorUtils

class TSListView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {

    private val list =
        ListView(this, R.layout.list_item_plain_icon_menu) { view: View, listItem: ListItem ->
            val binding = ListItemPlainIconMenuBinding.bind(view)
            binding.title.text = listItem.title

            if (listItem.checkbox != null) {
                binding.checkbox.isChecked = listItem.checkbox.checked
                binding.checkbox.setOnClickListener { listItem.checkbox.onClick() }
                binding.checkbox.isVisible = true
            } else {
                binding.checkbox.isVisible = false
            }

            if (listItem.subtitle != null || listItem.description != null) {
                binding.description.text = buildSpannedString {
                    listItem.subtitle?.let {
                        bold { append(it) }
                        append("    ")
                    }
                    listItem.description?.let {
                        append(it)
                    }
                }
                binding.description.isVisible = true
            } else {
                binding.description.isVisible = false
            }

            if (listItem.tags.isNotEmpty()) {
                // TODO: Allow multiple
                val tag = listItem.tags.first()
                binding.tag.isVisible = true
                val foregroundColor =
                    ColorUtils.mostContrastingColor(Color.WHITE, Color.BLACK, tag.color)
                when (tag.icon) {
                    is ResourceListIcon -> {
                        binding.tag.statusImage.isVisible = true
                        binding.tag.statusImage.setImageResource(tag.icon.id)
                        CustomUiUtils.setImageColor(binding.tag.statusImage, foregroundColor)
                    }
                    else -> {
                        binding.tag.statusImage.isVisible = false
                    }
                }
                binding.tag.setStatusText(tag.text)
                binding.tag.statusText.setTextColor(foregroundColor)
                binding.tag.setBackgroundTint(tag.color)
            } else {
                binding.tag.isVisible = false
            }

            binding.trailingText.isVisible = listItem.trailingText != null
            binding.trailingText.text = listItem.trailingText
            when (listItem.icon) {
                is ResourceListIcon -> {
                    binding.icon.isVisible = true
                    binding.icon.setImageResource(listItem.icon.id)
                    CustomUiUtils.setImageColor(binding.icon, listItem.icon.tint)
                }
                is AsyncBitmapListIcon -> {
                    binding.icon.isVisible = true
                    findViewTreeLifecycleOwner()?.let {
                        CustomUiUtils.setImageColor(binding.icon, null)
                        binding.icon.setImageBitmap(it, listItem.icon.provider)
                    }
                }
                else -> {
                    binding.icon.isVisible = false
                }
            }
            when (listItem.trailingIcon) {
                is ResourceListIcon -> {
                    binding.trailingIconBtn.isVisible = true
                    binding.trailingIconBtn.setImageResource(listItem.trailingIcon.id)
                    CustomUiUtils.setImageColor(binding.trailingIconBtn, listItem.trailingIcon.tint)
                    binding.trailingIconBtn.setOnClickListener { listItem.trailingIconAction() }
                }
                else -> {
                    binding.trailingIconBtn.isVisible = false
                }
            }
            if (listItem.menu.isNotEmpty()) {
                binding.menuBtn.isVisible = true
                binding.menuBtn.setOnClickListener {
                    Pickers.menu(it, listItem.menu.map { it.text }) { idx ->
                        listItem.menu[idx].action()
                        true
                    }
                }
            } else {
                binding.menuBtn.isVisible = false
            }

            binding.root.setOnClickListener { listItem.action() }
            binding.root.setOnLongClickListener {
                listItem.longClickAction()
                true
            }

            val dataViews = listOf(
                binding.data1,
                binding.data2,
                binding.data3
            )

            for (i in dataViews.indices) {
                // TODO: Allow more than 3 data points
                if (listItem.data.size > i) {
                    dataViews[i].isVisible = true
                    val data = listItem.data[i]
                    dataViews[i].text = data.text
                    when (data.icon) {
                        is ResourceListIcon -> {
                            dataViews[i].setCompoundDrawables(
                                Resources.dp(context, 12f).toInt(),
                                left = data.icon.id
                            )
                            CustomUiUtils.setImageColor(dataViews[i], data.icon.tint)
                        }
                        else -> {
                            dataViews[i].setCompoundDrawables(null, null, null, null)
                        }
                    }
                } else {
                    dataViews[i].isVisible = false
                }
            }

            binding.data.isVisible = listItem.data.isNotEmpty()

        }

    var emptyView: View? = null

    fun setItems(items: List<ListItem>) {
        // TODO: Be smart about how the list gets updated
        list.setData(items)
        emptyView?.isVisible = items.isEmpty()
    }

    fun <T> setItems(items: List<T>, mapper: ListItemMapper<T>) {
        setItems(items.map { mapper.map(it) })
    }

    fun scrollToPosition(position: Int, smooth: Boolean = true) {
        list.scrollToPosition(position, smooth)
    }

    init {
        list.addLineSeparator()
    }
}