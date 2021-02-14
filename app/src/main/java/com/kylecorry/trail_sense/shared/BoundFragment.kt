package com.kylecorry.trail_sense.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BoundFragment<T: ViewBinding>: Fragment() {

    abstract fun generateBinding(layoutInflater: LayoutInflater, container: ViewGroup?): T

    protected val binding: T
        get() = _binding!!

    private var _binding: T? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = generateBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}