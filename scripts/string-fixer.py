import xml.etree.ElementTree as ET
import os
import re

should_fix_issues = True
show_warnings = True


def read_xml(file) -> ET.ElementTree:
    ET.register_namespace('tools', 'http://schemas.android.com/tools')
    parser = ET.XMLParser(target=ET.TreeBuilder(insert_comments=True))
    return ET.parse(file, parser=parser)

def write_xml(tree: ET.ElementTree, file):
    with open(file, 'r', encoding='utf-8') as f:
        text = f.read()
        closing_tag = '</resources>'
        closing_tag_index = text.rfind(closing_tag)
        if closing_tag_index > -1:
            ending = text[(text.rfind(closing_tag) + len(closing_tag)):]
        else:
            ending = ''

    tree.write(file, encoding='utf-8', xml_declaration=True, short_empty_elements=True)
    # Replace the single quotes with double quotes
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    with open(file, 'w', encoding='utf-8') as f:
        f.write(content.replace("<?xml version='1.0' encoding='utf-8'?>", '<?xml version="1.0" encoding="utf-8"?>').strip() + ending)

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

def address_issue(tree, element, diagnostic, file):
    diagnostic_name = diagnostic.__class__.__name__
    file_parent_folder = file.split('/')[-2]
    file_name = file.split('/')[-1]
    if should_fix_issues and diagnostic.fix(tree, tree, element):
        print(f'[Fixed] {diagnostic_name} {element.get("name")} in {file_parent_folder}/{file_name}')
    elif not diagnostic.is_warning():
        print(f'[Fail] {diagnostic_name} {element.get("name")} in {file_parent_folder}/{file_name}')
    elif show_warnings:
        print(f'[Warn] {diagnostic_name} {element.get("name")} in {file_parent_folder}/{file_name}')

class StringDiagnostic(object):
    def check(self, source_tree, tree, element) -> bool:
        return True

    def fix(self, source_tree, tree, element) -> bool:
        return False

    def is_warning(self) -> bool:
        return False

