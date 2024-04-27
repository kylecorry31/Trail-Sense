package com.kylecorry.trail_sense.main

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.navigation.NavigationBarView
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.backup.BackupService
import com.kylecorry.trail_sense.databinding.ActivityMainBinding
import com.kylecorry.trail_sense.main.errors.ExceptionHandler
import com.kylecorry.trail_sense.onboarding.OnboardingActivity
import com.kylecorry.trail_sense.receivers.RestartServicesCommand
import com.kylecorry.trail_sense.settings.ui.SettingsMoveNotice
import com.kylecorry.trail_sense.shared.CustomUiUtils.isDarkThemeOn
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.ComposedCommand
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils.setupWithNavController
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.views.ErrorBannerView
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.battery.infrastructure.commands.PowerSavingModeAlertCommand
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MainActivity : AndromedaActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding!!

    private val navController: NavController
        get() = findNavController()

    val errorBanner: ErrorBannerView
        get() = binding.errorBanner

    private lateinit var userPrefs: UserPreferences
    private val cache by lazy { PreferencesSubsystem.getInstance(this).preferences }

    private val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        ExceptionHandler.initialize(this)

        userPrefs = UserPreferences(applicationContext)
        val mode = when (userPrefs.theme) {
            UserPreferences.Theme.Light -> ColorTheme.Light
            UserPreferences.Theme.Dark, UserPreferences.Theme.Black, UserPreferences.Theme.Night -> ColorTheme.Dark
            UserPreferences.Theme.System -> ColorTheme.System
            UserPreferences.Theme.SunriseSunset -> sunriseSunsetTheme()
        }
        val isBlackTheme =
            userPrefs.theme == UserPreferences.Theme.Black || userPrefs.theme == UserPreferences.Theme.Night
        setColorTheme(mode, userPrefs.useDynamicColors)
        enableEdgeToEdge(
            navigationBarStyle = if (isBlackTheme) {
                SystemBarStyle.dark(Color.BLACK)
            } else if (isDarkThemeOn()) {
                SystemBarStyle.dark(Resources.androidBackgroundColorSecondary(this))
            } else {
                SystemBarStyle.light(
                    Resources.androidBackgroundColorSecondary(this),
                    Color.BLACK
                )
            }
        )
        super.onCreate(savedInstanceState)

        Screen.setAllowScreenshots(window, !userPrefs.privacy.isScreenshotProtectionOn)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding?.root)

        // Handle black theme
        if (isBlackTheme) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            binding.bottomNavigation.setBackgroundColor(Color.BLACK)
        }

        if (userPrefs.theme == UserPreferences.Theme.Night) {
            binding.colorFilter.setColorFilter(
                PorterDuffColorFilter(
                    Color.RED,
                    PorterDuff.Mode.MULTIPLY
                )
            )
        }

        setBottomNavLabelsVisibility()
        binding.bottomNavigation.setupWithNavController(navController, false)

        bindLayoutInsets()

        if (cache.getBoolean(getString(R.string.pref_onboarding_completed)) != true) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        val previousPermissionStatus = permissions.map {
            Permissions.hasPermission(this, it)
        }
        requestPermissions(permissions) {
            val currentPermissionStatus = permissions.map {
                Permissions.hasPermission(this, it)
            }
            val permissionsChanged =
                previousPermissionStatus.zip(currentPermissionStatus).any { it.first != it.second }
            startApp(permissionsChanged)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    fun changeBottomNavLabelsVisibility(useCompactMode: Boolean) {
        userPrefs.useCompactMode = useCompactMode
        setBottomNavLabelsVisibility()
    }

    private fun setBottomNavLabelsVisibility() {
        binding.bottomNavigation.apply {
            if (userPrefs.useCompactMode) {
                layoutParams.height = Resources.dp(context, 55f).toInt()
                labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_UNLABELED
            } else {
                layoutParams.height = LayoutParams.WRAP_CONTENT
                labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_AUTO
            }
        }
    }

    private fun bindLayoutInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    fun reloadTheme() {
        cache.putBoolean("pref_theme_just_changed", true)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        FlashlightSubsystem.getInstance(this).startSystemMonitor()
        PedometerSubsystem.getInstance(this).recalculateState()
    }

    override fun onPause() {
        super.onPause()
        FlashlightSubsystem.getInstance(this).stopSystemMonitor()
    }

    private fun startApp(shouldReloadNavigation: Boolean) {
        if (cache.getBoolean("pref_theme_just_changed") == true) {
            cache.putBoolean("pref_theme_just_changed", false)
            recreate()
        }

        errorBanner.dismissAll()

        if (cache.getBoolean(BackupService.RECENTLY_BACKED_UP_KEY) == true) {
            cache.remove(BackupService.RECENTLY_BACKED_UP_KEY)
            navController.navigate(R.id.action_settings)
        } else if (navController.currentDestination?.id == R.id.action_navigation && shouldReloadNavigation) {
            navController.navigate(R.id.action_navigation)
        }

        ComposedCommand(
            ShowDisclaimerCommand(this),
            PowerSavingModeAlertCommand(this),
            RestartServicesCommand(this, false),
            SettingsMoveNotice(this)
        ).execute()

        if (!Sensors.hasBarometer(this)) {
            val item = binding.bottomNavigation.menu.findItem(R.id.action_weather)
            item?.isVisible = false
        }

        handleIntentAction(intent)
    }

    private fun handleIntentAction(intent: Intent) {
        val intentData = intent.data
        if (intent.scheme == "geo" && intentData != null) {
            val geo = GeoUri.from(intentData)
            binding.bottomNavigation.selectedItemId = R.id.action_navigation
            if (geo != null) {
                val bundle = bundleOf("initial_location" to geo)
                navController.navigate(
                    R.id.beacon_list,
                    bundle
                )
            }
        } else if ((intent.type?.startsWith("image/") == true || intent.type?.startsWith("application/pdf") == true)) {
            binding.bottomNavigation.selectedItemId = R.id.action_experimental_tools
            val intentUri = intent.clipData?.getItemAt(0)?.uri
            val bundle = bundleOf("map_intent_uri" to intentUri)
            navController.navigate(R.id.mapListFragment, bundle)
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
        binding.bottomNavigation.selectedItemId = savedInstanceState.getInt(
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
        outState.putInt("page", binding.bottomNavigation.selectedItemId)
        navController.currentBackStackEntry?.arguments?.let {
            outState.putBundle("navigation_arguments", it)
        }
        navController.currentDestination?.id?.let {
            outState.putInt("navigation", it)
        }
    }

    private fun sunriseSunsetTheme(): ColorTheme {
        val astronomyService = AstronomyService()
        val location = LocationSubsystem.getInstance(this).location
        if (location == Coordinate.zero) {
            return ColorTheme.System
        }
        val isSunUp = astronomyService.isSunUp(location)
        return if (isSunUp) {
            ColorTheme.Light
        } else {
            ColorTheme.Dark
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

    private fun getVolumeAction(): VolumeAction? {
        val navigationId = navController.currentDestination?.id
        val fragment = getFragment() as? AndromedaFragment ?: return null
        val tools = Tools.getTools(this)

        // Sort order = High priority active, open tool, normal priority active

        val highPriorityAction = tools.flatMap { tool ->
            tool.volumeActions.filter {
                it.priority == ToolVolumeActionPriority.High && it.isActive(
                    this,
                    tool.isOpen(navigationId ?: 0)
                )
            }
        }.firstOrNull()

        if (highPriorityAction != null) {
            return highPriorityAction.create(fragment)
        }

        val activeAction =
            tools.firstOrNull { it.isOpen(navigationId ?: 0) }?.volumeActions?.firstOrNull {
                it.isActive(this, true)
            }

        if (activeAction != null) {
            return activeAction.create(fragment)
        }

        val normalPriorityAction = tools.flatMap { tool ->
            tool.volumeActions.filter {
                it.priority == ToolVolumeActionPriority.Normal && it.isActive(
                    this,
                    tool.isOpen(navigationId ?: 0)
                )
            }
        }.firstOrNull()

        return normalPriorityAction?.create?.invoke(fragment)
    }

    private fun onVolumePressed(isVolumeUp: Boolean, isButtonPressed: Boolean): Boolean {
        val action = getVolumeAction()

        if (action != null) {
            return if (isButtonPressed) {
                action.onButtonPress()
            } else {
                action.onButtonRelease()
            }
        }

        return false
    }


    private fun findNavController(): NavController {
        return (supportFragmentManager.findFragmentById(R.id.fragment_holder) as NavHostFragment).navController
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getActivity(
                context,
                27383254,
                intent(context),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

}
