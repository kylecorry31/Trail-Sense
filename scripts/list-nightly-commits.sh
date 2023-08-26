#!/bin/bash

# Get the latest tag
latest_tag=$(git describe --tags --abbrev=0)

# Get the commit log since the last tag
git log $latest_tag..HEAD --oneline --abbrev-commit