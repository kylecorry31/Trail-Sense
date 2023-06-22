import xml.etree.ElementTree as ET
import os

def check_strings(file_path):
    tree = ET.parse(file_path)
    root = tree.getroot()

    non_translatable_strings = []

    for string_elem in root.iter('string'):
        string_name = string_elem.get('name')
        string_value = string_elem.text.strip()

        if string_name and string_value:
            # Check if the string shouldn't be translated
            if '_' in string_value and all(char.islower() or char.isdigit() or char == '_' for char in string_value):
                if not string_elem.get('translatable') or string_elem.get('translatable').lower() != 'false':
                    non_translatable_strings.append(string_name)

    return non_translatable_strings

# Get the script's directory
script_dir = os.path.dirname(os.path.realpath(__file__))

strings_file_path = script_dir + '/../app/src/main/res/values/strings.xml'

non_translatable_strings = check_strings(strings_file_path)

for string_name in non_translatable_strings:
    print(string_name)
