import requests

url = "https://api.github.com/repos/kylecorry31/Trail-Sense/contributors"

response = requests.get(url)

contributors = response.json()

usernames = [contributor["login"] for contributor in contributors]

kotlin_array = "arrayOf(" + ", ".join([f'"{username}"' for username in usernames]) + ")"

print(kotlin_array)