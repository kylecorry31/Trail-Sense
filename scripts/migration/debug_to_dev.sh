#!/bin/bash

APP_PACKAGE="com.kylecorry.trail_sense"
DEST_APP_PACKAGE="com.kylecorry.trail_sense.dev"

# Force stop the source and destination apps
adb shell am force-stop $APP_PACKAGE
adb shell am force-stop $DEST_APP_PACKAGE

# Step 1: Copy over app data

# Loop through the directories, get the list of files, and push them to the destination app
DIRS=("databases" "files" "shared_prefs")
for d in "${DIRS[@]}"; do
    # Fetch the list of files and write to a temporary file
    adb shell "run-as $APP_PACKAGE find /data/data/$APP_PACKAGE/$d -type f" | tr -d '\r' > tmp_files.txt

    while IFS= read -r -u 3 f; do
        echo "$f"

        # Determine the relative path for the destination
        REL_PATH=$(echo "$f" | sed "s|^/data/data/$APP_PACKAGE/||")
        DEST_PATH=$REL_PATH

        # Echo out which file is being copied
        echo "Copying $f to $DEST_PATH"

        # Directly save the file content to a temporary location on the device
        adb shell "run-as $APP_PACKAGE cat $f > /data/local/tmp/temp_file"

        # Create the destination directory if it doesn't exist (remove the file name from the path)
        DEST_DIR=$(dirname "/data/data/$DEST_APP_PACKAGE/$DEST_PATH")
        adb shell "run-as $DEST_APP_PACKAGE mkdir -p $DEST_DIR"

        # Move the file from the temporary location to the desired destination
        adb shell "run-as $DEST_APP_PACKAGE cp /data/local/tmp/temp_file /data/data/$DEST_APP_PACKAGE/$DEST_PATH"

        # If the file is $APP_PACKAGE_preferences.xml, rename it to $DEST_APP_PACKAGE_preferences.xml
        if [ "$REL_PATH" == "shared_prefs/${APP_PACKAGE}_preferences.xml" ]; then
            adb shell "run-as $DEST_APP_PACKAGE mv /data/data/$DEST_APP_PACKAGE/$DEST_PATH /data/data/$DEST_APP_PACKAGE/shared_prefs/${DEST_APP_PACKAGE}_preferences.xml"
        fi

        # Delete the temp_file on the device
        adb shell "rm /data/local/tmp/temp_file"
    done 3< tmp_files.txt
done

# Clean up the temporary file at the end of the script
rm tmp_files.txt

# Step 2: Copy over already granted permissions
# Get all permissions for the source package and filter the granted ones
adb shell pm dump $APP_PACKAGE | grep "permission" | grep "granted=true" > temp_permissions.txt

while IFS=: read -r -u 3 permission _; do
    # Grant the permission to the destination package and ignore errors
    adb shell pm grant $DEST_APP_PACKAGE $permission 2> /dev/null
done 3< temp_permissions.txt

echo "Permissions granted to $DEST_APP_PACKAGE"
rm temp_permissions.txt
