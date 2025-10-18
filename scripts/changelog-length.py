import sys
import os

character_limit = 500


def check_changelog_length(changelog_number):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    changelog_path = os.path.join(
        project_root,
        "fastlane",
        "metadata",
        "android",
        "en-US",
        "changelogs",
        f"{changelog_number}.txt",
    )

    if not os.path.exists(changelog_path):
        print(f"Changelog file not found: {changelog_path}")
        sys.exit(1)

    with open(changelog_path, "r", encoding="utf-8") as f:
        content = f.read()

    char_count = len(content)

    print(f"Changelog {changelog_number}: {char_count} characters")

    if char_count > character_limit:
        print(
            f"WARNING: Changelog exceeds 500 characters by {char_count - character_limit} characters"
        )
    else:
        remaining = character_limit - char_count
        print(f"{remaining} characters remaining")

    return char_count


def main():
    if len(sys.argv) != 2:
        print("Usage: python changelog-length.py <changelog_number>")
        sys.exit(1)

    changelog_number = sys.argv[1]
    check_changelog_length(changelog_number)


if __name__ == "__main__":
    main()
