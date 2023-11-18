# Takes 1 argument: path to signing config file

# Signing config file format:
# Path to keystore
# Keystore password
# Key alias
# Key password
# Destination folder

# Assemble the APK
sh "${BASH_SOURCE%/*}"/../gradlew assembleStaging \
    -Pandroid.injected.signing.store.file=$(head -n 1 "$1") \
    -Pandroid.injected.signing.store.password=$(head -n 2 "$1" | tail -n 1) \
    -Pandroid.injected.signing.key.alias=$(head -n 3 "$1" | tail -n 1) \
    -Pandroid.injected.signing.key.password=$(head -n 4 "$1" | tail -n 1)

# Get the version name from the gradle file
export VERSION_CODE=$(LC_ALL=C.UTF-8 grep -oP '(?<=versionCode = )\d+' "${BASH_SOURCE%/*}"/../app/build.gradle.kts)

# Copy the files to the destination folder (line 5 of the signing config file)
echo "Copying files to $(head -n 5 "$1" | tail -n 1)"
cp "${BASH_SOURCE%/*}"/../app/build/outputs/apk/staging/app-staging.apk $(head -n 5 "$1" | tail -n 1)/${VERSION_CODE}-staging.apk