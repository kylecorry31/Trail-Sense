import xml.etree.ElementTree as ET
import os
import re

def fix_strings(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        text = f.read()

    tree = ET.parse(file_path)
    root = tree.getroot()

    for string_elem in root.iter('string'):
        string_name = string_elem.get('name')
        original_string_value = string_elem.text
        string_value = string_elem.text.strip()

        if string_name and string_value:
            # Regex to get all format arguments without a positional index
            r = r'%[sd]'
            # Find all matches
            matches = re.findall(r, string_value)

            # If there are no matches, continue
            if not matches or len(matches) < 2:
                continue

            # Add the positional index to the matches within the string
            for i, match in enumerate(matches):
                string_value = string_value.replace(match, '%' + str(i + 1) + '$' + match[1], 1)
            
            text = text.replace('>' + original_string_value + '<', '>' + string_value + '<')
        
            # Replace the string value with the new one
            string_elem.text = string_value
        
    # Write the text back to the file
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(text)

# Get the script's directory
script_dir = os.path.dirname(os.path.realpath(__file__))

# Find all strings.xml files
for root, dirs, files in os.walk(script_dir + '/../app/src/main/res'):
    for file in files:
        if file == 'strings.xml':
            strings_file_path = os.path.join(root, file)
            fix_strings(strings_file_path)

