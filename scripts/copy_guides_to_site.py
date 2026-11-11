import datetime
import os
import shutil
import re
import subprocess

root = os.path.dirname(os.path.realpath(__file__)).replace('\\', '/') + '/..'

def capitalize_title(text):
    words = text.split()
    result = []
    for word in words:
        if word == 'qr':
            result.append('QR')
        elif word:
            result.append(word[0].upper() + word[1:])
    return ' '.join(result)

def process_markdown_content(content):
    # Replace image paths
    content = content.replace('file:///android_asset/', '/assets/images/')
    
    # If line is the start of a list, add a new line before it so the python markdown processor handles it correctly
    lines = content.split('\n')
    processed_lines = []
    
    for i, line in enumerate(lines):
        if ((line.strip().startswith('- ') or re.match(r'^\d+\.', line.strip())) and 
            i > 0 and 
            lines[i-1].strip() != '' and 
            not lines[i-1].strip().startswith('- ') and 
            not re.match(r'^\d+\.', lines[i-1].strip())):
            processed_lines.append('')
        processed_lines.append(line)
    
    return '\n'.join(processed_lines)

def copy_images(content, destination_dir):    
    image_pattern = r'!\[.*?\]\((.*?)\)'
    images = re.findall(image_pattern, content)
    
    for image_path in images:
        if image_path.startswith('file:///android_asset/'):
            actual_path = image_path.replace('file:///android_asset/', root + '/app/src/main/assets/')
            if os.path.exists(actual_path):
                folder = '/'.join(image_path.replace('file:///android_asset/', '').split('/')[:-1])
                actual_destination_dir = os.path.join(destination_dir, folder)
                os.makedirs(actual_destination_dir, exist_ok=True)
                # Copy image to destination
                image_filename = os.path.basename(actual_path)
                shutil.copy2(actual_path, os.path.join(actual_destination_dir, image_filename))

# Copy user guides
for filename in os.listdir(root + "/guides/en-US"):
    if filename.endswith(".txt") and filename.startswith("guide_tool"):
        destination = root + "/site/src/user-guide/" + filename[:-4] + ".md"
        shutil.copy2(root + "/guides/en-US/" + filename, destination)
        with open(destination, 'r', encoding='utf8') as f:
                content = f.read()
        title = capitalize_title(destination.split('/')[-1].split('.')[0].replace('guide_tool', '').replace('_', ' ')).strip()
        with open(destination, 'w') as f:
            new_content = f"""---
title: "{title}"
---

{process_markdown_content(content)}
"""
            copy_images(content, root + "/site/src/assets/images")

            f.write(new_content)

chapters = chapters = [
    "guide_survival_chapter_overview.txt",
    "guide_survival_chapter_medical.txt",
    "guide_survival_chapter_shelter.txt",
    "guide_survival_chapter_water.txt",
    "guide_survival_chapter_fire.txt",
    "guide_survival_chapter_food.txt",
    "guide_survival_chapter_navigation.txt",
    "guide_survival_chapter_weather.txt",
    "guide_survival_chapter_references.txt"
]


# Copy survival guides
# Copy user guides
if os.path.exists(root + "/site/src/assets/images/survival_guide"):
    shutil.rmtree(root + "/site/src/assets/images/survival_guide", ignore_errors=True)
for filename in os.listdir(root + "/guides/en-US"):
    if filename.endswith(".txt") and filename.startswith("guide_survival"):
        destination = root + "/site/src/survival-guide/" + filename[:-4] + ".md"
        shutil.copy2(root + "/guides/en-US/" + filename, destination)
        with open(destination, 'r', encoding='utf8') as f:
                content = f.read()
        chapter_number = chapters.index(filename) + 1
        chapter_name = capitalize_title(destination.split('/')[-1].split('.')[0].replace('guide_survival_chapter', '').replace('_', ' ')).strip()
        title = str(chapter_number) + " - " + chapter_name
        with open(destination, 'w') as f:
            new_content = f"""---
title: "{title}"
page_title: "{chapter_name} - Survival Guide"
---

{process_markdown_content(content)}
"""
            copy_images(content, root + "/site/src/assets/images")

            f.write(new_content)

def get_release_date(version):
    """Returns the date (YYYY-MM-DD) the version was tagged, or today if it is about to be released."""
    result = subprocess.run(
        ["git", "-C", root, "log", "-1", "--format=%cs", "refs/tags/" + version],
        capture_output=True,
        text=True,
    )
    date = result.stdout.strip()
    return date if result.returncode == 0 and date else datetime.date.today().isoformat()

# Copy release notes (unreleased notes are not published)
release_notes_source = root + "/release-notes"
release_notes_destination = root + "/site/src/release-notes"
release_notes_images_destination = root + "/site/src/assets/images/release-notes"

for filename in os.listdir(release_notes_destination):
    if filename.endswith(".md"):
        os.remove(release_notes_destination + "/" + filename)
shutil.rmtree(release_notes_images_destination, ignore_errors=True)

for filename in os.listdir(release_notes_source):
    version = filename[:-3]
    if not re.fullmatch(r'\d+\.\d+\.\d+', version) or not filename.endswith(".md"):
        continue

    with open(release_notes_source + "/" + filename, 'r', encoding='utf8') as f:
        content = f.read()

    content = re.sub(r'\A# .*\n+', '', content)
    major, minor, patch = (int(part) for part in version.split('.'))
    content = re.sub(r'\]\(\./(\d+\.\d+\.\d+)\.md\)', r'](/release-notes/\1/)', content)
    content = re.sub(r'(src=|\]\()"?images/', lambda m: m.group(0).replace('images/', '/assets/images/release-notes/'), content)

    with open(release_notes_destination + "/" + filename, 'w', encoding='utf8') as f:
        f.write(f"""---
title: "{version}"
page_title: "{version} Release Notes"
date: {get_release_date(version)}
order: {major * 1000000 + minor * 1000 + patch}
---

{process_markdown_content(content)}
""")

    images_source = release_notes_source + "/images/" + version
    if os.path.isdir(images_source):
        shutil.copytree(images_source, release_notes_images_destination + "/" + version)
