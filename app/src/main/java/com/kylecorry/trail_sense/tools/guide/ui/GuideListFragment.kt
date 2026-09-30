package com.kylecorry.trail_sense.tools.guide.ui

import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides

class GuideListFragment : TrailSenseReactiveFragment(R.layout.fragment_guide_list) {

    override fun update() {
        // Views
        val searchView = useView<SearchView>(R.id.searchbox)
        val listContainerView = useView<FragmentContainerView>(R.id.guide_fragment)

        // State
        val context = useAndroidContext()
        val guides = useMemo(context) { Guides.guides(context) }
        val (query, setQuery) = useState("")
        val (listFragment, setListFragment) = useState<GuideListPreferenceFragment?>(null)

        val filteredGuides = useMemo(guides, query) {
            guides.mapNotNull { category ->
                if (category.name.contains(query, true)) {
                    category
                } else {
                    UserGuideCategory(
                        category.name,
                        category.guides.filter { it.name.contains(query, true) }
                    ).takeIf { it.guides.isNotEmpty() }
                }
            }
        }

        // Effects
        useEffect(searchView) {
            searchView.setOnSearchListener {
                setQuery(it)
            }
        }

        // A new fragment is used for each view since the old one is destroyed with its container
        useEffect(listContainerView) {
            val fragment = GuideListPreferenceFragment()
            childFragmentManager.commit {
                replace(listContainerView.id, fragment)
            }
            setListFragment(fragment)
        }

        useEffect(listFragment, filteredGuides) {
            listFragment?.updateList(filteredGuides)
        }
    }
}
