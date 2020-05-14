package com.kylecorry.trail_sense

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.trail_sense.astronomy.ui.AstronomyFragment
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.shared.doTransaction
import com.kylecorry.trail_sense.shared.sensors.SensorChecker
import com.kylecorry.trail_sense.weather.infrastructure.BarometerService
import com.kylecorry.trail_sense.weather.ui.BarometerFragment


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val theme = prefs.getString(getString(R.string.pref_theme), "system") ?: "system"

        val mode = when (theme){
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        setContentView(R.layout.activity_main)
        bottomNavigation = findViewById(R.id.bottom_navigation)


        if (!prefs.getBoolean(getString(R.string.pref_onboarding_completed), false)){
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasPermissions()){
            getPermission()
        } else {
            startApp()
        }
    }

    private fun startApp(){
        val sensorChecker = SensorChecker(this)
        if(!sensorChecker.hasBarometer()) {
            val item: MenuItem = bottomNavigation.menu.findItem(R.id.action_weather)
            item.isVisible = false
        } else {
            BarometerService.start(this)
        }

        syncFragmentWithSelection(bottomNavigation.selectedItemId)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            syncFragmentWithSelection(item.itemId)
            true
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavigation.selectedItemId = savedInstanceState.getInt("page",
            R.id.action_navigation
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", bottomNavigation.selectedItemId)
    }

    private fun syncFragmentWithSelection(selection: Int){
        when (selection) {
            R.id.action_navigation -> {
                switchFragment(NavigatorFragment())
            }
            R.id.action_weather -> {
                switchFragment(BarometerFragment())
            }
            R.id.action_astronomy -> {
                switchFragment(AstronomyFragment())
            }
            R.id.action_settings -> {
                switchFragment(SettingsFragment())
            }
        }
    }

    private fun switchFragment(fragment: Fragment){
        supportFragmentManager.doTransaction {
            this.replace(R.id.fragment_holder, fragment)
        }
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount

        if (count == 0) {
            super.onBackPressed()
            //additional code
        } else {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (!granted){
            Toast.makeText(this, "Not all permissions granted, some features may be broken", Toast.LENGTH_LONG).show()
        } else {
            startApp()
        }
    }

    private fun hasPermissions(): Boolean {
        for (permission in permissions){
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false
            }
        }

        return true
    }

    private fun getPermission(){
        ActivityCompat.requestPermissions(this,
            permissions.toTypedArray(),
            1
        )
    }
    
}
