package com.kylecorry.trail_sense.shared.lists

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ListItemPlainIconMenuBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils

class TSListView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {
    private val list =
        ListView(this, R.layout.list_item_plain_icon_menu) { view: View, listItem: ListItem ->
            val binding = ListItemPlainIconMenuBinding.bind(view)
            binding.title.text = listItem.title
            binding.centeredTitle.text = listItem.title
            binding.subtitle.text = listItem.subtitle
            binding.subtitle.isVisible = listItem.subtitle != null
            binding.description.text = listItem.description
            binding.description.isVisible = listItem.description != null
            binding.title.isVisible = listItem.description != null || listItem.subtitle != null
            binding.centeredTitle.isVisible =
                listItem.description == null && listItem.subtitle == null
            binding.trailingText.isVisible = listItem.trailingText != null
            binding.trailingText.text = listItem.trailingText
            when (listItem.icon) {
                is ResourceListIcon -> {
                    binding.icon.isVisible = true
                    binding.icon.setImageResource(listItem.icon.id)
                    CustomUiUtils.setImageColor(binding.icon, listItem.icon.tint)
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