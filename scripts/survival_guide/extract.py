import pymupdf
from markdownify import markdownify as md
import re
import base64
from PIL import Image
import os
import shutil


def slugify(value):
    """
    Normalizes string, converts to lowercase, removes non-alpha characters,
    and converts spaces to hyphens.
    """
    import unicodedata

    value = (
        unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    )
    value = re.sub(r"[^\w\s-]", "", value).strip().lower()
    value = re.sub(r"[-\s]+", "-", value)
    return value


# https://armypubs.army.mil/epubs/DR_pubs/DR_a/pdf/web/ARN12086_ATP%203-50x21%20FINAL%20WEB%202.pdf
doc = pymupdf.open("ARN12086_ATP 3-50x21 FINAL WEB 2.pdf")

shutil.rmtree("output", ignore_errors=True)

if not os.path.exists("output"):
    os.mkdir("output")

html = ""

for page in doc:
    # TODO: Image positions aren't retained - they are all at the bottom of the page
    # TODO: Tables aren't properly found
    content = page.get_text("xhtml")
    # tables = page.find_tables().tables
    # print([table.to_markdown() for table in tables])
    html += content

# Remove all img tags where height="8"
# Replace this with the section number (this only happens in chapter 2)
section_images = re.findall(r'<p><img[^>]*height="8"[^>]*></p>\n<p>', html)
for i, image in enumerate(section_images):
    section = f"2-{i + 1}. "
    html = html.replace(image, f"<p>{section}")

# Replace all images with webp images
i = 0
total_size = 0
images = re.findall(r'(<img[^>]*src="[^"]*"[^>]*>)', html)
for image in images:
    srcBase64 = re.search(r'data:image/png;base64,([^"]*)', image).group(1)
    # Base64 decode
    srcBytes = base64.b64decode(srcBase64)
    # Convert to webp
    with open(f"output/{i}.png", "wb") as f:
        f.write(srcBytes)
    img = Image.open(f"output/{i}.png")
    # TODO: Most images can be smaller / lower quality
    img.thumbnail((1000, 400))
    img.save(f"output/{i}.webp", "WEBP", quality=20)
    img.close()
    total_size += os.stat(f"output/{i}.webp").st_size
    # Delete the png
    os.remove(f"output/{i}.png")
    html = html.replace(image, f'<img src="{i}.webp" />')
    i += 1

with open("output/guide.html", "w") as f:
    f.write(html)

print(f"Total size of images: {total_size / 1024} KB")

# TODO: Fix tables

# Remove the footer
html = html.replace("&#x14;&#x1b;&#x3;6HSWHPEHU&#x3;&#x15;&#x13;&#x14;&#x1b;", "")

# Set up the lists
html = re.sub("<p>z (.*)</p>", r"<li>\1</li>", html)
html = re.sub("(&#x83;.*?)(?=</[^b]+>)", r"<ul>\1</ul>", html)
html = re.sub("&#x83;(.*?)(?=&#x83;|</[^b]+>)", r"<li>\1</li>", html)

# Replace unsupported characters
html = html.replace("&#x2019;", "'")
html = html.replace("&#xbd;", "1/2")
html = html.replace("&#x2013;", "-")
html = html.replace("&#x2014;", "-")
html = html.replace("&#xbc;", "1/4")
html = html.replace("&#x201c;", '"')
html = html.replace("&#x201d;", '"')
# TODO: This isn't working in the markdown
html = html.replace("&#xb0;", "&deg;")
html = html.replace("&#xe8;", "&egrave;")
html = html.replace("&#x3;", "")
html = html.replace("This page intentionally left blank.", "")

# Remove the last 2 pages
html = html.split('<div id="page0">')[:-2]
html = '<div id="page0">'.join(html)

# Always remove the first 2 lines after a div line (they are page headers), except if they are a title
lines = html.split("\n")
for i in range(len(lines) - 2):
    if lines[i].startswith("<div"):
        if lines[i + 1].startswith("<p>"):
            lines[i + 1] = ""
        if lines[i + 2].startswith("<p>"):
            lines[i + 2] = ""

html = "\n".join([line for line in lines if line.strip() != ""])

# Remove the introduction
html = html[html.find('<div id="page0">\n<h3><b>Chapter 1</b></h3>') :]

# Remove everything below glossary
html = html[: html.find('<div id="page0">\n<h2><b>Glossary </b></h2>')]

