#!/usr/bin/env python3

import sys

print("# Changelog")
print()
for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    print(f'- {line}')
print()