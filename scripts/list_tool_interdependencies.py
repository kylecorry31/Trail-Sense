# Step 1. Go through each folder in app/src/main/java/com/kylecorry/trail_sense/tools
# Step 2. For each file in the folder find all imports from other packages in the tools folder (excluding itself and the tools package)
# Step 3. List each tool and the tools it depends on

import os
import re

def get_imports(file):
    with open(file, "r") as f:
        lines = f.readlines()
        imports = []
        for line in lines:
            if line.startswith("import"):
                imports.append(line)
    return imports

def get_tools(file):
    tools = set()
    imports = get_imports(file)
    for imp in imports:
        match = re.match(r"import com\.kylecorry\.trail_sense\.tools\.(.*)\.", imp)
        if match:
            tools.add(match.group(1).split(".")[0])
    return tools

def get_all_files_in_dir(dir):
    files = []
    for root, dirs, fs in os.walk(dir):
        for f in fs:
            if f.endswith(".kt"):
                files.append(os.path.join(root, f))
    return files

def get_root_level_folders(dir):
    folders = []
    for f in os.listdir(dir):
        if os.path.isdir(os.path.join(dir, f)):
            folders.append(f)
    return folders

script_dir = os.path.dirname(os.path.realpath(__file__))
tools_dir = os.path.join(script_dir, "..", "app", "src", "main", "java", "com", "kylecorry", "trail_sense", "tools")

for folder in get_root_level_folders(tools_dir):
    folder_path = os.path.join(tools_dir, folder)
    files = get_all_files_in_dir(folder_path)
    tools = set()
    for file in files:
        for t in get_tools(file):
            tools.add(t)
    if folder in tools:
        tools.remove(folder)
    if 'tools' in tools:
        tools.remove('tools')
    if len(tools) > 0:
        print(f"{folder} depends on {tools}")
    else:
        print(f"{folder} has no dependencies")
        
