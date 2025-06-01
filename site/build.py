import yaml
import os
import shutil
from generation import get_collection_metadata
from generation.markdown_converter import get_markdown_metadata
from generation.render import md_to_html, populate_html
import sys

def read_config():
    if len(sys.argv) > 1:
        config_path = sys.argv[1]
    else:
        config_path = "config.yaml"

    with open(config_path, mode="r", encoding="utf-8") as input_file:
        return yaml.load(input_file, Loader=yaml.FullLoader)

## STEP 1: Assemble the site-wide metadata ##
config = read_config()
source = 'src'
destination = config["destination"] if "destination" in config else "_site"
base_url = config["base_url"] if "base_url" in config else "/"

site_data = {}
site_data['title'] = config["title"] if "title" in config else "My Site"
site_data['description'] = config["description"] if "description" in config else "My site's description"
site_data['base_url'] = base_url
site_data['collections'] = {}

## STEP 2: Assemble the collection metadata (markdown) ##
for root, dirs, files in os.walk(source):
    parent = root.replace("\\", "/").replace("src/", "").replace("src", "")
    if parent != "":
        site_data['collections'][parent] = get_collection_metadata(root)

## STEP 3A: Get the includes ##
includes = {}
for root, dirs, files in os.walk(os.path.join(source, "_includes")):
    for name in files:
        if name.endswith(".html"):
            with open(os.path.join(root, name), mode="r", encoding="utf-8") as input_file:
                html = input_file.read()
            includes[name.replace(".html", "")] = populate_html(site_data, html)

site_data['includes'] = includes

## STEP 3B: Get the defaults ##
defaults = {}
for root, dirs, files in os.walk(os.path.join(source, "_defaults")):
    for name in files:
        if name.endswith(".html"):
            with open(os.path.join(root, name), mode="r", encoding="utf-8") as input_file:
                html = input_file.read()
            defaults[name.replace(".html", "")] = html

## STEP 3: Copy all files from src/ to public/ and convert all .md files to .html files ##

# Clear the contents destination folder, but not the folder itself
if os.path.exists(destination):
    for root, dirs, files in os.walk(destination):
        for name in files:
            os.remove(os.path.join(root, name))
        for name in dirs:
            shutil.rmtree(os.path.join(root, name))
else:
    os.makedirs(destination)


for root, dirs, files in os.walk(source):
    # Don't copy includes/defaults files
    if root.endswith("_includes") or root.endswith("_defaults"):
        continue

    for name in files:
        if name.endswith(".md"):
            md_file_path = os.path.join(root, name)
            url = '/' # TODO: Use the correct url
            metadata = get_markdown_metadata(md_file_path, url)
            template = os.path.join(root, "_item.html")
            if not os.path.exists(template):

                template = defaults["_item"] if "_item" in defaults else "{{{item.content}}}"
            else:
                with open(template, mode="r", encoding="utf-8") as template_file:
                    template = template_file.read()
            html = md_to_html(site_data, metadata, template)

            html_file_path = os.path.join(root.replace(source, destination), name.replace(".md", "/index.html"))
            if not os.path.exists(os.path.dirname(html_file_path)):
                os.makedirs(os.path.dirname(html_file_path))
            with open(html_file_path, mode="w", encoding="utf-8") as output_file:
                output_file.write(html)
        elif name == "_item.html":
            # Don't copy template files
            pass
        elif name.endswith(".html"):
            with open(os.path.join(root, name), mode="r", encoding="utf-8") as input_file:
                html = input_file.read()
            html = populate_html(site_data, html)
            output_path = os.path.join(root.replace(source, destination), name)
            if not os.path.exists(os.path.dirname(output_path)):
                os.makedirs(os.path.dirname(output_path))
            with open(output_path, mode="w", encoding="utf-8") as output_file:
                output_file.write(html)
        else:
            if not os.path.exists(os.path.dirname(os.path.join(root.replace(source, destination), name))):
                os.makedirs(os.path.dirname(os.path.join(root.replace(source, destination), name)))
            shutil.copy(os.path.join(root, name), os.path.join(root.replace(source, destination), name))

## STEP 4: Change all links to reflect base_url ##
for root, dirs, files in os.walk(destination):
    for name in files:
        if name.endswith(".html"):
            with open(os.path.join(root, name), mode="r", encoding="utf-8") as input_file:
                html = input_file.read()
            html = html.replace("href=\"/", "href=\"" + base_url)
            html = html.replace("src=\"/", "src=\"" + base_url)
            with open(os.path.join(root, name), mode="w", encoding="utf-8") as output_file:
                output_file.write(html)
        elif name.endswith(".css"):
            with open(os.path.join(root, name), mode="r", encoding="utf-8") as input_file:
                css = input_file.read()
            css = css.replace("url('/", "url('" + base_url)
            with open(os.path.join(root, name), mode="w", encoding="utf-8") as output_file:
                output_file.write(css)