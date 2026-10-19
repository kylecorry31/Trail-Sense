---
name: trail-sense-sync-priority-project
description: Sync the Trail Sense Priorities GitHub project (kylecorry31 project 2) with the repo's open issues - remove closed and community available issues, and rank new and misplaced issues for approval before adding or moving them. Use when asked to sync with issues or update the priority project.
disable-model-invocation: true
---

# Trail Sense Sync Priority Project

The project at https://github.com/users/kylecorry31/projects/2 holds every open issue that is not `Status: Community Available`, in the maintainer's priority order. Its manual table order is the ranking, so there is no rank field. The `Priority: High`, `Priority: Medium` and `Priority: Low` labels are set by the maintainer and decide the section an issue belongs in: all High issues, then all Medium, then all Low. Never edit the issues themselves (labels, state, comments); only the project's items change.

Closed issues are not removed by the project: its "Item closed" workflow only updates Status and no auto-archive workflow is enabled. Check with `gh api graphql -f query='query{user(login:"kylecorry31"){projectV2(number:2){workflows(first:20){nodes{name enabled}}}}}'` if this may have changed.

## Workflow

1. Run `python3 .agents/skills/trail-sense-sync-priority-project/scripts/sync_plan.py` (read-only). Its JSON lists `remove_closed`, `remove_community`, `new` (open, not community available, not in the project), `misplaced` (project items outside their priority section), `no_single_priority` (project items with no priority label or more than one) and `order` (the current project order, with each item's `priority`). This step is complete when the JSON is read and the counts are known.
2. If `new` and `misplaced` are both empty, skip to step 4 with an empty `insert` list (removals need no approval). Otherwise rank each new and misplaced issue using the criteria below and read its title, labels and body (the `body` field is truncated; run `gh issue view <n>` when the intent is unclear). The anchor must land inside the issue's priority section: after an item of the same priority, or directly after the last item of the section above when it goes first in its own. A new issue with no priority label has no section, so rank it by value alone and mention in step 3 which section it landed in. This step is complete when every new and misplaced issue has an `after` anchor, an existing project issue it goes directly below, or `null` for the very top. Do not anchor to another misplaced issue unless that one is also being moved earlier in the plan.
3. Show the user a table for approval: the new or misplaced issue (marked as such, with its priority), the proposed anchor (`after #X <title>`), its resulting position (a "Position" column: the 1-based row number the issue will have in the project once all approved removals and inserts are applied), and a one-line reason. Compute positions from `order` by dropping the removed items, then applying the inserts in list order. List any `no_single_priority` items so the maintainer can fix their labels. Also list what will be removed (closed and community available, with titles). This step is complete when the user has approved or adjusted the positions. Do not change the project before then.
4. Write the approved plan to a file in the scratchpad directory:

```json
{"remove": [123, 456], "insert": [{"number": 789, "after": 1348}, {"number": 790, "after": 789}]}
```

   Insert entries are applied in list order, so a later entry may anchor to an earlier one. Inserting an issue that is already in the project moves it, so misplaced issues need no removal. Run `python3 .agents/skills/trail-sense-sync-priority-project/scripts/apply_changes.py <plan.json> --dry-run`, then again without `--dry-run`. This step is complete when the script prints `done`.
5. Re-run `sync_plan.py` and confirm `remove_closed`, `remove_community`, `new` and `misplaced` are all empty. This step is complete when the re-run is clean.
6. Report what was removed and where each new or moved issue landed.

## Ranking criteria

The priority label picks the section; within a section, rank by how much a hiker would notice the issue.

- Tiers of tools based on use/importance:
   - Primary: Navigation, Map, Beacons, Paths, Offline Maps
   - Secondary: Flashlight, Astronomy, Weather, Pedometer, Signal Finder, Survival Guide, User Guide, Field Guide, Packing Lists
   - Tertiary: All other tools
- Safety-relevant behavior (alerts, getting back, knowing where you are) ranks above polish.
- The project is roughly ordered: work that completes the primary tools, then other strong-value work, then well-scoped smaller improvements by hiker value, then research, plugins, refactors, and non-hiking tools at the bottom. Read the titles around a candidate position within the issue's priority section in `order` and place the issue next to issues of similar value.
- Research spikes, refactors and technical debt rank low unless they unblock a higher-ranked issue; place those just below the issue they unblock.
- When a new issue overlaps an existing one (duplicate or same work), anchor it directly after that issue and say so in the reason.
