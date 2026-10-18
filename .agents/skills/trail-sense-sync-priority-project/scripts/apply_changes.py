#!/usr/bin/env python3
"""Apply an approved sync plan to the Trail Sense Priorities project.

Usage: apply_changes.py PLAN.json [--dry-run]

PLAN.json: {"remove": [issue numbers], "insert": [{"number": 123, "after": 456 | null}]}

Removals run first. Inserts run in list order, so "after" may name an issue inserted earlier
in the same list. "after": null puts the issue at the top of the project. Inserting an issue
that is already in the project moves it.
"""
import json
import subprocess
import sys

OWNER = "kylecorry31"
REPO_URL = "https://github.com/kylecorry31/Trail-Sense"
PROJECT_NUMBER = 2

ITEMS_QUERY = """
query($owner: String!, $number: Int!, $after: String) {
  user(login: $owner) {
    projectV2(number: $number) {
      id
      items(first: 100, after: $after) {
        pageInfo { hasNextPage endCursor }
        nodes { id content { ... on Issue { number } } }
      }
    }
  }
}
"""

MOVE_MUTATION = """
mutation($project: ID!, $item: ID!, $after: ID) {
  updateProjectV2ItemPosition(input: {projectId: $project, itemId: $item, afterId: $after}) {
    items(first: 1) { nodes { id } }
  }
}
"""


def gh(*args):
    result = subprocess.run(["gh", *args], capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit(f"gh {' '.join(args[:3])} failed: {result.stderr.strip()}")
    return result.stdout


def load_items():
    ids, after = {}, None
    while True:
        cmd = ["api", "graphql", "-f", f"query={ITEMS_QUERY}", "-f", f"owner={OWNER}", "-F", f"number={PROJECT_NUMBER}"]
        if after:
            cmd += ["-f", f"after={after}"]
        project = json.loads(gh(*cmd))["data"]["user"]["projectV2"]
        for node in project["items"]["nodes"]:
            if node["content"] and "number" in node["content"]:
                ids[node["content"]["number"]] = node["id"]
        page = project["items"]["pageInfo"]
        if not page["hasNextPage"]:
            return project["id"], ids
        after = page["endCursor"]


def main():
    plan = json.load(open(sys.argv[1]))
    dry_run = "--dry-run" in sys.argv
    project_id, ids = load_items()

    for number in plan.get("remove", []):
        if number not in ids:
            print(f"skip remove #{number}: not in project")
            continue
        print(f"remove #{number}")
        if not dry_run:
            gh("project", "item-delete", str(PROJECT_NUMBER), "--owner", OWNER, "--id", ids.pop(number))

    for entry in plan.get("insert", []):
        number, after = entry["number"], entry.get("after")
        if after is not None and after not in ids:
            sys.exit(f"cannot insert #{number}: anchor #{after} is not in the project")
        print(f"insert #{number} after {'top' if after is None else '#' + str(after)}")
        if dry_run:
            ids[number] = f"dry-run-{number}"
            continue
        added = json.loads(gh("project", "item-add", str(PROJECT_NUMBER), "--owner", OWNER,
                              "--url", f"{REPO_URL}/issues/{number}", "--format", "json"))
        ids[number] = added["id"]
        cmd = ["api", "graphql", "-f", f"query={MOVE_MUTATION}", "-f", f"project={project_id}", "-f", f"item={added['id']}"]
        if after is not None:
            cmd += ["-f", f"after={ids[after]}"]
        gh(*cmd)

    print("dry run, nothing changed" if dry_run else "done")


if __name__ == "__main__":
    main()
