import os
import re
import base64
from PIL import Image

script_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = f"{script_dir}/.."

# TODO: Add index using keywords

def convert_to_book(files, metadata, book_filetype = 'pdf', before_body_files=[], header_file=None, cover_image=None):
    command = f'pandoc -o Book.{book_filetype} {' '.join(files)} --toc --toc-depth=2 --top-level-division=chapter'
    for entry in metadata:
        command += f' --metadata {entry}="{metadata[entry]}"'
    
    for file in before_body_files:
        command += f' -B {file}'
    
    if header_file:
        command += f' -H {header_file}'
    
    if book_filetype == 'pdf':
        command += ' --pdf-engine=xelatex'
    
    if book_filetype == 'epub':
        command += ' --css=epub-style.css'
        command += ' --split-level=1'
        if cover_image:
            command += f' --epub-cover-image={cover_image}'
    
    os.system(command)

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
        "title": "Shelter",
        "file": "app/src/main/res/raw/guide_survival_chapter_shelter.md"
    },
    {
        "title": "Water",
        "file": "app/src/main/res/raw/guide_survival_chapter_water.md"
    },
    {
        "title": "Fire",
        "file": "app/src/main/res/raw/guide_survival_chapter_fire.md"
    },
    {
        "title": "Food",
        "file": "app/src/main/res/raw/guide_survival_chapter_food.md"
    },
    {
        "title": "Navigation",
        "file": "app/src/main/res/raw/guide_survival_chapter_navigation.md"
    },
    {
        "title": "Weather",
        "file": "app/src/main/res/raw/guide_survival_chapter_weather.md"
    }
]

full_resolution_directory = "survival-guide-book/images"

content = ""

# Grab the disclaimer section from the overview chapter
with open(f"{root_dir}/{chapters[0]["file"]}", 'r') as overview_file:
    overview_content = overview_file.read()
    disclaimer_idx = overview_content.index("## Disclaimer")
    disclaimer = overview_content[disclaimer_idx + len("## Disclaimer"):]

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

        file_content = file_content.replace('<!-- \\pagebreak -->', '\\pagebreak')

        content += file_content
    content += "\n\n"

content = content.strip()

# Save to a temporary file
with open("temp.md", 'w') as file:
    file.write(content)

# Generate PDF
metadata = {
    "title": "Trail Sense",
    "subtitle": "Wilderness Survival Guide",
    "author": "Kyle Corry",
    "year": "2025",
    "identifier": "9798313546445",
    "disclaimer": disclaimer
}

# Create copyright page content
copyright_page = """
\\thispagestyle{{empty}}
\\vspace*{{\\fill}}
\\begin{{center}}
Copyright © {year} {author}\\\\
All rights reserved.\\\\
\\
ISBN: {identifier}
\\end{{center}}
\\vspace*{{\\fill}}
\\newpage
""".format(**metadata)

# Write copyright page to temporary file
with open("copyright.tex", "w") as f:
    f.write(copyright_page)

# Create disclaimer page content
disclaimer_page = """
\\thispagestyle{{empty}}
\\vspace*{{\\fill}}
\\begin{{center}}
\\textbf{{DISCLAIMER}}\\\\
\\
{disclaimer}\\\\
\\end{{center}}
\\vspace*{{\\fill}}
\\newpage
""".format(**metadata)

# Write disclaimer page to temporary file
with open("disclaimer.tex", "w") as f:
    f.write(disclaimer_page)


convert_to_book(['temp.md'], metadata, 'pdf', ['copyright.tex', 'disclaimer.tex'], 'head.tex')

# Convert pagebreaks to divs with the pagebreak class
with open('temp.md', 'r') as file:
    content = file.read()

content = content.replace('\\pagebreak', '::: pagebreak\n:::')

with open('temp.md', 'w') as file:
    file.write(content)

metadata['title'] = 'Trail Sense: Wilderness Survival Guide'
metadata['subtitle'] = ''

convert_to_book(['temp.md'], metadata, 'epub', cover_image=f'{root_dir}/{full_resolution_directory}/cover.jpg')

# Unzip the epub and manually edit the titlepage
os.system('unzip Book.epub -d temp')

with open('temp/EPUB/text/title_page.xhtml', 'r') as file:
    content = file.read()

content = content.replace('<p class="author">Kyle Corry</p>', f"""<p class="author">Kyle Corry</p>
<div class="pagebreak"></div>
<section id="copyright">
    <p>Copyright © {metadata["year"]} {metadata["author"]}</p>
    <p>All rights reserved.</p>
</section>
<div class="pagebreak"></div>
<section id="disclaimer">
    <h2>Disclaimer</h2>
    <p class="disclaimer">{disclaimer}</p>
</section>""")

with open('temp/EPUB/text/title_page.xhtml', 'w') as file:
    file.write(content)

os.system('cd temp && zip -r ../Book.epub *')

# Delete temp files
os.remove('temp.md')
os.remove('temp.jpg')
os.remove('copyright.tex')
os.remove('disclaimer.tex')
os.system('rm -rf temp')
