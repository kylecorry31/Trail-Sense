# Takes 1 argument: path to signing config file

# Signing config file format:
# Path to keystore
# Keystore password
# Key alias
# Key password
# Destination folder

# Assemble the APK
config_file="$1"
store_file="$(sed -n '1p' "$config_file" | tr -d '\r')"
store_password="$(sed -n '2p' "$config_file" | tr -d '\r')"
key_alias="$(sed -n '3p' "$config_file" | tr -d '\r')"
key_password="$(sed -n '4p' "$config_file" | tr -d '\r')"
destination_folder="$(sed -n '5p' "$config_file" | tr -d '\r')"

sh "${BASH_SOURCE%/*}"/../gradlew assembleStaging \
    -Pandroid.injected.signing.store.file=$store_file \
    -Pandroid.injected.signing.store.password=$store_password \
    -Pandroid.injected.signing.key.alias=$key_alias \
    -Pandroid.injected.signing.key.password=$key_password

# Get the version name from the gradle file
export VERSION_CODE=$(LC_ALL=C.UTF-8 grep -oP '(?<=versionCode = )\d+' "${BASH_SOURCE%/*}"/../app/build.gradle.kts)

# Copy the files to the destination folder
echo "Copying files to $destination_folder"
cp "${BASH_SOURCE%/*}"/../app/build/outputs/apk/staging/app-staging.apk "$destination_folder/${VERSION_CODE}-staging.apk"