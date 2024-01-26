package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SearchBarPreference
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides
import kotlinx.coroutines.Dispatchers

class GuideListFragment : PreferenceFragmentCompat() {

    private var guides = listOf<UserGuideCategory>()
    private var searchPref: SearchBarPreference? = null
    private var prefs = mutableListOf<Preference>()
    private val queue = CoroutineQueueRunner(dispatcher = Dispatchers.Main)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.user_guide, rootKey)
        guides = Guides.guides(requireContext())
        preferenceScreen.setShouldUseGeneratedIds(true)
        createSearchBar()
        updateList(guides)
    }

    private fun createSearchBar() {
        searchPref = SearchBarPreference(requireContext(), null)
        searchPref?.setOnSearchListener {
            val newGuides = mutableListOf<UserGuideCategory>()

            for (category in guides){
                if (category.name.contains(it, true)){
                    newGuides.add(category)
                } else {
                    val newCategory = UserGuideCategory(category.name, category.guides.filter { guide ->
                        guide.name.contains(it, true)
                    })
                    if (newCategory.guides.isNotEmpty()){
                        newGuides.add(newCategory)
                    }
                }
            }

            updateList(newGuides)
        }
        // Add a searchbar to the top
        searchPref?.let {
            preferenceScreen.addPreference(it)
        }
    }

    private fun updateList(guides: List<UserGuideCategory>) {
        inBackground {
            queue.enqueue {
                prefs.forEach {
                    preferenceScreen.removePreference(it)
                }
                prefs.clear()

                for (guideCategory in guides) {
                    val category = PreferenceCategory(requireContext())
                    category.title = guideCategory.name
                    category.isIconSpaceReserved = false
                    category.isSingleLineTitle = false
                    preferenceScreen.addPreference(category)
                    prefs.add(category)

                    for (guide in guideCategory.guides) {
                        val guidePref = Preference(requireContext())
                        guidePref.title = guide.name
                        if (guide.description != null) {
                            guidePref.summary = guide.description
                        }
                        guidePref.isSingleLineTitle = false
                        guidePref.isIconSpaceReserved = false
                        onClick(guidePref) {
                            tryOrNothing {
                                findNavController().navigate(
                                    R.id.action_guideListFragment_to_guideFragment, bundleOf(
                                        "guide_name" to guide.name,
                                        "guide_contents" to guide.contents
                                    )
                                )
                            }
                        }
                        category.addPreference(guidePref)
                        prefs.add(guidePref)
                    }
                }
            }
        }
    }


    private fun onClick(pref: Preference?, action: () -> Unit) {
        pref?.setOnPreferenceClickListener {
            action.invoke()
            true
        }
    }

}