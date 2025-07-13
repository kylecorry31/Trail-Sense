import os
from tqdm import tqdm

script_dir = os.path.dirname(os.path.abspath(__file__))

assets_path = os.path.join(script_dir, "../app/src/main/assets")
application_dir = os.path.join(script_dir, "../app/src/main")

excluded_dirs = ['temperatures', 'tides', 'precipitation', 'dewpoint', 'dem']

# Get all file names from the assets path
files = []
for root, _, filenames in os.walk(assets_path):
    if any(excluded_dir in root for excluded_dir in excluded_dirs):
        continue
    for filename in filenames:
        files.append(str(os.path.join(root, filename)).replace(str(assets_path) + '/', ''))

# Filter out any files that have a reference somewhere in the project
def is_referenced(file):
    for root, _, filenames in os.walk(application_dir):
        for filename in filenames:
            extensions = ['.kt', '.json', '.md', '.txt', '.java']
            if any(filename.endswith(ext) for ext in extensions):
                with open(os.path.join(root, filename), 'r', encoding='utf-8') as f:
                    if file in f.read():
                        return True
    return False

# Check if the file is referenced in the project
with tqdm(total=len(files), desc="Checking files") as pbar:
    for file in files:
        if not is_referenced(file):
            os.remove(os.path.join(assets_path, file))
            print(f"Removed unused asset: {file}")
        pbar.update(1)
