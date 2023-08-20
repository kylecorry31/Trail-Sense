# Migrating from the debug build to the dev build (GitHub)
1. Install the dev build from the latest release
2. Ensure you have adb installed and working correct
3. Connect your device to your computer
4. Execute either debug_to_dev.bat or debug_to_dev.sh depending on your OS
5. Open Trail Sense dev and verify that your data is intact before deleting the debug build

# What the script does
1. Force stops both the debug and dev apps
2. Copies the debug app's database, files, and shared preferences to the dev app's directory
3. Re-grants the same set of permissions to the dev app that the debug app had

# How to do it manually
1. Force stop both the debug and dev apps
2. From the Device File Explorer in Android Studio, save the debug app's database, files, and shared_prefs directories to your computer (located at `/data/data/com.kylecorry.trail_sense`)
3. Rename the shared_prefs/com.kylecorry.trail_sense_preferences.xml file to shared_prefs/com.kylecorry.trail_sense.dev_preferences.xml
4. From the Device File Explorer in Android Studio, upload the 3 directories from your computer into the dev app's directory (located at `/data/data/com.kylecorry.trail_sense.dev`)
5. Start the dev app and verify that your data is intact before deleting the debug build (you will need to regrant permissions manually)

# Issues
## ADB command not found
Ensure that ADB is added to your path. If you are on Windows, this can typically be found at C:\Users\<YOUR USERNAME>\AppData\Local\Android\Sdk\platform-tools if you have Android Studio installed.
Otherwise you can install the platform tools from https://developer.android.com/tools/releases/platform-tools