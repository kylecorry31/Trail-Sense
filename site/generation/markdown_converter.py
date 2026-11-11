import markdown
import os
import datetime
import re
import yaml

MIN_TOC_SECTIONS = 3
MAX_SUMMARY_LENGTH = 160

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

def split_front_matter(text):
    """Splits text into its YAML front matter (empty if there is none) and the remaining content."""
    lines = text.split("\n")
    if lines[0] != "---":
        return {}, text
    end = lines.index("---", 1)
    metadata = yaml.load("\n".join(lines[1:end]), Loader=yaml.FullLoader) or {}
    return metadata, "\n".join(lines[end + 1:])

def get_summary(md_text):
    """Returns the first paragraph of the markdown as plain text, or None if it doesn't start with one."""
    for block in md_text.split("\n\n"):
        block = " ".join(block.split())
        if block == "" or block.startswith("<!--"):
            continue
        if re.match(r"[#<>|!*-]|\d+\.", block):
            return None
        summary = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", block)
        summary = re.sub(r"[*_`]", "", summary)
        if len(summary) > MAX_SUMMARY_LENGTH:
            summary = summary[:MAX_SUMMARY_LENGTH].rsplit(" ", 1)[0] + "..."
        return summary
    return None

def get_markdown_metadata(md_file_path, updated_url):
    with open(md_file_path, mode="r", encoding="utf-8") as input_file:
        text = input_file.read()

    metadata, text_without_metadata = split_front_matter(text)

    metadata['url'] = updated_url
    metadata['path'] = md_file_path
    metadata['raw_content'] = text_without_metadata
    md = markdown.Markdown(
        extensions=["toc"],
        extension_configs={"toc": {"permalink": "#", "permalink_title": "Link to this section"}},
    )
    metadata['content'] = md.convert(text_without_metadata)

    # Short pages don't need a table of contents
    sections = [{"id": token["id"], "name": token["name"]} for token in md.toc_tokens]
    metadata['toc'] = {"sections": sections} if len(sections) >= MIN_TOC_SECTIONS else None

    if 'title' not in metadata:
        metadata['title'] = os.path.basename(md_file_path).replace(".md", "")

    if 'description' not in metadata:
        metadata['description'] = get_summary(text_without_metadata)

    if isinstance(metadata.get('date'), datetime.date):
        date = metadata['date']
        metadata['date_display'] = f"{date:%B} {date.day}, {date.year}"

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

    sort_method = lambda item: item['order'] if 'order' in item else item['date'] if 'date' in item else item['title']

    # Reverse sort if any item contains a date or an explicit order
    sort_reverse = any('date' in item or 'order' in item for item in items)

    metadata['items'] = sorted(items, key=sort_method, reverse=sort_reverse)

    for i, item in enumerate(metadata['items']):
        neighbors = {'previous': i - 1, 'next': i + 1}
        for name, index in neighbors.items():
            if 0 <= index < len(items):
                neighbor = metadata['items'][index]
                item[name] = {'title': neighbor['title'], 'url': neighbor['url']}

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