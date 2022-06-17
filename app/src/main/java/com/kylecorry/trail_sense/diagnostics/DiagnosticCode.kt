package com.kylecorry.trail_sense.diagnostics

import com.kylecorry.trail_sense.shared.database.Identifiable

// Max ID = 31
enum class DiagnosticCode(override val id: Long, val severity: Severity): Identifiable {
    // Overrides
    AltitudeOverridden(1, Severity.Warning),
    LocationOverridden(2, Severity.Warning),
    LocationUnset(3, Severity.Error),

    // Power
    PowerSavingMode(4, Severity.Warning),
    BatteryHealthPoor(5, Severity.Error),
    BatteryUsageRestricted(6, Severity.Error),

    // Unavailable
    CameraUnavailable(7, Severity.Warning),
    BarometerUnavailable(8, Severity.Warning),
    MagnetometerUnavailable(9, Severity.Error),
    LightSensorUnavailable(10, Severity.Error),
    AccelerometerUnavailable(11, Severity.Error),
    GPSUnavailable(12, Severity.Error),
    FlashlightUnavailable(13, Severity.Warning),
    PedometerUnavailable(14, Severity.Warning),

    // Permissions
    CameraNoPermission(15, Severity.Warning),
    LocationNoPermission(16, Severity.Warning),
    BackgroundLocationNoPermission(17, Severity.Warning),
    PedometerNoPermission(18, Severity.Warning),
    ExactAlarmNoPermission(31, Severity.Warning),

    // Sensor quality
    BarometerPoor(19, Severity.Warning),
    MagnetometerPoor(20, Severity.Warning),
    AccelerometerPoor(21, Severity.Warning),
    GPSPoor(22, Severity.Warning),
    GPSTimedOut(23, Severity.Error),

    // Notifications
    SunsetAlertsBlocked(24, Severity.Warning),
    StormAlertsBlocked(25, Severity.Warning),
    DailyForecastNotificationsBlocked(26, Severity.Warning),
    FlashlightNotificationsBlocked(27, Severity.Warning),
    PedometerNotificationsBlocked(28, Severity.Warning),
    WeatherNotificationsBlocked(29, Severity.Warning),

    // Services
    WeatherMonitorDisabled(30, Severity.Warning)
}