# Make chapter titles h2
html = re.sub(
    r"<h3><b>(Chapter .+)</b></h3><h2><b>(.*)</b></h2>",
    lambda m: f'<h2 id="{slugify(m.group(1))}">{m.group(1)}: {m.group(2)}</h2>',
    html,
)
html = re.sub(
    r"<h3><b>(Appendix .+)</b></h3><h2><b>(.*)</b></h2>",
    lambda m: f'<h2 id="{slugify(m.group(1))}">{m.group(1)}: {m.group(2)}</h2>',
    html,
)

# Remove unneeded bold tags
html = re.sub(r"<h2><b>(.*)</b></h2>", r"<h2>\1</h2>", html)
html = re.sub(r"<h3><b>(.*)</b></h3>", r"<h3>\1</h3>", html)

# Promote some other headers to h3
html = html.replace('<p><b>LIFESAVING STEPS</b></p>', '<h3>LIFESAVING STEPS</h3>')
html = html.replace('<p><b>WATER CROSSINGS</b></p>', '<h3>WATER CROSSINGS</h3>')
html = html.replace('<p><b>BASIC KNOTS</b></p>', '<h3>BASIC KNOTS</h3>')

# Restore broken paragraphs
# TODO: Anything that isn't a <b>, - , or in the form ##-## should be part of the same paragraph. An image may also break a paragraph, so it should move up the sentence to be above the image.
# html = re.sub(r'</p>\n</div>\n<div id="page0">\n<p>([a-z])', r' \1', html)

# Remove unnecessary tags
html = re.sub(r"<p[^>]*>\s*</p>", "", html)
html = re.sub(r"<div[^>]*>\s*</div>", "", html)
# html = re.sub(r'</?div.*>', '', html)

# Remove empty lines
# html = re.sub(r'\n', '', html)

# TODO: Generate a table of contents with links to each section
# TODO: Delete images that aren't in the PDF

# Split by chapter
actual_chapters = []
chapters = re.split(r'<div id="page0">\n<h2 id="chapter-\d+">', html)
for i, chapter in enumerate(chapters):
    if i == 0:
        continue
    actual_chapters.append(f"<div id=\"page0\">\n<h2 id=\"chapter-{i}\">" + chapter)

# The last chapter contains both the last chapter and the appendix, split them
chapter = chapters[-1]
appendices = re.split(r'<div id="page0">\n<h2 id="appendix-\w+">', chapter)
actual_chapters.pop()
actual_chapters.append(
    f"<div id=\"page0\">\n<h2 id=\"chapter-{len(chapters) - 1}\">" + appendices[0]
)
for i, appendix in enumerate(appendices):
    if i == 0:
        continue
    actual_chapters.append(f"<div id=\"page0\">\n<h2 id=\"appendix-a\">" + appendix)

# Convert each chapter to markdown
for chapter in actual_chapters:
    name = re.search(r'<h2 id="([^"]*)">', chapter).group(1)
    # Remove the title
    chapter = re.sub(r"<h2 id=\".*\">.*</h2>", "", chapter)
    # Replace the h3 tags with h2 tags
    chapter = re.sub(r"<h3>", "<h2>", chapter)
    chapter = re.sub(r"</h3>", "</h2>", chapter)
    markdown = md(chapter)

    # Remove extra newlines
    markdown = re.sub(r"\n{3,}", "\n\n", markdown)

    # Convert the --- syntax into h2
    markdown = re.sub(r"(.+)\n-{2,}\n", r"## \1\n", markdown)

    # Convert the markdown image syntax into html with width="100%"
    markdown = re.sub(r"!\[.*\]\((.*)\)", r'![](file:///android_asset/survival_guide/\1)', markdown)

    # Replace warnings and cautions with bold text
    markdown = re.sub(r"#+ WARNING", r"**WARNING**", markdown)
    markdown = re.sub(r"#+ CAUTION", r"**CAUTION**", markdown)

    # Lowercase all headers (except for the first letter)
    markdown = re.sub(r"(#+) (.*)", lambda m: f"{m.group(1)} {m.group(2).capitalize()}", markdown)

    # Remove the \- and \.
    markdown = markdown.replace("\\-", "-")
    markdown = markdown.replace("\\.", ".")

    # Remove paragraph passage numbers
    markdown = re.sub(r"^[1-9A]+-\d+\.\s", "", markdown, flags=re.MULTILINE)

    with open(f"output/guide_survival_{name.replace('-', '_')}.md", "w") as f:
        f.write(markdown)
    total_size += os.stat(f"output/guide_survival_{name.replace('-', '_')}.md").st_size

print(f"Total size: {total_size / 1024} KB")
