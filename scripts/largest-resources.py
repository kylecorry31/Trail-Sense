import os

script_dir = os.path.dirname(os.path.abspath(__file__))

dirs_to_check = [
    f"{script_dir}/../app/src/main/assets",
    f"{script_dir}/../app/src/main/res/drawable",
    f"{script_dir}/../app/src/main/res/raw"
]

root_path = f"{script_dir}/../app/src/main"

file_sizes = []

for dir_path in dirs_to_check:
    if os.path.exists(dir_path):
        for root, dirs, files in os.walk(dir_path):
            for filename in files:
                filepath = os.path.join(root, filename)
                size = os.path.getsize(filepath) / 1024  # Convert to KB
                short_path = os.path.relpath(filepath, root_path)
                file_sizes.append((short_path, size))

file_sizes.sort(key=lambda x: x[1], reverse=True)

print("\nTop 100 largest files:")
for filepath, size in file_sizes[:100]:
    print(f"{filepath}: {size:,.2f} KB")
