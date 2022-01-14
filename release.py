import re
import os

# TODO: Build APK
apk = input('Is the APK up to date? ').lower().startswith('y')

if not apk:
    raise Exception("APK is not up to date")

gradle = open('app/build.gradle.kts', 'r')
contents = gradle.read()
gradle.close()

version_code = re.search('versionCode = (\d+)', contents).group(1)
version_name = re.search('versionName = "(.+)"', contents).group(1)

changelog = 'fastlane/metadata/android/en-US/changelogs/' + version_code + '.txt'

if not os.path.exists(changelog):
    raise Exception("Changelog does not exist (" + changelog + ")")

print("Creating draft release for version " + version_name + " (" + version_code + ")")

os.system("gh release create " + version_name + " app/build/outputs/apk/debug/app-debug.apk -F " + changelog + " -d -t " + version_name)