import re
import os
import subprocess
import tempfile

# TODO: Build APK
# apk = input('Is the APK up to date? ').lower().startswith('y')
# 
# if not apk:
#     raise Exception("APK is not up to date")

gradle = open('app/build.gradle.kts', 'r')
contents = gradle.read()
gradle.close()

version_code = re.search(r'versionCode = (\d+)', contents).group(1)
version_name = re.search('versionName = "(.+)"', contents).group(1)

changelog = 'fastlane/metadata/android/en-US/changelogs/' + version_code + '.txt'

if not os.path.exists(changelog):
    raise Exception("Changelog does not exist (" + changelog + ")")

print("Creating draft release for version " + version_name + " (" + version_code + ")")

milestone_url = None
milestones = subprocess.run(
    [
        "gh",
        "api",
        "repos/:owner/:repo/milestones?state=all&per_page=100",
        "--paginate",
        "--jq",
        ".[] | [.title, .html_url] | @tsv",
    ],
    capture_output=True,
    text=True,
)

if milestones.returncode == 0:
    for milestone in milestones.stdout.splitlines():
        if "\t" not in milestone:
            continue
        title, url = milestone.split("\t", 1)
        if title == version_name:
            milestone_url = url
            break
else:
    print("Unable to check for release milestone")

with open(changelog, "r") as changelog_file:
    changelog_contents = changelog_file.read()

detailed_release_notes_url = (
    "https://github.com/kylecorry31/Trail-Sense/blob/main/release-notes/"
    + version_name
    + ".md"
)
release_notes = (
    "[Read the detailed release notes]("
    + detailed_release_notes_url
    + ")\n\n## Highlights\n\n"
    + changelog_contents
)

if milestone_url:
    release_notes += "\n\nMilestone: [" + version_name + "](" + milestone_url + ")\n"

with tempfile.NamedTemporaryFile("w", delete=False) as release_notes_file:
    release_notes_file.write(release_notes)
    release_notes_path = release_notes_file.name

try:
    subprocess.run(
        [
            "gh",
            "release",
            "create",
            version_name,
            "-F",
            release_notes_path,
            "-d",
            "-t",
            version_name,
        ],
        check=True,
    )
finally:
    os.remove(release_notes_path)
