import os
import re

assets = '../../app/src/main/assets/survival_guide'
guides = '../../guides/en-US'

images = []
for root, dirs, files in os.walk(assets):
    for file in files:
        if file.endswith(".webp"):
            images.append(file)

used_images = []
for root, dirs, files in os.walk(guides):
    for file in files:
        if file.endswith(".md"):
            with open(os.path.join(root, file), 'r') as f:
                contents = f.read()
            
            used_images.extend(re.findall(r"(\d+.webp)", contents))

unused_images = set(images) - set(used_images)
size = sum(os.path.getsize(os.path.join(assets, image)) for image in unused_images)
for image in unused_images:
    os.remove(os.path.join(assets, image))
print(f"Removed {len(unused_images)} unused images ({size / 1024:.2f} KB)")
            
