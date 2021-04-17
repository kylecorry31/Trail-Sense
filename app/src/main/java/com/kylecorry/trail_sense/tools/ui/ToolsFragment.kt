package com.kylecorry.trail_sense.tools.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolsBinding
import com.kylecorry.trail_sense.databinding.ListItemToolBinding
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import com.kylecorry.trailsensecore.infrastructure.view.ListView


class ToolsFragment : BoundFragment<FragmentToolsBinding>() {

    private lateinit var toolsList: ListView<ToolListItem>
    private val tools by lazy { Tools.getTools(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val primaryColor = UiUtils.color(requireContext(), R.color.colorPrimary)
        val textColor = UiUtils.androidTextColorPrimary(requireContext())
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = requireContext().obtainStyledAttributes(attrs)
        val selectableBackground = typedArray.getResourceId(0, 0)
        typedArray.recycle()
        toolsList = ListView(binding.toolRecycler, R.layout.list_item_tool) { view, tool ->
            val toolBinding = ListItemToolBinding.bind(view)

            if (tool.action != null && tool.icon != null) {
                // Tool
                toolBinding.root.setBackgroundResource(selectableBackground)
                toolBinding.title.text = tool.name
                toolBinding.title.setTextColor(textColor)
                toolBinding.description.text = tool.description
                toolBinding.icon.isVisible = true
                toolBinding.description.isVisible = tool.description != null
                toolBinding.icon.setImageResource(tool.icon)
                toolBinding.root.setOnClickListener {
                    findNavController().navigate(tool.action)
                }
            } else {
                // Tool group
                toolBinding.root.setBackgroundResource(0)
                toolBinding.title.text = tool.name
                toolBinding.title.setTextColor(primaryColor)
                toolBinding.description.text = ""
                toolBinding.icon.isVisible = false
                toolBinding.description.isVisible = false
                toolBinding.root.setOnClickListener(null)
            }

        }

        binding.searchbox.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                updateToolList()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                updateToolList()
                return true
            }

        })

        updateToolList()
    }

    private fun updateToolList() {
        val toolListItems = mutableListOf<ToolListItem>()
        val search = binding.searchbox.query

        if (search.isNullOrBlank()) {
            for (group in tools) {
                toolListItems.add(ToolListItem(group.name, null, null, null))
                for (tool in group.tools) {
                    toolListItems.add(
                        ToolListItem(
                            tool.name,
                            tool.description,
                            tool.icon,
                            tool.navAction
                        )
                    )
                }
            }
        } else {
            for (group in tools) {
                for (tool in group.tools) {
                    if (tool.name.contains(search, true) || tool.description?.contains(
                            search,
                            true
                        ) == true
                    ) {
                        toolListItems.add(
                            ToolListItem(
                                tool.name,
                                tool.description,
                                tool.icon,
                                tool.navAction
                            )
                        )
                    }
                }
            }
        }

        toolsList.setData(toolListItems)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolsBinding {
        return FragmentToolsBinding.inflate(layoutInflater, container, false)
    }

    internal data class ToolListItem(
        val name: String,
        val description: String?,
        @DrawableRes val icon: Int?,
        @IdRes val action: Int?
    )

}