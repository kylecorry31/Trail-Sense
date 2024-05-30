# Ocean Sense

> Use your Android phone's sensors to assist with boating. Designed for entirely offline use.

> Ocean Sense is a fork of [Trail Sense](https://github.com/kylecorry31/Trail-Sense) with some nautical features added (at present it adds nautical miles and knots as units). Please visit [https://github.com/kylecorry31/Trail-Sense](https://github.com/kylecorry31/Trail-Sense).

Ocean Sense is a tool, and just like any other tool, it's essential to have backup equipment and skills. Ocean Sense is *NOT FOR NAVIGATION*!

> Add image here

## Table of Contents

- [Feature Roadmap](#feature-roadmap)
- [Goals](#goals)
- [Features](#features)
- [Privacy](#privacy)
- [Contributing](#contributing)
- [Open Source Credits](#open-source-credits)
- [License](#license)

## Feature Roadmap (2024)
The feature roadmap is a tentative outline of the major features that are planned to be completed in the next year. Smaller features may end up being implemented in between.
- [X] Nautical miles and knots
- [X] Heading and COG arrows
- [X] MOB system
- [ ] More useful icons
- [ ] Seamarks
- [ ] Anchor alarm


## Goals
- Ocean Sense must not use the Internet in any way, the entire app is usable when there is no Internet connection
- Features must provide some benefits to people using the app while hiking, in a survival situation, etc.
- Features should make use of the sensors on a phone rather than relying on stored information such as guides
- Features must be based on peer-reviewed science or be verified against real world data
- [Use Cases](https://github.com/BloodWalrus/Ocean-Sense/wiki/Use-Cases)

## Features

- Nautical miles and knots
- Place beacons and navigate to them
- Follow paths
- Retrace your steps with backtrack
- Use a photo as a map
- Plan what to pack
- Be alerted before the sun sets
- Predict the weather
- Use your phone as a flashlight
- [And much more!](https://github.com/BloodWalrus/Ocean-Sense/wiki/Use-Cases)

<table>
  <tr>
    <td>
      <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png"/>
    </td>
    <td>
      <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png"/>
    </td>
    <td>
      <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png"/>
    </td>
  </tr>
  <tr>
    <td>
      <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.jpg"/>
    </td>
    <td>
      <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png"/>
    </td>
    <td>
      <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png"/>
    </td>
  </tr>
</table>

# Privacy

Location information gathered by this application does not leave your device (as a matter of fact, this app doesn't use the Internet at all). The altitude and pressure history for the last 48 hours is stored in local app storage - this is used to determine weather forecasts. The last known location is also stored in app preferences to allow faster load times and support app functionality when the GPS can not be reached. The beacons and paths store their location information in a local SQLite database. All of this information is cleared when you clear the app storage or delete it.

## Permissions
### Sensitive
- **POST_NOTIFICATIONS**
  - Allows Ocean Sense to display notifications (backtrack, weather, sunset alerts, astronomy events, water boil timer, etc)
  - **When denied**: Alerts will not be displayed and some services may not function properly depending on your device manufacturer.
- **ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION**
  - Allows Ocean Sense to retrieve your location for navigation, weather (sea level calibration), and astronomy. 
  - **When denied**: You will have the ability to mock your location under Settings > Sensors > GPS. On Android 14+, backtrack and weather will be unavailable due to Android OS constraints.
- **ACCESS_BACKGROUND_LOCATION**
  - Allows Ocean Sense to retrieve your location for sunset alerts while in the background. On some devices, this will also improve the reliability of backtrack and weather monitor (though shouldn't be needed on most devices).
  - **When denied**: If you travel and do not open Ocean Sense, but have Sunset Alerts enabled, the times will likely be inaccurate.
- **ACTIVITY_RECOGNITION**
  - Allows Ocean Sense to use your phone's pedometer for distance calculation.
  - **When denied**: The pedometer will not work.
- **CAMERA**
  - Allows Ocean Sense to use your camera on the sighting compass, clinometer, and for taking photos used by the Cloud Scanner, QR Code Scanner, and Photo Maps.
  - **When denied**: You will not be able to use the sighting compass, camera clinometer, or QR Code Scanner. You will need to pick an existing photo to use for the Cloud Scanner or Photo Maps.
- **SCHEDULE_EXACT_ALARM**
  - Allows Ocean Sense to post a notification at an exact time. This is used by the Clock tool (when updating system time) and Sunset Alerts.
  - **When denied**: The clock and sunset alerts may not be accurate (can be off by several minutes).
 
### Not sensitive (always granted)
- **RECEIVE_BOOT_COMPLETED**
  - Allows Ocean Sense to restart when you reboot your device. This will re-enable backtrack, weather monitor, and several other background services.
- **FOREGROUND_SERVICE**
  - Allows Ocean Sense to start foreground services, such as backtrack and weather monitor.
- **FLASHLIGHT**
  - Allows Ocean Sense to control the phone's flashlight.
- **VIBRATE**
  - Allows Ocean Sense to vibrate the phone. Used for haptic feedback on dials and on the metal detector tool.
- **WAKE_LOCK**
  - Allows Ocean Sense to reliably run services such as backtrack and weather monitor, especially when the frequency is under 15 minutes.


# Debug features
Only available on debug APKs / builds via Android Studio
- Weather tool's barometer chart shows unsmoothed readings in background
- Weather history, elevation history, path elevations, and latest cloud scan are logged to the files/debug folder in Ocean Sense data as CSV files
- Weather settings shows statistics timing (for weather monitor service)
- Paths show statistics about timing (for backtrack service)

# Contributing

See the [CONTRIBUTING.md](https://github.com/BloodWalrus/Ocean-Sense/blob/main/CONTRIBUTING.md) file for details on contributing to Ocean Sense.

# Open Source Credits

- This app is a fork of [Trail Sense](https://github.com/kylecorry31/Trail-Sense) with nautical features added.
- Thank you to everyone who tried out this app and opened issues, suggested features, provided translations, or tested debug builds for me
- Please see the in app licenses for all open source licenses
- Contributors and translators: https://github.com/BloodWalrus/Ocean-Sense/graphs/contributors
- [showdownjs](https://github.com/showdownjs/showdown): used for website markdown rendering [License](https://github.com/showdownjs/showdown/blob/master/LICENSE)

# License

[![License](https://img.shields.io/:license-mit-blue.svg?style=flat-square)](https://badges.mit-license.org)

- **[MIT license](LICENSE)**
