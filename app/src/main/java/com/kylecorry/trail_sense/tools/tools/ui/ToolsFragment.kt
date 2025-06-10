package com.kylecorry.trail_sense.tools.tools.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.list.GridView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolsBinding
import com.kylecorry.trail_sense.databinding.ListItemToolBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.quickactions.ToolsQuickActionBinder
import com.kylecorry.trail_sense.tools.tools.ui.items.ToolListItem
import com.kylecorry.trail_sense.tools.tools.ui.items.ToolListItemStyle
import com.kylecorry.trail_sense.tools.tools.ui.items.render.DelegateToolListItemRenderer
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort
import com.kylecorry.trail_sense.tools.tools.ui.sort.CategorizedTools
import com.kylecorry.trail_sense.tools.tools.ui.sort.ToolSortFactory
import com.kylecorry.trail_sense.tools.tools.ui.sort.ToolSortType

class ToolsFragment : BoundFragment<FragmentToolsBinding>() {

    private var tools: List<Tool> = emptyList()
    private val prefs by lazy { UserPreferences(requireContext()) }

    private val pinnedToolManager by lazy { PinnedToolManager(prefs) }

    private val toolSortFactory by lazy { ToolSortFactory(requireContext()) }

    private val pinnedSorter = AlphabeticalToolSort()

    private lateinit var toolListView: GridView<ToolListItem>

    private var toolListItems = listOf<GridView.SpannedItem<ToolListItem>>()
    private var pinnedListItems = listOf<GridView.SpannedItem<ToolListItem>>()
    private val listLock = Any()

    private val toolItemRenderer = DelegateToolListItemRenderer()

    private val toolHeader by lazy {
        getToolHeaderListItem(
            getString(R.string.tools),
            R.drawable.sort_ascending
        ) {
            changeToolSort()
        }
    }

