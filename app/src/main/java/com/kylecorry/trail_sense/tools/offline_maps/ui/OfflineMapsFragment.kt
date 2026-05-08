package com.kylecorry.trail_sense.tools.offline_maps.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.commitNow
import com.google.android.material.tabs.TabLayout
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.OfflineMapListFragment
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapListFragment

class OfflineMapsFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_offline_maps) {

    private var selectedTab = 0
    private var photoMapsFragment: PhotoMapListFragment? = null
    private var mapsforgeFragment: OfflineMapListFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTab = savedInstanceState?.getInt(KEY_SELECTED_TAB) ?: 0
    }

    override fun update() {
        val tabs = useView<TabLayout>(R.id.tabs)
        val tabContent = useView<View>(R.id.tab_content)

        useEffect(tabs, tabContent) {
            tabs.clearOnTabSelectedListeners()

            if (tabs.tabCount != 2) {
                tabs.removeAllTabs()
                tabs.addTab(tabs.newTab().setText(R.string.photo_maps))
                tabs.addTab(tabs.newTab().setText(R.string.mapsforge))
            }

            tabs.getTabAt(selectedTab)?.select()

            tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    selectedTab = tab?.position ?: 0
                    showSelectedTab(tabContent.id)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    // Empty
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    // Empty
                }
            })

            showSelectedTab(tabContent.id)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            view?.findViewById<View>(R.id.tab_content)?.let {
                showSelectedTab(it.id)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, selectedTab)
    }

    private fun showSelectedTab(containerId: Int) {
        val fragment = when (selectedTab) {
            1 -> getMapsforgeFragment()
            else -> getPhotoMapsFragment()
        }

        childFragmentManager.commitNow {
            childFragmentManager.fragments
                .filter { it != fragment }
                .forEach { hide(it) }

            if (fragment.isAdded) {
                show(fragment)
            } else {
                add(containerId, fragment)
            }
        }
    }

    private fun getPhotoMapsFragment(): PhotoMapListFragment {
        return photoMapsFragment ?: PhotoMapListFragment().apply {
            val mapIntentUri = BundleCompat.getParcelable(
                this@OfflineMapsFragment.arguments ?: Bundle(),
                "map_intent_uri",
                Uri::class.java
            )
            if (mapIntentUri != null) {
                arguments = Bundle().apply {
                    putParcelable("map_intent_uri", mapIntentUri)
                }
            }
            photoMapsFragment = this
        }
    }

    private fun getMapsforgeFragment(): OfflineMapListFragment {
        return mapsforgeFragment ?: OfflineMapListFragment().also {
            mapsforgeFragment = it
        }
    }

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }
}
