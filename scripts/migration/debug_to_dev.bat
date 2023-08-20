@echo off
setlocal enabledelayedexpansion

set APP_PACKAGE=com.kylecorry.trail_sense
set DEST_APP_PACKAGE=com.kylecorry.trail_sense.dev

:: Force stop the source and destination apps
adb shell am force-stop %APP_PACKAGE%
adb shell am force-stop %DEST_APP_PACKAGE%

:: Step 1: Copy over app data

:: Loop through the directories, get the list of files, and push them to the destination app
set DIRS=(databases files shared_prefs)
for %%d in %DIRS% do (
    for /f "tokens=*" %%f in ('adb shell run-as %APP_PACKAGE% find /data/data/%APP_PACKAGE%/%%d -type f') do (
        :: Determine the relative path for the destination
        set REL_PATH=%%f
        set REL_PATH=!REL_PATH:/data/data/%APP_PACKAGE%/=!
        set DEST_PATH=!REL_PATH!

        :: Echo out which file is being copied
        echo Copying %%f to !DEST_PATH!

        :: Directly save the file content to a temporary location on the device
        adb shell "run-as %APP_PACKAGE% cat %%f > /data/local/tmp/temp_file"

        :: Create the destination directory if it doesn't exist (remove the file name from the path)
        adb shell run-as %DEST_APP_PACKAGE% mkdir -p /data/data/%DEST_APP_PACKAGE%/!DEST_PATH:%%~nxf=!

        :: Move the file from the temporary location to the desired destination
        adb shell run-as %DEST_APP_PACKAGE% cp /data/local/tmp/temp_file /data/data/%DEST_APP_PACKAGE%/!DEST_PATH!

        :: If the file is %APP_PACKAGE%_preferences.xml, rename it to %DEST_APP_PACKAGE%_preferences.xml
        if "!REL_PATH!" == "shared_prefs/%APP_PACKAGE%_preferences.xml" (
            adb shell run-as %DEST_APP_PACKAGE% mv /data/data/%DEST_APP_PACKAGE%/!DEST_PATH! /data/data/%DEST_APP_PACKAGE%/shared_prefs/%DEST_APP_PACKAGE%_preferences.xml
        )

        :: Delete the temp_file on the device
        adb shell rm /data/local/tmp/temp_file
    )
)

:: Step 2: Copy over already granted permissions
:: Get all permissions for the source package and filter the granted ones
adb shell pm dump %APP_PACKAGE% | findstr /c:"permission" | findstr /c:"granted=true" > temp_permissions.txt

for /F "tokens=1 delims=:" %%a in (temp_permissions.txt) do (
    adb shell pm grant %DEST_APP_PACKAGE% %%a 2> nul
)

echo Re-granted permissions to %DEST_APP_PACKAGE%
del temp_permissions.txt

endlocal