    private val pinnedHeader by lazy {
        getToolHeaderListItem(
            getString(R.string.pinned),
            R.drawable.ic_edit
        ) {
            editPinnedTools()
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentToolsBinding {
        return FragmentToolsBinding.inflate(layoutInflater, container, false)
    }

    override fun onUpdate() {
        super.onUpdate()

        useEffect(resetOnResume) {
            tools = Tools.getTools(requireContext())

            toolListView = GridView(binding.tools, R.layout.list_item_tool, 2) { view, tool ->
                val binding = ListItemToolBinding.bind(view)
                toolItemRenderer.render(binding, tool)
            }

            updatePinnedTools()
            updateTools()

            updateQuickActions()

            binding.settingsBtn.setOnClickListener {
                findNavController().navigate(R.id.action_settings)
            }

            binding.searchbox.setOnSearchListener {
                updateTools()
            }

            CustomUiUtils.oneTimeToast(
                requireContext(),
                getString(R.string.tool_long_press_hint_toast),
                "tools_long_press_notice_shown",
                short = false
            )

        }
    }

    // TODO: Add a way to customize this
    private fun updateQuickActions() {
        ToolsQuickActionBinder(this, binding).bind()
    }

    private fun changeToolSort() {
        val sortTypes = ToolSortType.values()
        val sortTypeNames = mapOf(
            ToolSortType.Name to getString(R.string.name),
            ToolSortType.Category to getString(R.string.category)
        )

        Pickers.item(
            requireContext(),
            getString(R.string.sort),
            sortTypes.map { sortTypeNames[it] ?: "" },
            sortTypes.indexOf(prefs.toolSort)
        ) { selectedIdx ->
            if (selectedIdx != null) {
                prefs.toolSort = sortTypes[selectedIdx]
                updateTools()
            }
        }
    }

    private fun updateTools() {
        inBackground {
            onDefault {
                val filteredTools = filterTools(tools)
                val sorter = toolSortFactory.getToolSort(prefs.toolSort)
                toolListItems = listOf(toolHeader) + getToolItemList(sorter.sort(filteredTools))
            }

            updateList()
        }
    }

    private fun filterTools(tools: List<Tool>): List<Tool> {
        val filter = binding.searchbox.query
        return if (filter.isNullOrBlank()) {
            tools
        } else {
            tools.filter {
                it.name.contains(filter, true) || it.description?.contains(
                    filter,
                    true
                ) == true
            }
        }
    }

    private fun updateList() {
        val filter = binding.searchbox.query

        // Hide pinned when searching
        synchronized(listLock) {
            if (filter.isNullOrBlank()) {
                toolListView.setSpannedData(pinnedListItems + toolListItems)
            } else {
                toolListView.setSpannedData(toolListItems)
            }
        }
    }

    private fun getToolHeaderListItem(
        name: String,
        icon: Int,
        action: () -> Unit
    ): GridView.SpannedItem<ToolListItem> {
        return GridView.SpannedItem(
            ToolListItem(
                name,
                ToolListItemStyle.Header,
                icon,
                onClick = { action() }
            ), 2
        )
    }

    private fun getToolCategoryListItem(name: String?): GridView.SpannedItem<ToolListItem> {
        return GridView.SpannedItem(ToolListItem(name, ToolListItemStyle.Category), 2)
    }

    private fun getToolListItem(tool: Tool): GridView.SpannedItem<ToolListItem> {
        return GridView.SpannedItem(
            ToolListItem(
                tool.name,
                ToolListItemStyle.Tool,
                tool.icon,
                onClick = {
                    findNavController().navigateWithAnimation(tool.navAction)
                },
                onLongClick = {
                    Pickers.menu(
                        it, listOf(
                            if (tool.isExperimental) getString(R.string.experimental) else null,
                            if (tool.description != null) getString(R.string.pref_category_about) else null,
                            if (pinnedToolManager.isPinned(tool.id)) {
                                getString(R.string.unpin)
                            } else {
                                getString(R.string.pin)
                            },
                            if (tool.guideId != null) getString(R.string.tool_user_guide_title) else null,
                            if (tool.settingsNavAction != null) getString(R.string.settings) else null
                        )
                    ) { selectedIdx ->
                        when (selectedIdx) {
                            1 -> dialog(tool.name, tool.description, cancelText = null)
                            2 -> {
                                if (pinnedToolManager.isPinned(tool.id)) {
                                    pinnedToolManager.unpin(tool.id)
                                } else {
                                    pinnedToolManager.pin(tool.id)
                                }
                                updatePinnedTools()
                            }

                            3 -> {
                                UserGuideUtils.showGuide(this, tool.guideId!!)
                            }

                            4 -> {
                                findNavController().navigateWithAnimation(tool.settingsNavAction!!)
                            }
                        }
                        true
                    }
                    true
                }
            ), 1)
    }

    private fun updatePinnedTools() {
        inBackground {
            onDefault {
                val pinned = tools.filter {
                    pinnedToolManager.isPinned(it.id)
                }

                pinnedListItems = listOf(pinnedHeader) + getToolItemList(pinnedSorter.sort(pinned))
            }

            updateList()
        }
    }

    private fun getToolItemList(tools: List<CategorizedTools>): List<GridView.SpannedItem<ToolListItem>> {
        return if (tools.size == 1) {
            tools.first().tools.map { tool ->
                getToolListItem(tool)
            }
        } else {
            tools.flatMap {
                listOf(getToolCategoryListItem(it.categoryName)) +
                        it.tools.map { tool ->
                            getToolListItem(tool)
                        }
            }
        }
    }

    private fun editPinnedTools() {
        // Sort alphabetically, but if the tool is already pinned, put it first
        val sorted = tools.sortedBy { tool ->
            if (pinnedToolManager.isPinned(tool.id)) {
                "0${tool.name}"
            } else {
                tool.name
            }
        }
        val toolNames = sorted.map { it.name }
        val defaultSelected = sorted.mapIndexedNotNull { index, tool ->
            if (pinnedToolManager.isPinned(tool.id)) {
                index
            } else {
                null
            }
        }

        Pickers.items(
            requireContext(), getString(R.string.pinned), toolNames, defaultSelected
        ) { selected ->
            if (selected != null) {
                pinnedToolManager.setPinnedToolIds(selected.map { sorted[it].id })
            }

            updatePinnedTools()
        }
    }

}