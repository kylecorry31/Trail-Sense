package com.kylecorry.trail_sense.diagnostics

enum class DiagnosticCode(val severity: Severity) {
    // Overrides
    AltitudeOverridden(Severity.Warning),
    LocationOverridden(Severity.Warning),
    LocationUnset(Severity.Error),

    // Power
    PowerSavingMode(Severity.Warning),
    BatteryHealthPoor(Severity.Error),
    BatteryUsageRestricted(Severity.Warning),

    // Unavailable
    CameraUnavailable(Severity.Warning),
    BarometerUnavailable(Severity.Warning),
    MagnetometerUnavailable(Severity.Error),
    AccelerometerUnavailable(Severity.Error),
    GPSUnavailable(Severity.Error),
    FlashlightUnavailable(Severity.Warning),
    PedometerUnavailable(Severity.Warning),

    // Permissions
    CameraNoPermission(Severity.Warning),
    LocationNoPermission(Severity.Warning),
    BackgroundLocationNoPermission(Severity.Warning),
    PedometerNoPermission(Severity.Warning),

    // Sensor quality
    BarometerPoor(Severity.Warning),
    MagnetometerPoor(Severity.Warning),
    AccelerometerPoor(Severity.Warning),
    GPSPoor(Severity.Warning),
    GPSTimedOut(Severity.Error),

    // Notifications
    SunsetAlertsBlocked(Severity.Warning),
    StormAlertsBlocked(Severity.Warning),
    DailyForecastNotificationsBlocked(Severity.Warning),
    FlashlightNotificationsBlocked(Severity.Warning),
    PedometerNotificationsBlocked(Severity.Warning),
    WeatherNotificationsBlocked(Severity.Warning),
}