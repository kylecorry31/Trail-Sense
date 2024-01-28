import xml.etree.ElementTree as ET
import os
import sys

def read_xml(file) -> ET.ElementTree:
    ET.register_namespace('tools', 'http://schemas.android.com/tools')
    parser = ET.XMLParser(target=ET.TreeBuilder(insert_comments=True))
    return ET.parse(file, parser=parser)

def write_xml(tree: ET.ElementTree, file):
    tree.write(file, encoding='utf-8', xml_declaration=True, short_empty_elements=False)
    # Replace the single quotes with double quotes
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    with open(file, 'w', encoding='utf-8') as f:
        f.write(content.replace("<?xml version='1.0' encoding='utf-8'?>", '<?xml version="1.0" encoding="utf-8"?>'))

def delete_element(tree, element):
    root = tree.getroot()
    root.remove(element)

def get_string_element(tree, name):
    root = tree.getroot()
    elt1 = root.find('string[@name="' + name + '"]')
    if elt1 is not None:
        return elt1
    elt2 = root.find('plurals[@name="' + name + '"]')
    return elt2

script_dir = os.path.dirname(os.path.realpath(__file__)).replace('\\', '/')

# Read the key to remove from the arguments
if len(sys.argv) < 2:
    print('Please provide the key to remove as an argument')
    exit()
key_to_remove = sys.argv[1]

# Find all strings.xml files
for root, dirs, files in os.walk(script_dir + '/../app/src/main/res'):
    for file in files:
        if file == 'strings.xml':
            strings_file_path = os.path.join(root, file).replace('\\', '/')
            
            tree = read_xml(strings_file_path)
            root = tree.getroot()

            elt = get_string_element(tree, key_to_remove)
            if elt is not None:
                delete_element(tree, elt)
                print(f'[Removed] {key_to_remove} in {strings_file_path}')
                write_xml(tree, strings_file_path)