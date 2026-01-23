#!/usr/bin/env python3

import sys
import re

# Initialize lists for different commit types
feat = []
fix = []
docs = []
style = []
refactor = []
test = []
chore = []
other = []
all_commits = []

# Read commits from stdin
for line in sys.stdin:
    line = line.strip()
    # Skip empty lines
    if not line:
        continue
    
    all_commits.append(line)
    
    # Extract type from conventional commit format (handles commit hashes)
    match = re.match(r'^[^ ]+\s+([a-z]+)(\([^)]*\))?:\s*(.*)$', line)
    if match:
        commit_type = match.group(1)
        message = match.group(3)
    else:
        commit_type = 'other'
        message = line
        # Remove commit hash from the start
        message = re.sub(r'^[^ ]+\s+', '', message)
    
    # Clean up message: remove PR references and capitalize first letter
    message = re.sub(r'\s*\(#\d+\)$', '', message)
    message = message[0].upper() + message[1:] if message else ''
    
    # Add to appropriate category
    if commit_type == 'feat':
        feat.append(message)
    elif commit_type == 'fix':
        fix.append(message)
    elif commit_type == 'docs':
        docs.append(message)
    elif commit_type == 'style':
        style.append(message)
    elif commit_type == 'refactor':
        refactor.append(message)
    elif commit_type == 'test':
        test.append(message)
    elif commit_type == 'chore':
        chore.append(message)
    else:
        other.append(message)

# Output changelog
print("# Changelog")
print()

# Function to print category
def print_category(title, items):
    if items:
        print(f"## {title}")
        for msg in items:
            print(f"- {msg}")
        print()

# Print each category
print_category("Features", feat)
print_category("Bug Fixes", fix)
print_category("Documentation", docs)
print_category("Style Changes", style)
print_category("Refactoring", refactor)
print_category("Tests", test)
print_category("Chores", chore)
print_category("Other Changes", other)
print_category("All Commits", all_commits)