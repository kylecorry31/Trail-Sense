import os
import shutil
import re

root = os.path.dirname(os.path.realpath(__file__)).replace('\\', '/') + '/..'

# Copy user guides
for filename in os.listdir(root + "/guides/en-US"):
    if filename.endswith(".txt") and filename.startswith("guide_tool"):
        destination = root + "/site/src/user-guide/" + filename[:-4] + ".md"
        shutil.copy2(root + "/guides/en-US/" + filename, destination)
        with open(destination, 'r', encoding='utf8') as f:
                content = f.read()
        # TODO: Capitalize the first letter of each word, but don't lowercase the rest
        title = destination.split('/')[-1].split('.')[0].replace('guide_tool', '').replace('_', ' ').title().strip()
        with open(destination, 'w') as f:
            new_content = f"""---
title: "{title}"
---

{content}
"""
            # TODO: Copy over images
            new_content = new_content.replace('file:///android_asset/', 'https://raw.githubusercontent.com/kylecorry31/Trail-Sense/main/app/src/main/assets/')

            f.write(new_content)

# Copy survival guides
# Copy user guides
for filename in os.listdir(root + "/guides/en-US"):
    if filename.endswith(".txt") and filename.startswith("guide_survival"):
        destination = root + "/site/src/survival-guide/" + filename[:-4] + ".md"
        shutil.copy2(root + "/guides/en-US/" + filename, destination)
        with open(destination, 'r', encoding='utf8') as f:
                content = f.read()
        title = destination.split('/')[-1].split('.')[0].replace('guide_survival_chapter', '').replace('_', ' ').title().strip()
        with open(destination, 'w') as f:
            new_content = f"""---
title: "{title}"
---

{content}
"""
            # TODO: If line is the start of a list, add a new line before it
            # TODO: Copy over images
            new_content = new_content.replace('file:///android_asset/', 'https://raw.githubusercontent.com/kylecorry31/Trail-Sense/main/app/src/main/assets/')

            f.write(new_content)