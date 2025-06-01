import markdown
import os
import yaml

def get_md_content(md_file_path):
    with open(md_file_path, mode="r", encoding="utf-8") as input_file:
        text = input_file.read()

    # Remove metadata from text
    lines = text.split("\n")
    if lines[0] == "---":
        i = 1
        while lines[i] != "---":
            i += 1
        text = "\n".join(lines[i+1:])

    return text

def get_markdown_metadata(md_file_path, updated_url):
    with open(md_file_path, mode="r", encoding="utf-8") as input_file:
        text = input_file.read()

    lines = text.split("\n")

    metadata_text = ""
    if lines[0] == "---":
        i = 1
        while lines[i] != "---":
            metadata_text += lines[i] + "\n"
            i += 1
        text_without_metadata = "\n".join(lines[i+1:])
        # Parse the metadata as yaml
        metadata = yaml.load(metadata_text, Loader=yaml.FullLoader)
    else:
        text_without_metadata = text
        metadata = {}


    metadata['url'] = updated_url
    metadata['raw_content'] = text_without_metadata
    metadata['content'] = markdown.markdown(text_without_metadata)

    if 'title' not in metadata:
        metadata['title'] = os.path.basename(md_file_path).replace(".md", "")

    return metadata

def get_collection_metadata(collection_path):
    items = []
    for root, dirs, files in os.walk(collection_path):
        for name in files:
            if name.endswith(".md"):
                url = os.path.join(root.replace("\\", "/").replace("src/", "/"), name.replace(".md", "/index.html")).replace("\\", "/")
                meta = get_markdown_metadata(os.path.join(root, name), url)
                items.append(meta)
    metadata = {}

    sort_method = lambda item: item['date'] if 'date' in item else item['title']
    
    # Reverse sort if any item contains a date
    sort_reverse = any('date' in item for item in items)

    metadata['items'] = sorted(items, key=sort_method, reverse=sort_reverse)

    categories = {}
    for item in items:
        if 'category' in item:
            if item['category'] not in categories:
                categories[item['category']] = []
            categories[item['category']].append(item)
    
    category_list = []
    # Sort the categories alphabetically, and sort the items in each category alphabetically
    for category in sorted(categories.keys()):
        category_list.append({
            "name": category,
            "items": sorted(categories[category], key=sort_method, reverse=sort_reverse)
        })
    metadata['categories'] = category_list

    for category in category_list:
        metadata['category_' + category['name']] = category['items']

    return metadata