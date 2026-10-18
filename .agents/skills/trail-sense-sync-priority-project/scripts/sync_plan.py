#!/usr/bin/env python3
"""Read-only: compare the Trail Sense Priorities project against the repo's open issues.

Prints JSON with the items to remove (closed or community available), the open issues
that are not in the project yet, the current project order for choosing positions, and the
items that sit outside their priority section (High, then Medium, then Low) or have no single
priority label.
"""
import json
import subprocess
import sys

OWNER = "kylecorry31"
REPO = "kylecorry31/Trail-Sense"
PROJECT_NUMBER = 2
COMMUNITY_LABEL = "Status: Community Available"
PRIORITIES = {"Priority: High": 0, "Priority: Medium": 1, "Priority: Low": 2}

QUERY = """
query($owner: String!, $number: Int!, $after: String) {
  user(login: $owner) {
    projectV2(number: $number) {
      id
      title
      items(first: 100, after: $after) {
        pageInfo { hasNextPage endCursor }
        nodes {
          id
          content {
            ... on Issue { number title state labels(first: 30) { nodes { name } } }
          }
        }
      }
    }
  }
}
"""


def gh(*args):
    result = subprocess.run(["gh", *args], capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit(f"gh {' '.join(args[:3])} failed: {result.stderr.strip()}")
    return result.stdout


def project_items():
    items, after, project = [], None, None
    while True:
        cmd = ["api", "graphql", "-f", f"query={QUERY}", "-f", f"owner={OWNER}", "-F", f"number={PROJECT_NUMBER}"]
        if after:
            cmd += ["-f", f"after={after}"]
        data = json.loads(gh(*cmd))["data"]["user"]["projectV2"]
        project = data
        items += data["items"]["nodes"]
        page = data["items"]["pageInfo"]
        if not page["hasNextPage"]:
            return project, items
        after = page["endCursor"]


def priority_of(labels):
    """The issue's priority name, or None when it has no priority label or more than one."""
    found = [l for l in labels if l in PRIORITIES]
    return found[0].removeprefix("Priority: ") if len(found) == 1 else None


def find_misplaced(order):
    """Numbers of the fewest ranked items to move so the order is High, Medium, then Low.

    Keeps the longest run of items whose priority never goes down; items without a single
    priority label are skipped because they have no section to be out of.
    """
    ranked = [(i, PRIORITIES[f"Priority: {e['priority']}"]) for i, e in enumerate(order) if e["priority"]]
    best = []
    for k, (_, rank) in enumerate(ranked):
        candidates = [best[j] for j in range(k) if ranked[j][1] <= rank]
        best.append(max(candidates, key=len, default=[]) + [k])
    keep = {ranked[k][0] for k in max(best, key=len, default=[])}
    return [e for i, e in enumerate(order) if e["priority"] and i not in keep]


def main():
    project, items = project_items()
    in_project, remove_closed, remove_community, order, skipped = set(), [], [], [], 0

    for item in items:
        issue = item["content"]
        if not issue or "number" not in issue:
            skipped += 1
            continue
        labels = [l["name"] for l in issue["labels"]["nodes"]]
        number = issue["number"]
        in_project.add(number)
        entry = {"number": number, "title": issue["title"]}
        if issue["state"] != "OPEN":
            remove_closed.append(entry)
        elif COMMUNITY_LABEL in labels:
            remove_community.append(entry)
        else:
            order.append({
                **entry,
                "priority": priority_of(labels),
                "labels": [l for l in labels if l.startswith(("Tool:", "Type:"))],
            })

    open_issues = json.loads(
        gh("issue", "list", "--repo", REPO, "--state", "open", "--limit", "1000",
           "--json", "number,title,labels,body,comments")
    )
    new = []
    for issue in open_issues:
        labels = [l["name"] for l in issue["labels"]]
        if issue["number"] in in_project or COMMUNITY_LABEL in labels:
            continue
        new.append({
            "number": issue["number"],
            "title": issue["title"],
            "priority": priority_of(labels),
            "labels": labels,
            "comments": len(issue["comments"]),
            "body": " ".join((issue["body"] or "").split())[:600],
        })

    print(json.dumps({
        "project": {"id": project["id"], "title": project["title"]},
        "skipped_non_issue_items": skipped,
        "remove_closed": remove_closed,
        "remove_community": remove_community,
        "new": sorted(new, key=lambda i: i["number"]),
        "no_single_priority": [e for e in order if not e["priority"]],
        "misplaced": find_misplaced(order),
        "order": order,
    }, indent=1))


if __name__ == "__main__":
    main()
