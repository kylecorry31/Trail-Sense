import xml.etree.ElementTree as ET
import os
import re

def check_strings(reference_root, file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        text = f.read()
    
    tree = ET.parse(file_path)
    root = tree.getroot()

    for string_elem in root.iter('string'):
        string_name = string_elem.get('name')
        reference_string = reference_root.find('string[@name="' + string_name + '"]')
        if reference_string is None:
            print(f'Match FAIL: {string_name} in {file_path}')
            continue
        # If reference string is not translatable, continue
        if reference_string.get('translatable') == 'false':
            print(f'Translatable FAIL: {string_name} in {file_path}')
            continue
        # Count % signs in the reference string
        reference_count = reference_string.text.count('%')
        # Count % signs in the string
        string_count = string_elem.text.count('%')

        if string_count != reference_count:
            print(f'Formatting FAIL: {string_name} in {file_path}')

# Get the script's directory
script_dir = os.path.dirname(os.path.realpath(__file__))

reference_file = script_dir + '/../app/src/main/res/values/strings.xml'
reference_tree = ET.parse(reference_file)
reference_root = reference_tree.getroot()

# Find all strings.xml files
for root, dirs, files in os.walk(script_dir + '/../app/src/main/res'):
    for file in files:
        if file == 'strings.xml':
            strings_file_path = os.path.join(root, file)
            if strings_file_path != reference_file:
                check_strings(reference_root, strings_file_path)

