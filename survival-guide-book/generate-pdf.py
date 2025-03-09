import os
import re
import base64
from PIL import Image

script_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = f"{script_dir}/.."

# TODO: Better looking title page
# TODO: Add index using keywords
# TODO: Links (replace with text)

chapters = [
    {
        "title": "Overview",
        "file": "app/src/main/res/raw/guide_survival_chapter_overview.md"
    },
    {
        "title": "Medical",
        "file": "app/src/main/res/raw/guide_survival_chapter_medical.md"
    },
    {
        "title": "Water",
        "file": "app/src/main/res/raw/guide_survival_chapter_water.md"
    },
    {
        "title": "Food",
        "file": "app/src/main/res/raw/guide_survival_chapter_food.md"
    },
    {
        "title": "Fire",
        "file": "app/src/main/res/raw/guide_survival_chapter_fire.md"
    },
    {
        "title": "Shelter and Clothing",
        "file": "app/src/main/res/raw/guide_survival_chapter_shelter_and_clothing.md"
    },
    {
        "title": "Navigation",
        "file": "app/src/main/res/raw/guide_survival_chapter_navigation.md"
    },
    {
        "title": "Weather",
        "file": "app/src/main/res/raw/guide_survival_chapter_weather.md"
    },
    {
        "title": "Field Guide",
        "file": "survival-guide-book/field_guide.md"
    }
]

full_resolution_directory = "survival-guide-book/images"

content = ""

# Grab the disclaimer section from the overview chapter
with open(f"{root_dir}/{chapters[0]["file"]}", 'r') as overview_file:
    overview_content = overview_file.read()
    disclaimer_idx = overview_content.index("## Disclaimer")
    disclaimer = overview_content[disclaimer_idx + len("## Disclaimer"):]

    content += f"# Disclaimer\n\n{disclaimer}\n\n"

for chapter in chapters:
    content += f"# {chapter['title']}\n\n"
    with open(f"{root_dir}/{chapter['file']}", 'r') as file:
        file_content = file.read()
        file_content = file_content.replace("file:///android_asset/", f"{root_dir}/app/src/main/assets/")

        # Title case level 2 headers
        h2_regex = re.compile(r'^## (.*?)$', re.MULTILINE)
        def title_case_h2(match):
            ignored_words = [
                'a',
                'or',
                'of',
                'in',
                'and',
                'the',
                'to',
                'for',
                "GPS"
            ]

            prefix = ''
            if chapter['title'] == 'Medical':
                prefix = '\\pagebreak\n\n'

            return f'{prefix}## ' + ' '.join([word.title() if word.lower() not in ignored_words else word for word in match.group(1).split()])
        file_content = h2_regex.sub(title_case_h2, file_content)

        # Replace all images with their base64 content as a JPG
        image_regex = re.compile(r'!\[(.*?)\]\((.*?)\)')
        images = image_regex.findall(file_content)
        for image in images:

            file_path = image[1]
            if os.path.exists(f'{root_dir}/{full_resolution_directory}/{file_path.split('/')[-1]}'):
                file_path = f'{root_dir}/{full_resolution_directory}/{file_path.split('/')[-1]}'

            with open(file_path, 'rb') as image_file:
                image_bytes = image_file.read()
                pil_image = Image.open(image_file)
                # Save to a temp jpg and then read the bytes as base 64
                pil_image.save("temp.jpg")
                with open("temp.jpg", 'rb') as temp_jpg_file:
                    jpg_bytes = temp_jpg_file.read()
                    base64_image = base64.b64encode(jpg_bytes).decode('utf-8')
                    file_content = file_content.replace(f"![{image[0]}]({image[1]})", f"![](data:image/jpeg;base64,{base64_image}){{ height=3in margin=auto }}")

        # Remove the disclaimer section
        disclaimer_index = file_content.find("## Disclaimer")
        if disclaimer_index != None and disclaimer_index > 0:
            file_content = file_content[:disclaimer_index]

        # Automatically insert a newline before the first list item
        indices = []
        lines = file_content.split('\n')
        for i in range(len(lines)):
            if lines[i].startswith('- ') and (i == 0 or not lines[i-1].startswith('- ')):
                indices.append(i)
            if lines[i].startswith('1.'):
                indices.append(i)

        lines_with_spacing = []
        for i in range(len(lines)):
            if i in indices:
                lines_with_spacing.append('')
            lines_with_spacing.append(lines[i])

        file_content = '\n'.join(lines_with_spacing)

        content += file_content
    content += "\n\n"

content = content.strip()

# Save to a temporary file
with open("temp.md", 'w') as file:
    file.write(content)

# Generate PDF
metadata = {
    "title": "Survival Guide",
    "subtitle": "Trail Sense",
    "author": "Kyle Corry",
    "rights": "Copyright © 2025 Kyle Corry"
}

command = 'pandoc -H head.tex -o Book.pdf temp.md --pdf-engine=xelatex --toc --toc-depth=2 --top-level-division=chapter'
for entry in metadata:
    command += f' --metadata {entry}="{metadata[entry]}"'

os.system(command)

# Delete temp files
os.remove('temp.md')
os.remove('temp.jpg')
