import os

excluded_files = ["Licenses.kt"]

# Get the script directory
script_dir = os.path.dirname(os.path.realpath(__file__))

# Get the app directory
app_dir = os.path.join(script_dir, "..", "app")

# Get all the .kt files in the app directory (recursively)
kt_files = []
for root, dirs, files in os.walk(app_dir):
    for file in files:
        if (
            file.endswith(".kt")
            and file not in excluded_files
            and not file.endswith("Test.kt")
        ):
            kt_files.append(os.path.join(root, file))

# Get the length of each file and sort them
file_lengths = []
for file in kt_files:
    with open(file, "r") as f:
        file_lengths.append((file.replace('\\', '/').split('/')[-1], len(f.read().split('\n'))))
file_lengths.sort(key=lambda x: x[1], reverse=True)

# Print any file longer than 300 lines
for file in file_lengths:
    if file[1] > 300:
        print(file[0], file[1])
