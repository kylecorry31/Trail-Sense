# Takes 1 argument: path to signing config file

# Signing config file format:
# Path to keystore (unused)
# Keystore password (unused)
# Key alias (unused)
# Key password (unused)
# Destination folder

# Assemble the APK
config_file="$1"
destination_folder="$(sed -n '5p' "$config_file" | tr -d '\r')"

sh ./gradlew assembleStaging

# Get the version name from the gradle file
export VERSION_CODE=$(LC_ALL=C.UTF-8 grep -oP '(?<=versionCode = )\d+' app/build.gradle.kts)

# Copy the files to the destination folder
echo "Copying files to $destination_folder"
cp ./app/build/outputs/apk/staging/app-staging.apk "$destination_folder/${VERSION_CODE}-staging.apk"