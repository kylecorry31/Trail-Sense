package com.kylecorry.trail_sense

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.trail_sense.services.BarometerService
import com.kylecorry.trail_sense.ui.*


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasPermissions()){
            getPermission()
        }

        BarometerService.start(this)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        syncFragmentWithSelection(bottomNavigation.selectedItemId)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            syncFragmentWithSelection(item.itemId)
            true
        }

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavigation.selectedItemId = savedInstanceState.getInt("page", R.id.action_navigation)
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
            R.id.action_altimeter -> {
                switchFragment(AltimeterFragment())
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
            supportFragmentManager.popBackStack()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (granted){
            // Do nothing yet
        } else {
            Toast.makeText(this, "Location permission is required, some features may be broken", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        for (permission in permissions){
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false
            }
        }

        return true
    }

    private fun getPermission(){
        val permissions = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        ActivityCompat.requestPermissions(this,
            permissions.toTypedArray(),
            1
        )
    }
    
}
