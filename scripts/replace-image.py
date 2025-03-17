import os
import sys
import shutil

# Get the script directory
current_dir = os.path.dirname(os.path.abspath(__file__))

# Find all files in the parent directory that match the desired file name (arg 2)
parent_dir = os.path.dirname(current_dir)
matches = []
for root, dirs, files in os.walk(parent_dir):
    if 'survival-guide-book' in root:
        continue
    for file in files:
        if file == sys.argv[2]:
            matches.append(os.path.join(root, file))

# Replace those files with the file the user provided (arg 1)
source_file = sys.argv[1]
for match in matches:
    shutil.copy2(source_file, match)
    print('Replaced', match)
