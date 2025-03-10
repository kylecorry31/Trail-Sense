import os
import json

script_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = f"{script_dir}/.."

with open(f"{root_dir}/app/src/main/res/raw/field_guide_pages.json", 'r') as file:
    pages = json.load(file)['pages']
    tags = ['Plant', 'Fungus', 'Insect', 'Worm', 'Mollusk', 'Crustacean', 'Mammal', 'Bird', 'Reptile', 'Amphibian', 'Fish', 'Rock', 'Weather']
    # Sort pages by min index of the above tags, then by name - pages contain multiple tags
    pages.sort(key=lambda x: (min([tags.index(tag) if tag in tags else 1000 for tag in x['tags']]), x['content']))

content = "This chapter contains some common animals, plants, fungi, and rocks that may be useful in a survival situation. Always use caution when identifying plants, animals, or mushrooms. Some species may be dangerous or protected. If you are unsure about an identification, consult a professional.\n\n"

for page in pages:
    with open(f"{root_dir}/app/src/main/res/{page['content']}.txt", 'r') as file:
        text = file.read()
        file_content = '\\pagebreak\n\n## ' + text.split('\n')[0] + f'\n\n![](file:///android_asset/{page['image']})\n\n' + '\n'.join(text.split('\n')[1:])

        # Remove any line that starts with http
        file_content = '\n'.join([line for line in file_content.split('\n') if not line.startswith('http')])

        file_content = file_content.strip()
        content += '\n\n' + file_content

content = content.strip()

with open("field_guide.md", 'w') as file:
    file.write(content)
