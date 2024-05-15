package com.kylecorry.trail_sense.tools.comms.talk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.databinding.FragmentCommsPluginBinding

class FragmentToolLocalTalk : BoundFragment<FragmentCommsPluginBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        openApp(
            requireContext(),
            "com.kylecorry.trail_sense_comms",
            "b76f32bf-6a72-4992-a741-0c9bf19ebd11"
        )
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommsPluginBinding {
        return FragmentCommsPluginBinding.inflate(layoutInflater, container, false)
    }

    private fun openApp(context: Context, packageName: String, tool: String) {
        if (!Package.isPackageInstalled(context, packageName)) return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtras(bundleOf("tool" to tool))
        context.startActivity(intent)
    }
}