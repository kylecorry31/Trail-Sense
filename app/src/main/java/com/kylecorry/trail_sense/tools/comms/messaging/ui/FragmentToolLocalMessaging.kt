package com.kylecorry.trail_sense.tools.comms.messaging.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.databinding.FragmentCommsPluginBinding

class FragmentToolLocalMessaging : BoundFragment<FragmentCommsPluginBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: Only do this once
        Package.openApp(requireContext(), "com.kylecorry.trail_sense_comms")
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommsPluginBinding {
        return FragmentCommsPluginBinding.inflate(layoutInflater, container, false)
    }
}