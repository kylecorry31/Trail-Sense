import shutil
import os

assets = '../../app/src/main/assets/survival_guide'
guides = '../../guides/en-US'

# TODO: Only copy images

shutil.rmtree(assets, ignore_errors=True)
shutil.copytree("output", assets)

# Remove all .md files
for root, dirs, files in os.walk(assets):
    for file in files:
        if file.endswith(".md"):
            os.remove(os.path.join(root, file))

# Copy all .md files to the guides directory (guides already exists and has other guides in it)
for root, dirs, files in os.walk("output"):
    for file in files:
        if file.endswith(".md"):
            shutil.copy2(os.path.join(root, file), os.path.join(guides, file)) #.replace('.md', '.txt')))