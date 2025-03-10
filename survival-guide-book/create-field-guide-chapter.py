import os
import json

script_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = f"{script_dir}/.."

with open(f"{root_dir}/app/src/main/res/raw/field_guide_pages.json", 'r') as file:
    pages = json.load(file)['pages']
    tags = ['C_Plant', 'C_Fungus', 'C_Insect', 'C_Worm', 'C_Mollusk', 'C_Crustacean', 'C_Mammal', 'C_Bird', 'C_Reptile', 'C_Amphibian', 'C_Fish', 'C_Rock', 'C_Weather']
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

        # Add the tags to the bottom
        # TODO: Use icons
        # Group tags by category
        tag_groups = {
            'Location': [],
            'Habitat': [],
            'Activity Pattern': []
        }

        for tag in page['tags']:
            # Insert spaces between capital letters
            tag_with_spaces = ''.join([' ' + c if c.isupper() and i > 0 else c for i, c in enumerate(tag)]).strip()
            tag_name = tag_with_spaces.split('_')[1].strip()

            if tag.startswith('L_'):
                tag_groups['Location'].append(tag_name)
            elif tag.startswith('H_'):
                tag_groups['Habitat'].append(tag_name)
            elif tag.startswith('A_'):
                tag_groups['Activity Pattern'].append(tag_name)

        if len(tag_groups['Location']) == 7:
            tag_groups['Location'] = ['Worldwide']

        if 'C_Weather' in page['tags']:
            tag_groups['Location'] = []

        # Build the tag string
        tag_strings = []
        for category, tags in tag_groups.items():
            if tags:
                tag_strings.append(f"### {category}\n{', '.join(tags)}")

        file_content += '\n\n' + '\n\n'.join(tag_strings)



        content += '\n\n' + file_content

content = content.strip()

with open("field_guide.md", 'w') as file:
    file.write(content)
