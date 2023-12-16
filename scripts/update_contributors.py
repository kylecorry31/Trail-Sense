import requests
import os

url = "https://api.github.com/repos/kylecorry31/Trail-Sense/contributors"
script_dir = os.path.dirname(os.path.realpath(__file__)).replace('\\', '/')
licenses_file = script_dir + "/../app/src/main/java/com/kylecorry/trail_sense/licenses/Licenses.kt"

response = requests.get(url)

contributors = response.json()

usernames = [contributor["login"] for contributor in contributors]

kotlin_array = "val contributors = arrayOf(" + ", ".join([f'"{username}"' for username in usernames]) + ")"

## Find the contributors array in the Licenses.kt file and replace it
with open(licenses_file, "r") as file:
    contents = file.read()

start = contents.find("val contributors = arrayOf(")
end = contents.find(")", start) + 1

new_contents = contents[:start] + kotlin_array + contents[end:]

with open(licenses_file, "w") as file:
    file.write(new_contents)