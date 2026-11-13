#!/usr/bin/env bash

pushd "$(dirname "$0")" > /dev/null || exit 1

python3 update_contributors.py
python3 fix_strings.py
python3 update_guides.py
python3 remove_unused_assets.py

popd > /dev/null || exit 1
