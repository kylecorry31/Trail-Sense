package com.kylecorry.trail_sense.tools.ruler.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.databinding.FragmentToolRulerBinding
import com.kylecorry.trail_sense.shared.FormatService

class RulerFragment : Fragment() {
    private var _binding: FragmentToolRulerBinding? = null
    private val binding get() = _binding!!

    private val formatService by lazy { FormatService(requireContext()) }

    private lateinit var ruler: Ruler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolRulerBinding.inflate(inflater, container, false)
        ruler = Ruler(binding.ruler)
        ruler.onTap = this::onRulerTap
        ruler.show()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        ruler.clearTap()
        binding.measurement.text = ""
    }

    private fun onRulerTap(centimeters: Float){
        binding.measurement.text = formatService.formatFractionalDistance(centimeters)
    }

}