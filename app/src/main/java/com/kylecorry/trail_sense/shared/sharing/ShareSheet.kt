package com.kylecorry.trail_sense.shared.sharing

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.databinding.FragmentShareSheetBinding
import com.kylecorry.trail_sense.shared.views.TileButton

class ShareSheet(
    private val title: String,
    private val actions: List<ShareAction>,
    private val onAction: (action: ShareAction?, sheet: ShareSheet) -> Unit
) : BoundBottomSheetDialogFragment<FragmentShareSheetBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.shareSheetTitle.text = title
        setAction(binding.shareSheetCopy, ShareAction.Copy)
        setAction(binding.shareSheetQr, ShareAction.QR)
        setAction(binding.shareSheetSend, ShareAction.Send)
        setAction(binding.shareSheetMaps, ShareAction.Maps)
        setAction(binding.shareSheetFile, ShareAction.File)
        setAction(binding.shareSheetNavigate, ShareAction.Navigate)
        setAction(binding.shareSheetBeacon, ShareAction.CreateBeacon)
        setAction(binding.shareSheetMeasure, ShareAction.Measure)
    }

    private fun setAction(button: TileButton, action: ShareAction) {
        button.setState(false)
        button.isVisible = actions.contains(action)
        button.setOnClickListener {
            onAction(action, this)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onAction(null, this)
    }


    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentShareSheetBinding {
        return FragmentShareSheetBinding.inflate(layoutInflater, container, false)
    }

}