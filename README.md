# Trail Sense

![](https://github.com/kylecorry31/Trail-Sense/workflows/Android%20CI/badge.svg)

<table>
    <tr>
        <th>F-Droid</th>
        <th>Google Play</th>
        <th>Donate</th>
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
        <td id="Donate">
         <a href="https://www.paypal.me/kylecorry">
          <img alt="Donate using PayPal" src="https://raw.githubusercontent.com/stefan-niedermann/paypal-donate-button/master/paypal-donate-button.png" height="60" align="middle"/>
         </a>
        </td>
    </tr>
</table>

 An Android app which provides useful information about the environment and can be used offline (designed for hiking / mountaineering).

## Features
* Compass
* Barometer
* Astronomy

## Navigation
The compass can be used to determine the direction to North, and when combined with the GPS it can be used to navigate to predefined locations. The predefined locations, known as beacons, can be created while at a location and at any point you can use the compass to navigate back to where the beacon was placed.

Example beacons: home, work, trailhead, campsite

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Navigation Screenshot" height="500"/>


## Weather
The barometer can be used to determine if the weather will change soon and if a storm is likely to occur. The barometric pressure history (last 48 hours) is displayed as a graph and an interpretation of the current reading is shown. If the pressure suddenly drops, a storm alert notification is sent.

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="Weather Screenshot" height="500"/>

## Astronomy
View the sun/moon rise and set times and see the current phase of the moon at your exact location.

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="Astronomy Screenshot" height="500"/>

# Privacy
Location information gathered by this application does not leave your device (as a matter of fact, this app doesn't use the Internet at all). The altitude and pressure history for the last 48 hours is stored in local app storage - this is used to determine weather forecasts. The last known location is also stored in app preferences to allow faster load times and support app functionality when the GPS can not be reached. The beacons store their location in a local SQLite database. All of this information is cleared when you clear the app storage or delete it.

## Permissions
- Location (fine, background): Used for beacon navigation, True North, barometer altitude correction (in background), and sun/moon rise/set times 

# Credits
- Weather icons: [Austin Andrews](https://materialdesignicons.com/contributor/Austin-Andrews)
- Moon icons: [Michael Irigoyen](https://materialdesignicons.com/contributor/Michael-Irigoyen)
- Charts: [MpAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- Thank you to everyone who tried out this app and opened issues, suggested features, or even tested debug builds for me

