import os
import shutil
import re

root = os.path.dirname(os.path.realpath(__file__)).replace('\\', '/') + '/..'

def check_url_integrity(original_text: str, translated_text: str) -> bool:
    """Returns True if the URLs in the translated guide are the same as the URLs in the original guide, 
    except for Wikipedia URLs which can have different locales. Otherwise, returns False."""    
    # Regex to get all URLs
    expression = r'(https?://[^\s\)]+)'
    
    original_urls = re.findall(expression, original_text)
    translated_urls = re.findall(expression, translated_text)

    if len(original_urls) != len(translated_urls):
        return False

    for original_url, translated_url in zip(original_urls, translated_urls):
        if original_url == translated_url:
            return True

        # If the URLs don't match, but it's for wikipedia, let it through with a warning
        wikipedia_regex = r'^https://[a-zA-Z]{2}(\.m)?\.wikipedia\.org/wiki/.*'
        if re.match(wikipedia_regex, original_url) and re.match(wikipedia_regex, translated_url):
            print(f'[WARN] Allowed {original_url} -> {translated_url}')
            return True
        
        return False

    return True

def check_new_lines(original_text: str, translated_text: str) -> bool:
    """Returns True if the translated guide is complete (based on the number of new lines). Otherwise, returns False."""
    original_lines = original_text.splitlines()
    translated_lines = translated_text.splitlines()

    # Make sure the translated guide has the same number of lines as the original guide
    return len(original_lines) == len(translated_lines)

def check_is_translated(original_text: str, translated_text: str) -> bool:
    """Returns True if the translated guide is mostly translated. Otherwise, returns False."""
    original_words = original_text.split()
    translated_words = translated_text.split()

    # Distinct
    original_words = set(original_words)
    translated_words = set(translated_words)
    translated_words_not_in_original = len(translated_words - original_words)

    # Greater than 75 % translated can pass through
    return translated_words_not_in_original / len(translated_words) > 0.75

def check_markdown_integrity(original_text: str, translated_text: str) -> bool:
    """Returns True if the translated guide has the same number of markdown elements as the original guide. Otherwise, returns False."""
    headers = ["#", "##", "###", "####", "#####", "######"]
    original_lines = original_text.splitlines()
    translated_lines = translated_text.splitlines()

    # Count the number of headers in the original guide
    original_header_count = 0
    original_unordered_list_count = 0
    for line in original_lines:
        if line.strip().startswith(tuple(headers)):
            original_header_count += 1
        elif line.strip().startswith("-"):
            original_unordered_list_count += 1

    # Count the number of headers in the translated guide
    translated_header_count = 0
    translated_unordered_list_count = 0
    for line in translated_lines:
        if line.strip().startswith(tuple(headers)):
            translated_header_count += 1
        elif line.strip().startswith("-"):
            translated_unordered_list_count += 1
    
    return original_header_count == translated_header_count and original_unordered_list_count == translated_unordered_list_count


def get_issues(original, translated):
    """Returns a list of issues found in the translated guide. If no issues are found, an empty list is returned."""
    with open(original, encoding="utf8") as f:
        original_text = f.read()
    with open(translated, encoding="utf8") as f:
        translated_text = f.read()
    issues = []

    if not check_url_integrity(original_text, translated_text):
        issues.append("urls")

    if not check_new_lines(original_text, translated_text):
        issues.append("new-lines")

    if not check_markdown_integrity(original_text, translated_text):
        issues.append("markdown")
    
    if not check_is_translated(original_text, translated_text):
        issues.append("not-fully-translated")

    return issues

# Steps
# 1. Copy the original guides from guides/en-US to app/src/main/res/raw and replace the .txt extension with .md
# 2. For each translated guide (e.g. guides/zh-CN), check if the translated guide is passing the test by comparing it with the original guide of the same name in the guides/en-US folder.
# 3. If the translated guide is passing the test, copy it to app/src/main/res/raw-<language-code> and replace the .txt extension with .md
# 4. If the guide does not pass the test, print the name of the guide and the reason why it is failing the test.

# Step 1
for filename in os.listdir(root + "/guides/en-US"):
    if filename.endswith(".txt") and not filename.startswith("field_guide"):
        shutil.copy2(root + "/guides/en-US/" + filename, root + "/app/src/main/res/raw/" + filename[:-4] + ".md")
    elif filename.endswith(".md") or filename.startswith("field_guide"):
        # These files are not translatable
        shutil.copy2(root + "/guides/en-US/" + filename, root + "/app/src/main/res/raw/" + filename)

# Step 2
for language in os.listdir(root + "/guides"):
    if language != "en-US":
        for filename in os.listdir(root + "/guides/" + language):
            if filename.endswith(".txt"):
                issues = get_issues(root + "/guides/en-US/" + filename, root + "/guides/" + language + "/" + filename)
                if len(issues) == 0:
                    # Create the destination folder if it does not exist
                    if not os.path.exists(root + "/app/src/main/res/raw-" + language):
                        os.makedirs(root + "/app/src/main/res/raw-" + language)
                    # Step 3
                    shutil.copy2(root + "/guides/" + language + "/" + filename, root + "/app/src/main/res/raw-" + language + "/" + filename[:-4] + ".md")
                else:
                    # Step 4
                    print("[ERROR] " + language + "/" + filename + ": " + ', '.join(issues))