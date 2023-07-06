import xml.etree.ElementTree as ET
import os
import re

def read_xml(file) -> ET.ElementTree:
    ET.register_namespace('tools', 'http://schemas.android.com/tools')
    parser = ET.XMLParser(target=ET.TreeBuilder(insert_comments=True))
    return ET.parse(file, parser=parser)

def write_xml(tree, file):
    tree.write(file, encoding='utf-8', xml_declaration=True)

def delete_element(tree, element):
    root = tree.getroot()
    root.remove(element)

def replace_text(element, text):
    element.text = text

def add_attribute(element, name, value):
    element.set(name, value)

def get_string_element(tree, name):
    root = tree.getroot()
    return root.find('string[@name="' + name + '"]')

class StringDiagnostic(object):
    def check(self, source_tree, tree, element) -> bool:
        return True

    def fix(self, source_tree, tree, element):
        pass

class NonTranslatableDiagnostic(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        if tree == source_tree:
            return False
        source_element = get_string_element(source_tree, element.get('name'))
        return source_element.get('translatable') == 'false'

    def fix(self, source_tree, tree, element):
        delete_element(tree, element)


script_dir = os.path.dirname(os.path.realpath(__file__))

reference_file = script_dir + '/../app/src/main/res/values/strings.xml'
reference_tree = read_xml(reference_file)

diagnostics = [
    NonTranslatableDiagnostic()
]

# Find all strings.xml files
for root, dirs, files in os.walk(script_dir + '/../app/src/main/res'):
    for file in files:
        if file == 'strings.xml':
            strings_file_path = os.path.join(root, file)
            for diagnostic in diagnostics:
                tree = read_xml(strings_file_path)
                root = tree.getroot()
                for string_elem in root.iter('string'):
                    if diagnostic.check(reference_tree, tree, string_elem):
                        diagnostic.fix(reference_tree, tree, string_elem)
                        print(f'Fixed {string_elem.get("name")} in {strings_file_path}')
                        break
            write_xml(tree, strings_file_path)