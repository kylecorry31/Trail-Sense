# Trail Sense

> Use your Android phone's sensors to assist with wilderness treks or survival situations. Designed for entirely offline use.

![](https://github.com/kylecorry31/Trail-Sense/workflows/Android%20CI/badge.svg)
![](https://hosted.weblate.org/widgets/trail-sense/-/trail-sense-android/svg-badge.svg)

<table>
    <tr>
        <th>F-Droid</th>
        <th>Google Play</th>
    </tr>
    <tr>
        <td>
            <a href="https://f-droid.org/en/packages/com.kylecorry.trail_sense">
                <img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="60" align="middle">
            </a>
        </td>
        <td>
            <a href="https://play.google.com/store/apps/details?id=com.kylecorry.trail_sense">
                <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="60" align="middle">
            </a>
        </td>
    </tr>
</table>

<img src="fastlane/metadata/android/en-US/images/featureGraphic.png">

## Table of Contents

- [Goals](#goals)
- [Features](#features)
- [Privacy](#privacy)
- [Contributing](#contributing)
- [FAQ](#faq)
- [Support](#support)
- [Open Source Credits](#open-source-credits)
- [License](#license)

## Goals
- Trail Sense must not use the Internet in any way, as I want the entire app usable when there is no Internet connection
- Features must provide some benefits to people using the app while hiking, in a survival situation, etc.
- Features should make use of the sensors on a phone rather than relying on stored information such as guides
- Features must be based on peer-reviewed science or be verified against real world data
- [Use Cases (WIP)](https://github.com/kylecorry31/Trail-Sense/wiki/Use-Cases)

## Features

- Compass navigation
- Flashlight and SOS
- Barometer forecasting
- Astronomy

See the need for a new feature? [Post it here!](https://github.com/kylecorry31/Trail-Sense/issues/59)

### Navigation

The compass can be used to determine the direction to North, and when combined with the GPS it can be used to navigate to predefined locations. The predefined locations, known as beacons, can be created while at a location and at any point you can use the compass to navigate back to where the beacon was placed. You can also use Backtrack to record waypoints and retrace your steps.

Example beacons: home, work, trailhead, campsite

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Navigation Screenshot" height="500"/>

### Weather

The barometer can be used to determine if the weather will change soon and if a storm is likely to occur. The barometric pressure history (last 48 hours) is displayed as a graph and an interpretation of the current reading is shown. If the pressure suddenly drops, a storm alert notification is sent. Note, this feature is only available for phones which have a barometer.

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" alt="Weather Screenshot" height="500"/>

### Astronomy

View the sun/moon rise and set times and see the current phase of the moon at your exact location.

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" alt="Astronomy Screenshot" height="500"/>

# Privacy

Location information gathered by this application does not leave your device (as a matter of fact, this app doesn't use the Internet at all). The altitude and pressure history for the last 48 hours is stored in local app storage - this is used to determine weather forecasts. The last known location is also stored in app preferences to allow faster load times and support app functionality when the GPS can not be reached. The beacons store their location in a local SQLite database. All of this information is cleared when you clear the app storage or delete it.

## Permissions

- Location (fine, background): Used for beacon navigation, True North, barometer altitude correction (in background), and sun/moon rise/set times

# Contributing

- [Request a new feature](https://github.com/kylecorry31/Trail-Sense/issues/59)
- [Submit an issue](https://github.com/kylecorry31/Trail-Sense/issues)
- [Translate Trail Sense on Weblate](https://hosted.weblate.org/projects/trail-sense/trail-sense-android)
- [Test out new features](https://github.com/kylecorry31/Trail-Sense/issues/74)

If you choose to write a new feature yourself, send me a message to verify that it is something that I will accept into Trail Sense before your write it (if not, you can always fork this repo and create your own version of Trail Sense!). I will conduct a code review on incoming pull requests to verify they align nicely with the rest of the code base and the feature works as intended.

Issues marked with the help-wanted label are open for community contribution at any time (just submit a PR to main and I will review it), or leave a comment on the story to say you are working on it / ask for more details. Please leave a comment on any other issue before you work on them because they might not have all the details, I may not want it implemented yet, or I may have to implement it myself - for fun :)

If an issue has a milestone and you would like to work on it, please leave a comment before working on it or creating a pull request. If you do not have the feature completed within 4 days of when I plan to release, I will implement it.

# FAQ

- **Trail Sense sends too many notifications**
  - All notifications from Trail Sense can be disabled/re-enabled under their respective settings
  - Android requires background processes which access your location to send a notification (ex. Backtrack, weather monitor, and sunset alert). On most supported Android versions, you can disable the "Updates" notification channel to hide these notifications. (https://www.howtogeek.com/715614/what-are-android-notification-channels/)
  - The processes which you may see notifications from frequently are Weather, Sunset Alert, Backtrack, and Pedometer. All of these processes can be disabled in Trail Sense settings.
  - Weather: Under "Weather" settings in Trail Sense, turn off "Monitor weather" or disable "Show weather notification" and hide the "Updates" notification channel (Android's settings)
  - Backtrack: Under "Navigation" settings in Trail Sense, turn off "Backtrack" or hide the "Updates" notification channel (Android's settings)
  - Pedometer: Under "Odometer" settings in Trail Sense, change the source to "GPS"
  - Sunset Alert: Under "Astronomy" settings in Trail Sense, disable Sunset Alerts
- **Will there be an iOS version?**
  - No - unfortunately I don't have a Mac or iPhone, and wouldn't be able to develop and test an iOS version (at least, there won't be an iOS version from me)
- **Can I request a new feature?**
  - Of course! See the [Contributing section](#contributing) for more details - I will consider every feature request, and it will be more likely that your feature gets included if you provide some rational behind how it could benefit TS users in wilderness treks or survival situations. Even if your feature idea is beneficial to only a small percent of users, I may still include it (even if it is in an experimental tab, or unlockable through settings)
- **When can I expect new releases?**
  - I will try to create a release every two to four weeks with new features or bug fixes. Some features may be feature flagged (via a hidden setting) or marked as experimental to deliver thouroughly tested features. Debug builds can be generated on request (just post in #74 that you are interested). I can't gaurantee this will always be the case, work/life may get in the way. There will not be separate releases for Weblate translations while there are other changes in progress to ensure only code which is ready gets deployed - translations will be included in the main releases. During times of good weather, I may release less often / respond slower to inquiries.
- **The pressure graph isn't populating or is very jagged, how can I fix it?**
  - You may need to mark Trail Sense as exempt from battery optimizations: [how to make TS exempt](https://dontkillmyapp.com/)
- **Pressure readings aren't appearing while I'm travelling or are inaccurate**
  - Trail Sense can't accurately determine the pressure when your altitude changes, therefore you must remain at the same altitude for some time for it to populate. You can also disable the "Ignore rapid altitude changes" and "Factor in rapid pressure changes" settings under the barometer settings. See the Trail Sense user guide (under Tools) for tips on calibrating your barometer.
- **The compass was working, but now it will not move and I haven't touched any settings. How can I fix it?**
  - Sometimes Android stops reporting compass data to apps, and you may need to reboot your device to fix the issue.
- **The compass isn't working or is inaccurate**
  - Under Trail Sense Settings > Sensors > Sensor details verify that the compass appears and you have both a Magnetometer and Gravity sensor. If your device does not have these sensors, there is nothing that can be done
  - Under Trail Sense Settings > Sensors > Compass, try adjusting the compass smoothing (lower) or enabling the legacy compass
  - You may also have luck restarting your phone
  - Finally, you can try recalibrating the compass by rotating it in all directions or in a figure-8 pattern
  - If you are using True North, ensure your location is accurate (GPS settings) or you enter the correct declination for your location
- **The compass is jittering**
  - You can apply smoothing to the compass by opening Trail Sense Settings > Sensors > Compass and adjusting the smoothing bar. For my device, I find the 22 is a good smoothing value.
- **GPS isn't working or sunrise/set times are inaccurate**
  - The sunrise/set features need to know your approximate location to display accurate times, please check the following to see if it resolves your issue:
  - Validate your location settings by opening Trail Sense Settings > GPS and ensure your location appears and is correct. If your device location is disabled but Trail Sense has location permission, then the location in Trail Sense may be stale.
  - If you have GPS disabled (or the location permission denied), you will need to manually configure your location under Trail Sense's GPS settings (it defaults to 0, 0 - on the astronomy page you will see an error banner for this condition).
  - If your GPS is enabled and the permission is granted, you may need to turn off the "require satellites" setting or wait for the 10 second timeout with a clear view of the sky so Trail Sense can cache a GPS reading. The cached value can be verified on the "sensor details" page - bottom of settings.
  - You can attempt to diagnose GPS/location issues by opening Trail Sense Settings > Sensor Details and viewing the GPS and GPS Cache settings
  - Trail Sense requires that location is enabled (either High Accuracy or Device Only) and the fine location permission is granted. You can check the status of these from Android's settings
- **How can I report a bug?**
  - Either create a new issue here or email me at trailsense@protonmail.com
- **The tide times are inaccurate**
  - The tide tool is experimental and is designed to mimic an analog tide clock. Tide clocks suffer the same inaccuracies as Trail Sense, so you can expect tide times to be off by up to 2 hours depending on the length of the lunar day. To ensure the best accuracy, calibrate the tide clock using a high tide on a day of a full or new moon, and do not correct the tide clock throughout the lunar month to remove inaccuracies - this will make it more inaccurate over time.
  - Currently Trail Sense can only predict tides in areas which experience twice daily high tides (semidiurnal), such as the Atlantic ocean.
  - I would love to be able to improve the accuracy of tides in the future, but it may not be feasible to do without a large datastore or Internet connection.
- **Where can I find the time of high tide to calibrate?**
  - You can use a website such as NOAA's Tides and Currents (United States) or similar site for your country.
  - You may also be able to find a printed tide book or tide tables (from a newspaper or ranger station) in your area.
- **I can't turn off the flashlight/white noise or they won't turn on**
  - Please verify your notification settings for Trail Sense by long pressing the Trail Sense app icon, then opening App Settings, then opening Notifications. Ensure that Trail Sense has notifications enabled and that the Flashlight and White Noise channels are active.
- **Flashlight notifications don't go away instantly when I turn it off**
  - Unfortunately, Android requires that notifications are present for certain services for at least 5 seconds, there is no way to get around this as it is a system "feature"
- **What other apps would be useful in a survival situation?**
  - [Offline Survival Manual](https://github.com/ligi/SurvivalManual)
    - A survival guide application. This app will teach you how to use resources around you to survive in almost every environment.
  - [OsmAnd~](https://github.com/osmandapp/OsmAnd)
    - An offline maps application. Prior to going on a hike, download the maps you need. This app is free via F-Droid.

# Support

<table>
    <tr>
        <th>PayPal</th>
        <th>Liberapay</th>
    </tr>
    <tr>
        <td id="Donate">
         <a href="https://www.paypal.me/kylecorry">
          <img alt="Donate using PayPal" src="https://raw.githubusercontent.com/stefan-niedermann/paypal-donate-button/master/paypal-donate-button.png" height="60" align="middle"/>
         </a>
        </td>
        <td>
            <a href="https://liberapay.com/kylecorry31/donate">
                <img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg" height="40" align="middle">
            </a>
        </td>
    </tr>
</table>

# Open Source Credits

- Icons: [Austin Andrews](https://materialdesignicons.com/contributor/Austin-Andrews) and [Michael Irigoyen](https://materialdesignicons.com/contributor/Michael-Irigoyen)
- Charts: [MpAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- Thank you to everyone who tried out this app and opened issues, suggested features, provided translations, or tested debug builds for me
- Thanks to @qwerty287 for implementing several features and bugfixes

# License

[![License](https://img.shields.io/:license-mit-blue.svg?style=flat-square)](https://badges.mit-license.org)

- **[MIT license](LICENSE)**
