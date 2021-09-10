package com.kylecorry.trail_sense

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.system.Exceptions
import com.kylecorry.andromeda.core.system.GeoUriParser
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.onboarding.OnboardingActivity
import com.kylecorry.trail_sense.receivers.TrailSenseServiceUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.ExceptionUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.ErrorBannerView
import com.kylecorry.trail_sense.volumeactions.FlashlightToggleVolumeAction
import com.kylecorry.trail_sense.volumeactions.VolumeAction
import java.time.Duration
import kotlin.system.exitProcess


class MainActivity : AndromedaActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigation: BottomNavigationView
    val errorBanner: ErrorBannerView by lazy { findViewById(R.id.error_banner) }

    private lateinit var userPrefs: UserPreferences
    private val cache by lazy { Preferences(this) }

    private val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)

    init {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Exceptions.onUncaughtException(Duration.ofMinutes(1)) {
            it.printStackTrace()
            Alerts.dialog(
                this@MainActivity,
                getString(R.string.error_occurred),
                getString(R.string.error_occurred_message),
                okText = getString(R.string.pref_email_title)
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
            UserPreferences.Theme.SunriseSunset -> sunriseSunsetTheme()
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)

        Screen.setAllowScreenshots(window, !userPrefs.privacy.isScreenshotProtectionOn)

        val cache = Preferences(this)

        setContentView(R.layout.activity_main)
        navController =
            (supportFragmentManager.findFragmentById(R.id.fragment_holder) as NavHostFragment).navController
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setupWithNavController(navController)

        if (userPrefs.theme == UserPreferences.Theme.Black) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            bottomNavigation.setBackgroundColor(Color.BLACK)
        }

        Package.setComponentEnabled(
            this,
            "com.kylecorry.trail_sense.AliasMainActivity",
            userPrefs.navigation.areMapsEnabled
        )

        if (cache.getBoolean(getString(R.string.pref_onboarding_completed)) != true) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        requestPermissions(permissions){
            if (shouldRequestBackgroundLocation()) {
                requestBackgroundLocation {
                    startApp()
                }
            } else {
                startApp()
            }
        }
    }

    private fun startApp() {
        errorBanner.dismissAll()
        if (navController.currentDestination?.id == R.id.action_navigation) {
            navController.navigate(R.id.action_navigation)
        }

        CustomUiUtils.disclaimer(
            this,
            getString(R.string.app_disclaimer_message_title),
            getString(R.string.disclaimer_message_content),
            getString(R.string.pref_main_disclaimer_shown_key),
            considerShownIfCancelled = true,
            shownValue = false
        )

        if (userPrefs.isLowPowerModeOn) {
            Alerts.toast(this, getString(R.string.low_power_mode_on_message))
        }

        TrailSenseServiceUtils.restartServices(this)

        if (!Sensors.hasBarometer(this)) {
            val item: MenuItem = bottomNavigation.menu.findItem(R.id.action_weather)
            item.isVisible = false
        }

        handleIntentAction(intent)
    }

    private fun handleIntentAction(intent: Intent) {
        val intentData = intent.data
        if (intent.scheme == "geo" && intentData != null) {
            val namedCoordinate = GeoUriParser.parse(intentData)
            bottomNavigation.selectedItemId = R.id.action_navigation
            if (namedCoordinate != null) {
                val bundle = bundleOf("initial_location" to MyNamedCoordinate.from(namedCoordinate))
                navController.navigate(
                    R.id.beacon_list,
                    bundle
                )
            }
        } else if ((intent.type?.startsWith("image/") == true || intent.type?.startsWith("application/pdf") == true) && userPrefs.navigation.areMapsEnabled) {
            bottomNavigation.selectedItemId = R.id.action_experimental_tools
            val intentUri = intent.clipData?.getItemAt(0)?.uri
            val bundle = bundleOf("map_intent_uri" to intentUri)
            navController.navigate(R.id.action_tools_to_maps_list, bundle)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return
        setIntent(intent)
        handleIntentAction(intent)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavigation.selectedItemId = savedInstanceState.getInt(
            "page",
            R.id.action_navigation
        )
        if (savedInstanceState.containsKey("navigation")) {
            tryOrNothing {
                val bundle = savedInstanceState.getBundle("navigation_arguments")
                navController.navigate(savedInstanceState.getInt("navigation"), bundle)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", bottomNavigation.selectedItemId)
        navController.currentBackStackEntry?.arguments?.let {
            outState.putBundle("navigation_arguments", it)
        }
        navController.currentDestination?.id?.let {
            outState.putInt("navigation", it)
        }
    }

    private fun hasBackgroundLocation(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Permissions.hasPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    private fun shouldRequestBackgroundLocation(): Boolean {
        return Permissions.canGetFineLocation(this) &&
                !hasBackgroundLocation() &&
                cache.getBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocation(action: () -> Unit) {
        cache.putBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION, true)

        val markdown = MarkdownService(this)
        val contents = markdown.toMarkdown(getString(R.string.access_background_location_rationale))

        dialog(
            getString(R.string.access_background_location),
            contents,
            okText = getString(R.string.dialog_grant),
            cancelText = getString(R.string.dialog_deny),
            allowLinks = true
        ) { cancelled ->
            if (!cancelled) {
                requestPermissions(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                    action()
                }
            } else {
                action()
            }
        }
    }

    private fun sunriseSunsetTheme(): Int {
        val astronomyService = AstronomyService()
        val sensorService by lazy { SensorService(applicationContext) }
        val gps by lazy { sensorService.getGPS() }
        if (gps.location == Coordinate.zero) {
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        val isSunUp = astronomyService.isSunUp(gps.location)
        return if (isSunUp) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return onVolumePressed(isVolumeUp = false, isButtonPressed = true)
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return onVolumePressed(isVolumeUp = true, isButtonPressed = true)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return onVolumePressed(isVolumeUp = false, isButtonPressed = false)
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return onVolumePressed(isVolumeUp = true, isButtonPressed = false)
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun onVolumePressed(isVolumeUp: Boolean, isButtonPressed: Boolean): Boolean {
        if (!shouldOverrideVolumePress()) {
            return false
        }

        val action =
            (if (isVolumeUp) getVolumeUpAction() else getVolumeDownAction()) ?: return false

        if (isButtonPressed) {
            action.onButtonDown()
        } else {
            action.onButtonUp()
        }

        return true
    }

    private fun shouldOverrideVolumePress(): Boolean {
        val excluded = listOf(R.id.toolWhistleFragment, R.id.fragmentToolWhiteNoise)
        if (excluded.contains(navController.currentDestination?.id)) {
            return false
        }
        return true
    }


    private fun getVolumeDownAction(): VolumeAction? {
        if (userPrefs.flashlight.toggleWithVolumeButtons) {
            return FlashlightToggleVolumeAction(this)
        }

        return null
    }

    private fun getVolumeUpAction(): VolumeAction? {
        if (userPrefs.flashlight.toggleWithVolumeButtons) {
            return FlashlightToggleVolumeAction(this)
        }

        return null
    }


}
