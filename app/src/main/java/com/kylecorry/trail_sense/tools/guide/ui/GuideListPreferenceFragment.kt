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
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import kotlinx.coroutines.Dispatchers

class GuideListPreferenceFragment : PreferenceFragmentCompat() {

    private val queue = CoroutineQueueRunner(dispatcher = Dispatchers.Main)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.user_guide, rootKey)
        preferenceScreen.setShouldUseGeneratedIds(true)
    }

    fun updateList(guides: List<UserGuideCategory>) {
        inBackground {
            queue.enqueue {
                preferenceScreen.removeAll()

                for (guideCategory in guides) {
                    val category = PreferenceCategory(requireContext())
                    category.title = guideCategory.name
                    category.isIconSpaceReserved = false
                    category.isSingleLineTitle = false
                    preferenceScreen.addPreference(category)

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