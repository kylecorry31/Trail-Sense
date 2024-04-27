import requests
import os

url = "https://api.github.com/repos/kylecorry31/Trail-Sense/contributors?q=contributions&order=desc&per_page=100"
script_dir = os.path.dirname(os.path.realpath(__file__)).replace('\\', '/')
licenses_file = script_dir + "/../app/src/main/java/com/kylecorry/trail_sense/settings/licenses/Licenses.kt"

def get_all_contributors():
    page = 1
    contributors = []
    while True:
        response = requests.get(url + f"&page={page}")
        page_contributors = response.json()
        if len(page_contributors) == 0:
            break
        contributors.extend(page_contributors)
        page += 1
    return contributors

contributors = get_all_contributors()

usernames = [contributor["login"] for contributor in contributors]

print(len(usernames))

kotlin_array = "val contributors = arrayOf(" + ", ".join([f'"{username}"' for username in usernames]) + ")"

## Find the contributors array in the Licenses.kt file and replace it
with open(licenses_file, "r") as file:
    contents = file.read()

start = contents.find("val contributors = arrayOf(")
end = contents.find(")", start) + 1

new_contents = contents[:start] + kotlin_array + contents[end:]

with open(licenses_file, "w") as file:
    file.write(new_contents)