class NonTranslatableTranslated(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        if tree == source_tree:
            return False
        source_element = get_string_element(source_tree, element.get('name'))
        if source_element is None:
            return False
        return source_element.get('translatable') == 'false'

    def fix(self, source_tree, tree, element) -> bool:
        delete_element(tree, element)
        return True

    def is_warning(self) -> bool:
        return True

class URLMismatch(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        if source_tree != tree:
            return False
        source_string_value = get_string_element(source_tree, element.get('name')).text.strip()
        string_value = element.text.strip()

        source_urls = self.__get_urls(source_string_value)
        urls = self.__get_urls(string_value)

        if len(source_urls) != len(urls):
            return True

        for i in range(len(source_urls)):
            if source_urls[i] != urls[i]:
                return True

        return False

    def __get_urls(self, text):
        # Regex to get all URLs
        r = r'(https?://[^\s]+)'
        # Find all matches
        return re.findall(r, text)

    def fix(self, source_tree, tree, element) -> bool:
        delete_element(tree, element)
        return True

    def is_warning(self) -> bool:
        return False

class PreferenceKeyTranslatable(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        if source_tree != tree:
            return False
        string_value = element.text.strip()
        if '_' in string_value and all(char.islower() or char.isdigit() or char == '_' for char in string_value):
            if not string_elem.get('translatable') or string_elem.get('translatable').lower() != 'false':
                return True
        return False

    def fix(self, source_tree, tree, element) -> bool:
        add_attribute(element, 'translatable', 'false')
        return True

    def is_warning(self) -> bool:
        return True

class FormattingDoesNotMatch(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        if source_tree == tree:
            return False
        source_element = get_string_element(source_tree, element.get('name'))
        if source_element is None:
            return False

        # Get the format arguments from the source string
        source_format_args = self.__get_format_args(source_element.text)

        # Get the format arguments from the string
        format_args = self.__get_format_args(element.text)

        # If the number of format arguments does not match, return true
        if len(source_format_args) != len(format_args):
            return True

        # If the format arguments do not match, return true
        remaining = format_args.copy()
        for source_format_arg in source_format_args:
            if source_format_arg in remaining:
                remaining.remove(source_format_arg)
            else:
                return True

        return False

    def fix(self, source_tree, tree, element) -> bool:
        delete_element(tree, element)
        return True

    def is_warning(self) -> bool:
        return False

    def __get_format_args(self, text):
        # Avoid counting %% as a format argument
        t = text.replace('%%', '')

        # Get the format arguments from the source string (ex. %s, %d, %1$s, %0.2f) but %% is ignored
        r = r'%[^a-zA-Z]*[a-zA-Z]'

        # Find all matches
        return re.findall(r, t)

class PositionalFormattingUnspecified(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        # Regex to get all format arguments without a positional index
        r = r'%[sd]'
        # Find all matches
        matches = re.findall(r, element.text)
        # If there are no matches, continue
        if not matches or len(matches) < 2:
            return False
        return True

    def fix(self, source_tree, tree, element) -> bool:
        # Regex to get all format arguments without a positional index
        r = r'%[sd]'
        # Find all matches
        matches = re.findall(r, element.text)
        # Add the positional index to the matches within the string
        for i, match in enumerate(matches):
            replace_text(element, element.text.replace(match, '%' + str(i + 1) + '$' + match[1], 1))
        return True

    def is_warning(self) -> bool:
        return True

class NotInSource(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        if source_tree == tree:
            return False
        source_element = get_string_element(source_tree, element.get('name'))
        return source_element is None

    def fix(self, source_tree, tree, element) -> bool:
        delete_element(tree, element)
        return True

    def is_warning(self) -> bool:
        return True

class TranslatedAppName(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        app_name = get_string_element(source_tree, 'app_name').text
        source_element = get_string_element(source_tree, element.get('name'))
        if source_element is not None and app_name in source_element.text:
            return app_name not in element.text
        return False

    def fix(self, source_tree, tree, element) -> bool:
        return False

    def is_warning(self) -> bool:
        return True

class HardCodedAppName(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        if source_tree != tree:
            return False
        app_name = get_string_element(source_tree, 'app_name').text
        source_element = get_string_element(source_tree, element.get('name'))
        if source_element is not None and app_name in source_element.text and element.attrib.get('translatable') != 'false':
            return app_name in element.text
        return False

    def fix(self, source_tree, tree, element) -> bool:
        return False

    def is_warning(self) -> bool:
        return True

class EmptyTranslation(StringDiagnostic):
    def check(self, source_tree, tree, element) -> bool:
        if source_tree == tree:
            return False
        return element.text is None or len(element.text.strip()) == 0

    def fix(self, source_tree, tree, element) -> bool:
        delete_element(tree, element)
        return True

    def is_warning(self) -> bool:
        return True

script_dir = os.path.dirname(os.path.realpath(__file__)).replace('\\', '/')

reference_file = script_dir + '/../app/src/main/res/values/strings.xml'
reference_tree = read_xml(reference_file)

diagnostics = [
    EmptyTranslation(),
    PreferenceKeyTranslatable(),
    NonTranslatableTranslated(),
    FormattingDoesNotMatch(),
    NotInSource(),
    PositionalFormattingUnspecified(),
    TranslatedAppName(),
    HardCodedAppName(),
    URLMismatch()
]


# Run diagnostics on reference file
for diagnostic in diagnostics:
    for string_elem in reference_tree.iter('string'):
        if diagnostic.check(reference_tree, reference_tree, string_elem):
            address_issue(reference_tree, string_elem, diagnostic, reference_file)
if should_fix_issues:
    write_xml(reference_tree, reference_file)


# Find all strings.xml files
for root, dirs, files in os.walk(script_dir + '/../app/src/main/res'):
    for file in files:
        if file == 'strings.xml':
            strings_file_path = os.path.join(root, file).replace('\\', '/')
            if strings_file_path == reference_file:
                continue
            tree = read_xml(strings_file_path)
            root = tree.getroot()
            for diagnostic in diagnostics:
                for string_elem in root.iter('string'):
                    if diagnostic.check(reference_tree, tree, string_elem):
                        address_issue(tree, string_elem, diagnostic, strings_file_path)
            if should_fix_issues:
                write_xml(tree, strings_file_path)
