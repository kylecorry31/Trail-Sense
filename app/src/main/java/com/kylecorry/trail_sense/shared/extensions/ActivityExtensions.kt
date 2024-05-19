package com.kylecorry.trail_sense.shared.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.kylecorry.trail_sense.R

fun AppCompatActivity.findNavController(): NavController {
    return (supportFragmentManager.findFragmentById(R.id.fragment_holder) as NavHostFragment).navController
}