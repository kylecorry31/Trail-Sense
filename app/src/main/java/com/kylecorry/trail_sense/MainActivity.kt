package com.kylecorry.trail_sense

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.shared.DisclaimerMessage
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.*
import io.noties.markwon.Markwon
import java.time.Duration
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigation: BottomNavigationView

    private var geoIntentLocation: GeoUriParser.NamedCoordinate? = null

    private val sensorChecker by lazy { SensorChecker(this) }

    private lateinit var userPrefs: UserPreferences
    private lateinit var disclaimer: DisclaimerMessage
    private val cache by lazy { Cache(this) }

    private val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "android.permission.FLASHLIGHT"
    )

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        ExceptionUtils.onUncaughtException(Duration.ofMinutes(1)){
            UiUtils.alertWithCancel(
                this@MainActivity,
                getString(R.string.error_occurred),
                getString(R.string.error_occurred_message),
                getString(R.string.pref_email_title),
                getString(R.string.dialog_cancel)
            ) { cancelled ->
                if (cancelled) {
                    exitProcess(2)
                } else {
                    ExceptionUtils.report(
                        this@MainActivity,
                        it,
                        getString(R.string.email),
                        getString(R.string.app_name)
                    )
                }
            }
        }

        userPrefs = UserPreferences(this)
        val mode = when (userPrefs.theme) {
            UserPreferences.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            UserPreferences.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            UserPreferences.Theme.Black -> AppCompatDelegate.MODE_NIGHT_YES
            UserPreferences.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)

        disclaimer = DisclaimerMessage(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        setContentView(R.layout.activity_main)
        navController =
            (supportFragmentManager.findFragmentById(R.id.fragment_holder) as NavHostFragment).navController
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setupWithNavController(navController)

        if (userPrefs.theme == UserPreferences.Theme.Black) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            bottomNavigation.setBackgroundColor(Color.BLACK)
        }


        if (!prefs.getBoolean(getString(R.string.pref_onboarding_completed), false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        requestPermissions(permissions)
    }

    private fun startApp() {
        if (disclaimer.shouldShow()) {
            disclaimer.show()
        }

        if (userPrefs.weather.shouldMonitorWeather) {
            WeatherUpdateScheduler.start(this)
        } else {
            WeatherUpdateScheduler.stop(this)
        }

        if (!sensorChecker.hasBarometer()){
            val item: MenuItem = bottomNavigation.menu.findItem(R.id.action_weather)
            item.isVisible = false
        }

        if (userPrefs.backtrackEnabled){
            BacktrackScheduler.start(this)
        } else {
            BacktrackScheduler.stop(this)
        }

        val sunsetIntent = SunsetAlarmReceiver.intent(applicationContext)
        sendBroadcast(sunsetIntent)


        val intentData = intent.data
        if (intent.scheme == "geo" && intentData != null) {
            val namedCoordinate = GeoUriParser().parse(intentData)
            geoIntentLocation = namedCoordinate
            bottomNavigation.selectedItemId = R.id.action_navigation
            if (namedCoordinate != null) {
                val bundle =  bundleOf("initial_location" to MyNamedCoordinate.from(namedCoordinate))
                navController.navigate(
                    R.id.place_beacon,
                    bundle
                )
            }
        }

        if (intent.hasExtra(getString(R.string.extra_action))) {
            val desiredAction =
                intent.getIntExtra(getString(R.string.extra_action), R.id.action_navigation)
            bottomNavigation.selectedItemId = desiredAction
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) {
            return
        }

        setIntent(intent)

        if (intent.hasExtra(getString(R.string.extra_action))) {
            val desiredAction =
                intent.getIntExtra(getString(R.string.extra_action), R.id.action_navigation)
            bottomNavigation.selectedItemId = desiredAction
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavigation.selectedItemId = savedInstanceState.getInt(
            "page",
            R.id.action_navigation
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", bottomNavigation.selectedItemId)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1 && shouldRequestBackgroundLocation()) {
            requestBackgroundLocation()
        } else {
            startApp()
        }
    }

    private fun hasBackgroundLocation(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || PermissionUtils.hasPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    private fun shouldRequestBackgroundLocation(): Boolean {
        return PermissionUtils.isLocationEnabled(this) &&
                !hasBackgroundLocation() &&
                cache.getBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocation() {
        cache.putBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION, true)

        val markwon = Markwon.create(this)
        val contents = markwon.toMarkdown(getString(R.string.access_background_location_rationale))

        PermissionUtils.requestPermissionsWithRationale(
            this,
            listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PermissionRationale(
                getString(R.string.access_background_location),
                contents
            ),
            2,
            getString(R.string.dialog_grant),
            getString(R.string.dialog_deny)
        )
    }

    private fun requestPermissions(permissions: List<String>, requestCode: Int = 1) {
        PermissionUtils.requestPermissions(this, permissions, requestCode)
    }

    companion object {

        fun weatherIntent(context: Context): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(context.getString(R.string.extra_action), R.id.action_weather)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            return intent
        }

        fun astronomyIntent(context: Context): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(context.getString(R.string.extra_action), R.id.action_astronomy)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            return intent
        }
    }

}
